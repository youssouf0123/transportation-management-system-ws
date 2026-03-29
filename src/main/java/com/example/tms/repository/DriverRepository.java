
package com.example.tms.repository;

import com.example.tms.model.Driver;
import com.example.tms.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverRepository extends JpaRepository<Driver,Long> {
 void deleteByOrganization(Organization organization);
}
