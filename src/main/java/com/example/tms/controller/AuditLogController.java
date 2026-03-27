package com.example.tms.controller;

import com.example.tms.model.AppUser;
import com.example.tms.model.AuditLog;
import com.example.tms.repository.AuditLogRepository;
import com.example.tms.security.CurrentUserSupport;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = {"http://localhost:8100", "http://localhost:4200"})
@RequestMapping("/audit")
public class AuditLogController {

 private final AuditLogRepository auditLogRepository;
 private final CurrentUserSupport currentUserSupport;

 public AuditLogController(AuditLogRepository auditLogRepository, CurrentUserSupport currentUserSupport){
  this.auditLogRepository = auditLogRepository;
  this.currentUserSupport = currentUserSupport;
 }

 @GetMapping
 public List<AuditLog> getLogs(
   @RequestParam(required = false) String entityType,
   @RequestParam(required = false) String action,
   HttpServletRequest request
 ){
  AppUser user = currentUserSupport.currentUser(request);
  currentUserSupport.requireRole(user, "OWNER", "MANAGER");
  return auditLogRepository.findAll().stream()
   .filter(log -> log.getOrganization().getId().equals(user.getOrganization().getId()))
   .filter(log -> entityType == null || entityType.isBlank() || entityType.equalsIgnoreCase(log.getEntityType()))
   .filter(log -> action == null || action.isBlank() || action.equalsIgnoreCase(log.getAction()))
   .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
   .limit(200)
   .toList();
 }
}
