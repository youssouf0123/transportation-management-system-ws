package com.example.tms.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
public class DocumentRecord {

 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 private Long id;

 private String title;
 private String documentType;
 private String entityType;
 private Long entityId;
 private String status = "ACTIVE";
 private String fileName;
 private String fileContentType;
 private Long fileSize;
 private String fileUrl;
 @JsonIgnore
 private String storagePath;
 private String notes;
 private LocalDate expiryDate;
 private LocalDateTime createdAt = LocalDateTime.now();

 @JsonIgnore
 @ManyToOne(optional = false)
 private Organization organization;

 @ManyToOne
 private AppUser uploadedBy;

 public Long getId(){ return id; }
 public void setId(Long id){ this.id = id; }

 public String getTitle(){ return title; }
 public void setTitle(String title){ this.title = title; }

 public String getDocumentType(){ return documentType; }
 public void setDocumentType(String documentType){ this.documentType = documentType; }

 public String getEntityType(){ return entityType; }
 public void setEntityType(String entityType){ this.entityType = entityType; }

 public Long getEntityId(){ return entityId; }
 public void setEntityId(Long entityId){ this.entityId = entityId; }

 public String getStatus(){ return status; }
 public void setStatus(String status){ this.status = status; }

 public String getFileName(){ return fileName; }
 public void setFileName(String fileName){ this.fileName = fileName; }

 public String getFileContentType(){ return fileContentType; }
 public void setFileContentType(String fileContentType){ this.fileContentType = fileContentType; }

 public Long getFileSize(){ return fileSize; }
 public void setFileSize(Long fileSize){ this.fileSize = fileSize; }

 public String getFileUrl(){ return fileUrl; }
 public void setFileUrl(String fileUrl){ this.fileUrl = fileUrl; }

 public String getStoragePath(){ return storagePath; }
 public void setStoragePath(String storagePath){ this.storagePath = storagePath; }

 public String getNotes(){ return notes; }
 public void setNotes(String notes){ this.notes = notes; }

 public LocalDate getExpiryDate(){ return expiryDate; }
 public void setExpiryDate(LocalDate expiryDate){ this.expiryDate = expiryDate; }

 public LocalDateTime getCreatedAt(){ return createdAt; }
 public void setCreatedAt(LocalDateTime createdAt){ this.createdAt = createdAt; }

 public Organization getOrganization(){ return organization; }
 public void setOrganization(Organization organization){ this.organization = organization; }

 public AppUser getUploadedBy(){ return uploadedBy; }
 public void setUploadedBy(AppUser uploadedBy){ this.uploadedBy = uploadedBy; }
}
