package com.example.tms.repository;

import com.example.tms.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {
 Optional<Organization> findByName(String name);
}
