package com.example.tms.service;

import com.example.tms.model.AppUser;
import com.example.tms.model.AuditLog;
import com.example.tms.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {

 private final AuditLogRepository auditLogRepository;

 public AuditLogService(AuditLogRepository auditLogRepository){
  this.auditLogRepository = auditLogRepository;
 }

 public void log(AppUser actor, String action, String entityType, Long entityId, String description){
  AuditLog auditLog = new AuditLog();
  auditLog.setOrganization(actor.getOrganization());
  auditLog.setAction(action);
  auditLog.setEntityType(entityType);
  auditLog.setEntityId(entityId);
  auditLog.setDescription(description);
  auditLog.setActorName(actor.getFullName());
  auditLog.setActorEmail(actor.getEmail());
  auditLog.setActorRole(actor.getRole());
  auditLogRepository.save(auditLog);
 }
}
