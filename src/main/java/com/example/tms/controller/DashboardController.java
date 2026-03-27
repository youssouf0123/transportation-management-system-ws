package com.example.tms.controller;

import com.example.tms.repository.DriverRepository;
import com.example.tms.repository.FinanceRepository;
import com.example.tms.repository.MaintenanceRecordRepository;
import com.example.tms.repository.TripRepository;
import com.example.tms.repository.VehicleRepository;
import com.example.tms.model.AppUser;
import com.example.tms.security.CurrentUserSupport;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@CrossOrigin(origins = {"http://localhost:8100", "http://localhost:4200"})
@RequestMapping("/dashboard")
public class DashboardController {

 private final DriverRepository driverRepo;
 private final VehicleRepository vehicleRepo;
 private final FinanceRepository financeRepo;
 private final TripRepository tripRepo;
 private final MaintenanceRecordRepository maintenanceRepo;
 private final CurrentUserSupport currentUserSupport;

 public DashboardController(
   DriverRepository driverRepo,
   VehicleRepository vehicleRepo,
   FinanceRepository financeRepo,
   TripRepository tripRepo,
   MaintenanceRecordRepository maintenanceRepo,
   CurrentUserSupport currentUserSupport
 ){
  this.driverRepo=driverRepo;
  this.vehicleRepo=vehicleRepo;
  this.financeRepo=financeRepo;
  this.tripRepo=tripRepo;
  this.maintenanceRepo=maintenanceRepo;
  this.currentUserSupport=currentUserSupport;
 }

 @GetMapping("/summary")
 public Map<String, Object> summary(HttpServletRequest request){
  AppUser user = currentUserSupport.currentUser(request);
  LocalDate now = LocalDate.now();
  LocalDate monthStart = now.withDayOfMonth(1);

  double earnings = financeRepo.findAll().stream()
   .filter(record -> record.getOrganization().getId().equals(user.getOrganization().getId()))
   .filter(record -> record.getDate() != null && !record.getDate().isBefore(monthStart))
   .filter(record -> "EARNING".equalsIgnoreCase(record.getType()))
   .mapToDouble(record -> record.getAmount())
   .sum();

  double expenses = financeRepo.findAll().stream()
   .filter(record -> record.getOrganization().getId().equals(user.getOrganization().getId()))
   .filter(record -> record.getDate() != null && !record.getDate().isBefore(monthStart))
   .filter(record -> "EXPENSE".equalsIgnoreCase(record.getType()))
   .mapToDouble(record -> record.getAmount())
   .sum();

  Map<String, Object> payload = new LinkedHashMap<>();
  payload.put("drivers", driverRepo.findAll().stream().filter(driver -> driver.getOrganization().getId().equals(user.getOrganization().getId())).count());
  payload.put("availableDrivers", driverRepo.findAll().stream().filter(driver -> driver.getOrganization().getId().equals(user.getOrganization().getId())).filter(driver -> "AVAILABLE".equalsIgnoreCase(driver.getStatus())).count());
  payload.put("vehicles", vehicleRepo.findAll().stream().filter(vehicle -> vehicle.getOrganization().getId().equals(user.getOrganization().getId())).count());
  payload.put("assignedVehicles", vehicleRepo.findAll().stream().filter(vehicle -> vehicle.getOrganization().getId().equals(user.getOrganization().getId())).filter(vehicle -> vehicle.getDriver() != null).count());
  payload.put("activeTrips", tripRepo.findAll().stream().filter(trip -> trip.getOrganization().getId().equals(user.getOrganization().getId())).filter(trip -> "IN_PROGRESS".equalsIgnoreCase(trip.getStatus())).count());
  payload.put("maintenanceDue", maintenanceRepo.findAll().stream().filter(record -> record.getOrganization().getId().equals(user.getOrganization().getId())).filter(record -> "SCHEDULED".equalsIgnoreCase(record.getStatus())).count());
  payload.put("earnings", earnings);
  payload.put("expenses", expenses);
  payload.put("net", earnings - expenses);
  payload.put("recentTrips", tripRepo.findAll().stream()
   .filter(trip -> trip.getOrganization().getId().equals(user.getOrganization().getId()))
   .sorted((a, b) -> {
    LocalDate aDate = a.getScheduledDate() == null ? LocalDate.MIN : a.getScheduledDate();
    LocalDate bDate = b.getScheduledDate() == null ? LocalDate.MIN : b.getScheduledDate();
    return bDate.compareTo(aDate);
   })
   .limit(5)
   .toList());
  payload.put("recentMaintenance", maintenanceRepo.findAll().stream()
   .filter(record -> record.getOrganization().getId().equals(user.getOrganization().getId()))
   .sorted((a, b) -> {
    LocalDate aDate = a.getServiceDate() == null ? LocalDate.MIN : a.getServiceDate();
    LocalDate bDate = b.getServiceDate() == null ? LocalDate.MIN : b.getServiceDate();
    return bDate.compareTo(aDate);
   })
   .limit(5)
   .toList());
  return payload;
 }
}
