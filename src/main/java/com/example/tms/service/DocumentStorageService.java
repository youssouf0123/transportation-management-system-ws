package com.example.tms.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class DocumentStorageService {

 private final Path storageDirectory;
 private final boolean r2Enabled;
 private final String bucketName;
 private final S3Client s3Client;

 public DocumentStorageService(
  @Value("${app.documents.storage-dir:uploads/documents}") String storageDir,
  @Value("${app.documents.r2.enabled:false}") boolean r2Enabled,
  @Value("${app.documents.r2.account-id:}") String accountId,
  @Value("${app.documents.r2.access-key-id:}") String accessKeyId,
  @Value("${app.documents.r2.secret-access-key:}") String secretAccessKey,
  @Value("${app.documents.r2.bucket-name:}") String bucketName
 ) throws IOException {
  this.r2Enabled = r2Enabled
   && !accountId.isBlank()
   && !accessKeyId.isBlank()
   && !secretAccessKey.isBlank()
   && !bucketName.isBlank();
  this.bucketName = bucketName;

  if(this.r2Enabled){
   this.storageDirectory = null;
   this.s3Client = S3Client.builder()
    .region(Region.of("auto"))
    .endpointOverride(URI.create("https://" + accountId + ".r2.cloudflarestorage.com"))
    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
    .build();
  } else {
   this.storageDirectory = Paths.get(storageDir).toAbsolutePath().normalize();
   Files.createDirectories(this.storageDirectory);
   this.s3Client = null;
  }
 }

 public StoredDocument store(MultipartFile file) throws IOException {
  String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() == null ? "document" : file.getOriginalFilename());
  String extension = "";
  int extensionIndex = originalFilename.lastIndexOf('.');
  if(extensionIndex >= 0){
   extension = originalFilename.substring(extensionIndex);
  }

  String storedFilename = UUID.randomUUID() + extension;
  if(r2Enabled){
   s3Client.putObject(
    PutObjectRequest.builder()
     .bucket(bucketName)
     .key(storedFilename)
     .contentType(file.getContentType())
     .build(),
    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
   );
  } else {
   Path target = storageDirectory.resolve(storedFilename);
   Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
  }

  return new StoredDocument(
   storedFilename,
   originalFilename,
   file.getContentType(),
   file.getSize()
  );
 }

 public Resource loadAsResource(String storagePath) {
  if(r2Enabled){
   try {
    byte[] fileBytes = s3Client.getObjectAsBytes(
     GetObjectRequest.builder()
      .bucket(bucketName)
      .key(storagePath)
      .build()
    ).asByteArray();
    return new ByteArrayResource(fileBytes);
   } catch (NoSuchKeyException exception) {
    throw new RuntimeException("File not found.", exception);
   }
  }

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

  if(r2Enabled){
   s3Client.deleteObject(
    DeleteObjectRequest.builder()
     .bucket(bucketName)
     .key(storagePath)
     .build()
   );
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
