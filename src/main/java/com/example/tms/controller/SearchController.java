package com.example.tms.controller;

import com.example.tms.model.AppUser;
import com.example.tms.repository.AppUserRepository;
import com.example.tms.repository.AuditLogRepository;
import com.example.tms.repository.DocumentRecordRepository;
import com.example.tms.repository.DriverRepository;
import com.example.tms.repository.FinanceRepository;
import com.example.tms.repository.MaintenanceRecordRepository;
import com.example.tms.repository.TripRepository;
import com.example.tms.repository.VehicleRepository;
import com.example.tms.security.CurrentUserSupport;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RestController
@CrossOrigin(origins = {"http://localhost:8100", "http://localhost:4200"})
@RequestMapping("/search")
public class SearchController {

 private final DriverRepository driverRepository;
 private final VehicleRepository vehicleRepository;
 private final TripRepository tripRepository;
 private final FinanceRepository financeRepository;
 private final MaintenanceRecordRepository maintenanceRecordRepository;
 private final DocumentRecordRepository documentRecordRepository;
 private final AppUserRepository appUserRepository;
 private final AuditLogRepository auditLogRepository;
 private final CurrentUserSupport currentUserSupport;

 public SearchController(
  DriverRepository driverRepository,
  VehicleRepository vehicleRepository,
  TripRepository tripRepository,
  FinanceRepository financeRepository,
  MaintenanceRecordRepository maintenanceRecordRepository,
  DocumentRecordRepository documentRecordRepository,
  AppUserRepository appUserRepository,
  AuditLogRepository auditLogRepository,
  CurrentUserSupport currentUserSupport
 ) {
  this.driverRepository = driverRepository;
  this.vehicleRepository = vehicleRepository;
  this.tripRepository = tripRepository;
  this.financeRepository = financeRepository;
  this.maintenanceRecordRepository = maintenanceRecordRepository;
  this.documentRecordRepository = documentRecordRepository;
  this.appUserRepository = appUserRepository;
  this.auditLogRepository = auditLogRepository;
  this.currentUserSupport = currentUserSupport;
 }

 @GetMapping
 public SearchResponse search(@RequestParam String q, HttpServletRequest request) {
  AppUser user = currentUserSupport.currentUser(request);
  String query = q == null ? "" : q.trim();
  if(query.isBlank()) {
   return new SearchResponse(query, 0, List.of());
  }

  String needle = query.toLowerCase(Locale.ROOT);
  List<SearchSection> sections = new ArrayList<>();

  List<SearchItem> driverResults = driverRepository.findAll().stream()
   .filter(driver -> driver.getOrganization().getId().equals(user.getOrganization().getId()))
   .filter(driver -> contains(needle, driver.getName(), driver.getLicenseNumber(), driver.getPhoneNumber(), driver.getStatus()))
   .limit(5)
   .map(driver -> new SearchItem(
    "driver",
    driver.getId(),
    driver.getName(),
    "License: " + safe(driver.getLicenseNumber()) + " • Status: " + safe(driver.getStatus()),
    "/drivers/list"
   ))
   .toList();
  if(!driverResults.isEmpty()) {
   sections.add(new SearchSection("drivers", "Drivers", driverResults));
  }

  List<SearchItem> vehicleResults = vehicleRepository.findAll().stream()
   .filter(vehicle -> vehicle.getOrganization().getId().equals(user.getOrganization().getId()))
   .filter(vehicle -> contains(needle, vehicle.getMake(), vehicle.getModel(), vehicle.getPlateNumber(), vehicle.getStatus(),
    vehicle.getDriver() != null ? vehicle.getDriver().getName() : null))
   .limit(5)
   .map(vehicle -> new SearchItem(
    "vehicle",
    vehicle.getId(),
    safe(vehicle.getMake()) + " " + safe(vehicle.getModel()),
    "Plate: " + safe(vehicle.getPlateNumber()) + " • Driver: " + safe(vehicle.getDriver() != null ? vehicle.getDriver().getName() : "Unassigned"),
    "/vehicles/list"
   ))
   .toList();
  if(!vehicleResults.isEmpty()) {
   sections.add(new SearchSection("vehicles", "Vehicles", vehicleResults));
  }

  List<SearchItem> tripResults = tripRepository.findAll().stream()
   .filter(trip -> trip.getOrganization().getId().equals(user.getOrganization().getId()))
   .filter(trip -> contains(needle, trip.getOrigin(), trip.getDestination(), trip.getStatus(), trip.getCargoDescription(), trip.getNotes(),
    trip.getDriver() != null ? trip.getDriver().getName() : null,
    trip.getVehicle() != null ? trip.getVehicle().getPlateNumber() : null))
   .limit(5)
   .map(trip -> new SearchItem(
    "trip",
    trip.getId(),
    safe(trip.getOrigin()) + " to " + safe(trip.getDestination()),
    "Date: " + String.valueOf(trip.getScheduledDate()) + " • Status: " + safe(trip.getStatus()),
    "/trips/list"
   ))
   .toList();
  if(!tripResults.isEmpty()) {
   sections.add(new SearchSection("trips", "Trips", tripResults));
  }

  List<SearchItem> financeResults = financeRepository.findAll().stream()
   .filter(record -> record.getOrganization().getId().equals(user.getOrganization().getId()))
   .filter(record -> contains(needle, record.getDescription(), record.getType(), record.getCategory(), record.getRecordScope(),
    record.getVehicle() != null ? record.getVehicle().getPlateNumber() : null,
    String.valueOf(record.getAmount())))
   .limit(5)
   .map(record -> new SearchItem(
    "finance",
    record.getId(),
    safe(record.getDescription()),
    "Type: " + safe(record.getType()) + " • Amount: " + record.getAmount(),
    "/finance/list"
   ))
   .toList();
  if(!financeResults.isEmpty()) {
   sections.add(new SearchSection("finance", "Finance", financeResults));
  }

  List<SearchItem> maintenanceResults = maintenanceRecordRepository.findAll().stream()
   .filter(record -> record.getOrganization().getId().equals(user.getOrganization().getId()))
   .filter(record -> contains(needle, record.getServiceType(), record.getStatus(), record.getNotes(),
    record.getVehicle() != null ? record.getVehicle().getPlateNumber() : null))
   .limit(5)
   .map(record -> new SearchItem(
    "maintenance",
    record.getId(),
    safe(record.getServiceType()),
    "Date: " + String.valueOf(record.getServiceDate()) + " • Vehicle: " + safe(record.getVehicle() != null ? record.getVehicle().getPlateNumber() : "No vehicle"),
    "/maintenance/list"
   ))
   .toList();
  if(!maintenanceResults.isEmpty()) {
   sections.add(new SearchSection("maintenance", "Maintenance", maintenanceResults));
  }

  List<SearchItem> documentResults = documentRecordRepository.findAll().stream()
   .filter(document -> document.getOrganization().getId().equals(user.getOrganization().getId()))
   .filter(document -> contains(needle, document.getTitle(), document.getDocumentType(), document.getEntityType(), document.getStatus(),
    document.getFileName(), document.getNotes()))
   .limit(5)
   .map(document -> new SearchItem(
    "document",
    document.getId(),
    safe(document.getTitle()),
    "Type: " + safe(document.getDocumentType()) + " • File: " + safe(document.getFileName()),
    "/documents"
   ))
   .toList();
  if(!documentResults.isEmpty()) {
   sections.add(new SearchSection("documents", "Documents", documentResults));
  }

  if(isManager(user)) {
   List<SearchItem> userResults = appUserRepository.findAll().stream()
    .filter(appUser -> appUser.getOrganization().getId().equals(user.getOrganization().getId()))
    .filter(appUser -> contains(needle, appUser.getFullName(), appUser.getEmail(), appUser.getRole()))
    .limit(5)
    .map(appUser -> new SearchItem(
     "user",
     appUser.getId(),
     safe(appUser.getFullName()),
     safe(appUser.getEmail()) + " • " + safe(appUser.getRole()),
     "/users"
    ))
    .toList();
   if(!userResults.isEmpty()) {
    sections.add(new SearchSection("users", "Users", userResults));
   }

   List<SearchItem> auditResults = auditLogRepository.findAll().stream()
    .filter(log -> log.getOrganization().getId().equals(user.getOrganization().getId()))
    .filter(log -> contains(needle, log.getAction(), log.getEntityType(), log.getDescription(), log.getActorName(), log.getActorEmail()))
    .limit(5)
    .map(log -> new SearchItem(
     "audit",
     log.getId(),
     safe(log.getEntityType()) + " • " + safe(log.getAction()),
     safe(log.getDescription()),
     "/audit"
    ))
    .toList();
   if(!auditResults.isEmpty()) {
    sections.add(new SearchSection("audit", "Audit Log", auditResults));
   }
  }

  int totalResults = sections.stream().mapToInt(section -> section.results().size()).sum();
  return new SearchResponse(query, totalResults, sections);
 }

 private boolean contains(String needle, String... values) {
  for(String value : values) {
   if(value != null && value.toLowerCase(Locale.ROOT).contains(needle)) {
    return true;
   }
  }
  return false;
 }

 private boolean isManager(AppUser user) {
  return "OWNER".equalsIgnoreCase(user.getRole()) || "MANAGER".equalsIgnoreCase(user.getRole());
 }

 private String safe(String value) {
  return value == null || value.isBlank() ? "N/A" : value;
 }

 public record SearchResponse(String query, int totalResults, List<SearchSection> sections) {}
 public record SearchSection(String key, String title, List<SearchItem> results) {}
 public record SearchItem(String type, Long id, String title, String subtitle, String route) {}
}
