package com.example.tms.repository;

import com.example.tms.model.AuditLog;
import com.example.tms.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
 void deleteByOrganization(Organization organization);
}
