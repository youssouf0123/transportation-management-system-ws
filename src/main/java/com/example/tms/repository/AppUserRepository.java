package com.example.tms.repository;

import com.example.tms.model.AppUser;
import com.example.tms.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
 Optional<AppUser> findByEmail(String email);
 Optional<AppUser> findByAuthToken(String authToken);
 List<AppUser> findByOrganization(Organization organization);
}
