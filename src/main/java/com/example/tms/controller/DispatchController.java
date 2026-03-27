package com.example.tms.controller;

import com.example.tms.model.AppUser;
import com.example.tms.repository.DriverRepository;
import com.example.tms.repository.MaintenanceRecordRepository;
import com.example.tms.repository.TripRepository;
import com.example.tms.repository.VehicleRepository;
import com.example.tms.security.CurrentUserSupport;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@CrossOrigin(origins = {"http://localhost:8100", "http://localhost:4200"})
@RequestMapping("/dispatch")
public class DispatchController {

 private final DriverRepository driverRepository;
 private final VehicleRepository vehicleRepository;
 private final TripRepository tripRepository;
 private final MaintenanceRecordRepository maintenanceRepository;
 private final CurrentUserSupport currentUserSupport;

 public DispatchController(
   DriverRepository driverRepository,
   VehicleRepository vehicleRepository,
   TripRepository tripRepository,
   MaintenanceRecordRepository maintenanceRepository,
   CurrentUserSupport currentUserSupport
 ){
  this.driverRepository = driverRepository;
  this.vehicleRepository = vehicleRepository;
  this.tripRepository = tripRepository;
  this.maintenanceRepository = maintenanceRepository;
  this.currentUserSupport = currentUserSupport;
 }

 @GetMapping("/board")
 public Map<String, Object> board(HttpServletRequest request){
  AppUser user = currentUserSupport.currentUser(request);
  LocalDate today = LocalDate.now();

  var drivers = driverRepository.findAll().stream()
   .filter(driver -> driver.getOrganization().getId().equals(user.getOrganization().getId()))
   .toList();
  var vehicles = vehicleRepository.findAll().stream()
   .filter(vehicle -> vehicle.getOrganization().getId().equals(user.getOrganization().getId()))
   .toList();
  var trips = tripRepository.findAll().stream()
   .filter(trip -> trip.getOrganization().getId().equals(user.getOrganization().getId()))
   .toList();
  var maintenance = maintenanceRepository.findAll().stream()
   .filter(record -> record.getOrganization().getId().equals(user.getOrganization().getId()))
   .toList();

  Map<String, Object> payload = new LinkedHashMap<>();
  payload.put("availableDrivers", drivers.stream()
   .filter(driver -> "AVAILABLE".equalsIgnoreCase(driver.getStatus()))
   .toList());
  payload.put("availableVehicles", vehicles.stream()
   .filter(vehicle -> !"MAINTENANCE".equalsIgnoreCase(vehicle.getStatus()))
   .toList());
  payload.put("unassignedTrips", trips.stream()
   .filter(trip -> trip.getDriver() == null || trip.getVehicle() == null)
   .toList());
  payload.put("activeTrips", trips.stream()
   .filter(trip -> "IN_PROGRESS".equalsIgnoreCase(trip.getStatus()) || "PLANNED".equalsIgnoreCase(trip.getStatus()))
   .sorted((a, b) -> {
    LocalDate aDate = a.getScheduledDate() == null ? LocalDate.MAX : a.getScheduledDate();
    LocalDate bDate = b.getScheduledDate() == null ? LocalDate.MAX : b.getScheduledDate();
    return aDate.compareTo(bDate);
   })
   .limit(8)
   .toList());
  payload.put("maintenanceAlerts", maintenance.stream()
   .filter(record -> record.getServiceDate() != null && !record.getServiceDate().isAfter(today.plusDays(7)))
   .sorted((a, b) -> {
    LocalDate aDate = a.getServiceDate() == null ? LocalDate.MAX : a.getServiceDate();
    LocalDate bDate = b.getServiceDate() == null ? LocalDate.MAX : b.getServiceDate();
    return aDate.compareTo(bDate);
   })
   .limit(8)
   .toList());
  payload.put("availableDriverCount", drivers.stream().filter(driver -> "AVAILABLE".equalsIgnoreCase(driver.getStatus())).count());
  payload.put("availableVehicleCount", vehicles.stream().filter(vehicle -> !"MAINTENANCE".equalsIgnoreCase(vehicle.getStatus())).count());
  payload.put("unassignedTripCount", trips.stream().filter(trip -> trip.getDriver() == null || trip.getVehicle() == null).count());
  return payload;
 }
}
