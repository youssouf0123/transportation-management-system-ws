
package com.example.tms.repository;

import com.example.tms.model.FinanceRecord;
import com.example.tms.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface FinanceRepository extends JpaRepository<FinanceRecord,Long> {

 List<FinanceRecord> findByDate(LocalDate date);

 List<FinanceRecord> findByDateBetween(LocalDate start, LocalDate end);

 List<FinanceRecord> findByVehicleId(Long vehicleId);

 List<FinanceRecord> findByVehicleIdAndDate(Long vehicleId, LocalDate date);

 List<FinanceRecord> findByVehicleIdAndDateBetween(Long vehicleId, LocalDate start, LocalDate end);

 void deleteByOrganization(Organization organization);
}
