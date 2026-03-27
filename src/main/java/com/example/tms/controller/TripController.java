package com.example.tms.controller;

import com.example.tms.model.Driver;
import com.example.tms.model.Trip;
import com.example.tms.model.Vehicle;
import com.example.tms.model.AppUser;
import com.example.tms.repository.DriverRepository;
import com.example.tms.repository.TripRepository;
import com.example.tms.repository.VehicleRepository;
import com.example.tms.security.CurrentUserSupport;
import com.example.tms.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@CrossOrigin(origins = {"http://localhost:8100", "http://localhost:4200"})
@RequestMapping("/trips")
public class TripController {

 private final TripRepository tripRepo;
 private final DriverRepository driverRepo;
 private final VehicleRepository vehicleRepo;
 private final CurrentUserSupport currentUserSupport;
 private final AuditLogService auditLogService;

 public TripController(TripRepository tripRepo, DriverRepository driverRepo, VehicleRepository vehicleRepo, CurrentUserSupport currentUserSupport, AuditLogService auditLogService){
  this.tripRepo=tripRepo;
  this.driverRepo=driverRepo;
  this.vehicleRepo=vehicleRepo;
  this.currentUserSupport=currentUserSupport;
  this.auditLogService = auditLogService;
 }

 @PostMapping
 public Trip create(@RequestBody Trip trip, HttpServletRequest request){
  AppUser user = currentUserSupport.currentUser(request);
  currentUserSupport.requireRole(user, "OWNER", "MANAGER", "DISPATCHER");
  attachRelations(trip);
  trip.setOrganization(user.getOrganization());
  Trip saved = tripRepo.save(trip);
  auditLogService.log(user, "CREATE", "TRIP", saved.getId(), "Created trip " + saved.getOrigin() + " to " + saved.getDestination());
  return saved;
 }

 @PutMapping("/{id}")
 public Trip update(@PathVariable Long id, @RequestBody Trip incoming, HttpServletRequest request){
  AppUser user = currentUserSupport.currentUser(request);
  currentUserSupport.requireRole(user, "OWNER", "MANAGER", "DISPATCHER");
  Trip existing = tripRepo.findById(id).orElseThrow();
  if(!existing.getOrganization().getId().equals(user.getOrganization().getId())){ throw new RuntimeException("Forbidden"); }
  existing.setOrigin(incoming.getOrigin());
  existing.setDestination(incoming.getDestination());
  existing.setStatus(incoming.getStatus());
  existing.setScheduledDate(incoming.getScheduledDate());
  existing.setCargoDescription(incoming.getCargoDescription());
  existing.setNotes(incoming.getNotes());
  existing.setDriver(resolveDriver(incoming.getDriver()));
  existing.setVehicle(resolveVehicle(incoming.getVehicle()));
  Trip saved = tripRepo.save(existing);
  auditLogService.log(user, "UPDATE", "TRIP", saved.getId(), "Updated trip " + saved.getOrigin() + " to " + saved.getDestination());
  return saved;
 }

 @DeleteMapping("/{id}")
 public void delete(@PathVariable Long id, HttpServletRequest request){
  AppUser user = currentUserSupport.currentUser(request);
  currentUserSupport.requireRole(user, "OWNER", "MANAGER", "DISPATCHER");
  Trip existing = tripRepo.findById(id).orElseThrow();
  if(existing.getOrganization().getId().equals(user.getOrganization().getId())){
   auditLogService.log(user, "DELETE", "TRIP", existing.getId(), "Deleted trip " + existing.getOrigin() + " to " + existing.getDestination());
   tripRepo.delete(existing);
  }
 }

 @GetMapping
 public List<Trip> getAll(
   @RequestParam(required = false) String status,
   @RequestParam(required = false) Long driverId,
   @RequestParam(required = false) Long vehicleId,
   @RequestParam(required = false) String date,
   HttpServletRequest request
 ){
  AppUser user = currentUserSupport.currentUser(request);
  return tripRepo.findAll().stream()
   .filter(trip -> trip.getOrganization().getId().equals(user.getOrganization().getId()))
   .filter(trip -> status == null || status.isBlank() || status.equalsIgnoreCase(trip.getStatus()))
   .filter(trip -> driverId == null || (trip.getDriver() != null && driverId.equals(trip.getDriver().getId())))
   .filter(trip -> vehicleId == null || (trip.getVehicle() != null && vehicleId.equals(trip.getVehicle().getId())))
   .filter(trip -> date == null || date.isBlank() || LocalDate.parse(date).equals(trip.getScheduledDate()))
   .toList();
 }

 private void attachRelations(Trip trip){
  trip.setDriver(resolveDriver(trip.getDriver()));
  trip.setVehicle(resolveVehicle(trip.getVehicle()));
 }

 private Driver resolveDriver(Driver driver){
  if(driver == null || driver.getId() == null){
   return null;
  }
  return driverRepo.findById(driver.getId()).orElseThrow();
 }

 private Vehicle resolveVehicle(Vehicle vehicle){
  if(vehicle == null || vehicle.getId() == null){
   return null;
  }
  return vehicleRepo.findById(vehicle.getId()).orElseThrow();
 }
}
