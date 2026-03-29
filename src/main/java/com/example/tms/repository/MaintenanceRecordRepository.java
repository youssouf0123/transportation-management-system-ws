package com.example.tms.repository;

import com.example.tms.model.Organization;
import com.example.tms.model.MaintenanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintenanceRecordRepository extends JpaRepository<MaintenanceRecord, Long> {
 void deleteByOrganization(Organization organization);
}
