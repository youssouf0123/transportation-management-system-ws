package com.example.tms.repository;

import com.example.tms.model.DocumentRecord;
import com.example.tms.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRecordRepository extends JpaRepository<DocumentRecord, Long> {
 void deleteByOrganization(Organization organization);
}
