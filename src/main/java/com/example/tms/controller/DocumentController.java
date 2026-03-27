package com.example.tms.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.tms.model.AppUser;
import com.example.tms.model.DocumentRecord;
import com.example.tms.repository.DocumentRecordRepository;
import com.example.tms.security.CurrentUserSupport;
import com.example.tms.service.AuditLogService;
import com.example.tms.service.DocumentStorageService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@CrossOrigin(origins = {"http://localhost:8100", "http://localhost:4200"})
@RequestMapping("/documents")
public class DocumentController {

 private final DocumentRecordRepository documentRepository;
 private final CurrentUserSupport currentUserSupport;
 private final AuditLogService auditLogService;
 private final DocumentStorageService documentStorageService;

 public DocumentController(
   DocumentRecordRepository documentRepository,
   CurrentUserSupport currentUserSupport,
   AuditLogService auditLogService,
   DocumentStorageService documentStorageService
 ){
  this.documentRepository = documentRepository;
  this.currentUserSupport = currentUserSupport;
  this.auditLogService = auditLogService;
  this.documentStorageService = documentStorageService;
 }

 @GetMapping
 public List<DocumentRecord> getAll(
   @RequestParam(required = false) String documentType,
   @RequestParam(required = false) String entityType,
   @RequestParam(required = false) String status,
   @RequestParam(required = false) String expiryBefore,
   HttpServletRequest request
 ){
  AppUser user = currentUserSupport.currentUser(request);
  LocalDate parsedExpiryBefore = (expiryBefore == null || expiryBefore.isBlank()) ? null : LocalDate.parse(expiryBefore);
  return documentRepository.findAll().stream()
   .filter(document -> document.getOrganization().getId().equals(user.getOrganization().getId()))
   .filter(document -> documentType == null || documentType.isBlank() || documentType.equalsIgnoreCase(document.getDocumentType()))
   .filter(document -> entityType == null || entityType.isBlank() || entityType.equalsIgnoreCase(document.getEntityType()))
   .filter(document -> status == null || status.isBlank() || status.equalsIgnoreCase(document.getStatus()))
   .filter(document -> parsedExpiryBefore == null || (document.getExpiryDate() != null && !document.getExpiryDate().isAfter(parsedExpiryBefore)))
   .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
   .toList();
 }

 @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
 public DocumentRecord create(
   @RequestPart("document") DocumentRecord document,
   @RequestPart(value = "file", required = false) MultipartFile file,
   HttpServletRequest request
 ) throws IOException {
  AppUser user = currentUserSupport.currentUser(request);
  currentUserSupport.requireRole(user, "OWNER", "MANAGER", "DISPATCHER", "FINANCE");
  if(file == null || file.isEmpty()){
   throw new RuntimeException("Document file is required.");
  }
  document.setOrganization(user.getOrganization());
  document.setUploadedBy(user);
  DocumentRecord saved = documentRepository.save(document);
  applyFile(saved, file, request);
  saved = documentRepository.save(saved);
  auditLogService.log(user, "CREATE", "DOCUMENT", saved.getId(), "Added document " + saved.getTitle());
  return saved;
 }

 @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
 public DocumentRecord update(
   @PathVariable Long id,
   @RequestPart("document") DocumentRecord incoming,
   @RequestPart(value = "file", required = false) MultipartFile file,
   HttpServletRequest request
 ) throws IOException {
  AppUser user = currentUserSupport.currentUser(request);
  currentUserSupport.requireRole(user, "OWNER", "MANAGER", "DISPATCHER", "FINANCE");
  DocumentRecord existing = documentRepository.findById(id).orElseThrow();
  if(!existing.getOrganization().getId().equals(user.getOrganization().getId())){ throw new RuntimeException("Forbidden"); }
  existing.setTitle(incoming.getTitle());
  existing.setDocumentType(incoming.getDocumentType());
  existing.setEntityType(incoming.getEntityType());
  existing.setEntityId(incoming.getEntityId());
  existing.setStatus(incoming.getStatus());
  existing.setNotes(incoming.getNotes());
  existing.setExpiryDate(incoming.getExpiryDate());
  if(file != null && !file.isEmpty()){
   documentStorageService.deleteIfExists(existing.getStoragePath());
   applyFile(existing, file, request);
  }
  DocumentRecord saved = documentRepository.save(existing);
  auditLogService.log(user, "UPDATE", "DOCUMENT", saved.getId(), "Updated document " + saved.getTitle());
  return saved;
 }

 @GetMapping("/{id}/file")
 public ResponseEntity<Resource> openFile(@PathVariable Long id, HttpServletRequest request){
  AppUser user = currentUserSupport.currentUser(request);
  DocumentRecord existing = documentRepository.findById(id).orElseThrow();
  if(!existing.getOrganization().getId().equals(user.getOrganization().getId())){ throw new RuntimeException("Forbidden"); }
  if(existing.getStoragePath() == null || existing.getStoragePath().isBlank()){ throw new RuntimeException("File not found"); }

  Resource resource = documentStorageService.loadAsResource(existing.getStoragePath());
  String contentType = existing.getFileContentType() == null || existing.getFileContentType().isBlank()
   ? MediaType.APPLICATION_OCTET_STREAM_VALUE
   : existing.getFileContentType();

  return ResponseEntity.ok()
   .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + existing.getFileName() + "\"")
   .contentType(MediaType.parseMediaType(contentType))
   .body(resource);
 }

 @DeleteMapping("/{id}")
 public void delete(@PathVariable Long id, HttpServletRequest request){
  AppUser user = currentUserSupport.currentUser(request);
  currentUserSupport.requireRole(user, "OWNER", "MANAGER");
  DocumentRecord existing = documentRepository.findById(id).orElseThrow();
  if(existing.getOrganization().getId().equals(user.getOrganization().getId())){
   documentStorageService.deleteIfExists(existing.getStoragePath());
   auditLogService.log(user, "DELETE", "DOCUMENT", existing.getId(), "Deleted document " + existing.getTitle());
   documentRepository.delete(existing);
  }
 }

 private void applyFile(DocumentRecord document, MultipartFile file, HttpServletRequest request) throws IOException {
  DocumentStorageService.StoredDocument storedDocument = documentStorageService.store(file);
  document.setFileName(storedDocument.originalFilename());
  document.setFileContentType(storedDocument.contentType());
  document.setFileSize(storedDocument.fileSize());
  document.setStoragePath(storedDocument.storagePath());
  document.setFileUrl(buildFileUrl(request, document));
 }

 private String buildFileUrl(HttpServletRequest request, DocumentRecord document) {
  String baseUrl = request.getRequestURL().toString().replace(request.getRequestURI(), "");
  return baseUrl + "/documents/" + document.getId() + "/file";
 }
}
