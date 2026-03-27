package com.example.tms.controller;

import com.example.tms.model.MaintenanceRecord;
import com.example.tms.model.Vehicle;
import com.example.tms.model.AppUser;
import com.example.tms.repository.MaintenanceRecordRepository;
import com.example.tms.repository.VehicleRepository;
import com.example.tms.security.CurrentUserSupport;
import com.example.tms.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@CrossOrigin(origins = {"http://localhost:8100", "http://localhost:4200"})
@RequestMapping("/maintenance")
public class MaintenanceController {

 private final MaintenanceRecordRepository maintenanceRepo;
 private final VehicleRepository vehicleRepo;
 private final CurrentUserSupport currentUserSupport;
 private final AuditLogService auditLogService;

 public MaintenanceController(MaintenanceRecordRepository maintenanceRepo, VehicleRepository vehicleRepo, CurrentUserSupport currentUserSupport, AuditLogService auditLogService){
  this.maintenanceRepo=maintenanceRepo;
  this.vehicleRepo=vehicleRepo;
  this.currentUserSupport=currentUserSupport;
  this.auditLogService = auditLogService;
 }

 @PostMapping
 public MaintenanceRecord create(@RequestBody MaintenanceRecord record, HttpServletRequest request){
  AppUser user = currentUserSupport.currentUser(request);
  currentUserSupport.requireRole(user, "OWNER", "MANAGER", "DISPATCHER");
  record.setVehicle(resolveVehicle(record.getVehicle()));
  record.setOrganization(user.getOrganization());
  MaintenanceRecord saved = maintenanceRepo.save(record);
  auditLogService.log(user, "CREATE", "MAINTENANCE", saved.getId(), "Created maintenance record " + saved.getServiceType());
  return saved;
 }

 @PutMapping("/{id}")
 public MaintenanceRecord update(@PathVariable Long id, @RequestBody MaintenanceRecord incoming, HttpServletRequest request){
  AppUser user = currentUserSupport.currentUser(request);
  currentUserSupport.requireRole(user, "OWNER", "MANAGER", "DISPATCHER");
  MaintenanceRecord existing = maintenanceRepo.findById(id).orElseThrow();
  if(!existing.getOrganization().getId().equals(user.getOrganization().getId())){ throw new RuntimeException("Forbidden"); }
  existing.setServiceType(incoming.getServiceType());
  existing.setStatus(incoming.getStatus());
  existing.setServiceDate(incoming.getServiceDate());
  existing.setMileage(incoming.getMileage());
  existing.setCost(incoming.getCost());
  existing.setNotes(incoming.getNotes());
  existing.setVehicle(resolveVehicle(incoming.getVehicle()));
  MaintenanceRecord saved = maintenanceRepo.save(existing);
  auditLogService.log(user, "UPDATE", "MAINTENANCE", saved.getId(), "Updated maintenance record " + saved.getId());
  return saved;
 }

 @DeleteMapping("/{id}")
 public void delete(@PathVariable Long id, HttpServletRequest request){
  AppUser user = currentUserSupport.currentUser(request);
  currentUserSupport.requireRole(user, "OWNER", "MANAGER");
  MaintenanceRecord existing = maintenanceRepo.findById(id).orElseThrow();
  if(existing.getOrganization().getId().equals(user.getOrganization().getId())){
   auditLogService.log(user, "DELETE", "MAINTENANCE", existing.getId(), "Deleted maintenance record " + existing.getId());
   maintenanceRepo.delete(existing);
  }
 }

 @GetMapping
 public List<MaintenanceRecord> getAll(
   @RequestParam(required = false) Long vehicleId,
   @RequestParam(required = false) String status,
   @RequestParam(required = false) String date,
   HttpServletRequest request
 ){
  AppUser user = currentUserSupport.currentUser(request);
  return maintenanceRepo.findAll().stream()
   .filter(record -> record.getOrganization().getId().equals(user.getOrganization().getId()))
   .filter(record -> vehicleId == null || (record.getVehicle() != null && vehicleId.equals(record.getVehicle().getId())))
   .filter(record -> status == null || status.isBlank() || status.equalsIgnoreCase(record.getStatus()))
   .filter(record -> date == null || date.isBlank() || LocalDate.parse(date).equals(record.getServiceDate()))
   .toList();
 }

 private Vehicle resolveVehicle(Vehicle vehicle){
  if(vehicle == null || vehicle.getId() == null){
   return null;
  }
  return vehicleRepo.findById(vehicle.getId()).orElseThrow();
 }
}
