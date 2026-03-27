package com.example.tms.repository;

import com.example.tms.model.DocumentRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRecordRepository extends JpaRepository<DocumentRecord, Long> {
}
