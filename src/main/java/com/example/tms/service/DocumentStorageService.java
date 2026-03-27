package com.example.tms.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class DocumentStorageService {

 private final Path storageDirectory;

 public DocumentStorageService(@Value("${app.documents.storage-dir:uploads/documents}") String storageDir) throws IOException {
  this.storageDirectory = Paths.get(storageDir).toAbsolutePath().normalize();
  Files.createDirectories(this.storageDirectory);
 }

 public StoredDocument store(MultipartFile file) throws IOException {
  String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() == null ? "document" : file.getOriginalFilename());
  String extension = "";
  int extensionIndex = originalFilename.lastIndexOf('.');
  if(extensionIndex >= 0){
   extension = originalFilename.substring(extensionIndex);
  }

  String storedFilename = UUID.randomUUID() + extension;
  Path target = storageDirectory.resolve(storedFilename);
  Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

  return new StoredDocument(
   storedFilename,
   originalFilename,
   file.getContentType(),
   file.getSize()
  );
 }

 public Resource loadAsResource(String storagePath) {
  try {
   Path filePath = storageDirectory.resolve(storagePath).normalize();
   Resource resource = new UrlResource(filePath.toUri());
   if(resource.exists()){
    return resource;
   }
   throw new RuntimeException("File not found.");
  } catch (MalformedURLException exception) {
   throw new RuntimeException("File not found.", exception);
  }
 }

 public void deleteIfExists(String storagePath) {
  if(storagePath == null || storagePath.isBlank()){
   return;
  }

  try {
   Files.deleteIfExists(storageDirectory.resolve(storagePath).normalize());
  } catch (IOException exception) {
   throw new RuntimeException("Unable to delete stored document.", exception);
  }
 }

 public record StoredDocument(
  String storagePath,
  String originalFilename,
  String contentType,
  long fileSize
 ) {}
}
