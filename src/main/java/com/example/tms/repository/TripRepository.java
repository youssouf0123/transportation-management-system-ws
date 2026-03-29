package com.example.tms.repository;

import com.example.tms.model.Trip;
import com.example.tms.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripRepository extends JpaRepository<Trip, Long> {
 void deleteByOrganization(Organization organization);
}
