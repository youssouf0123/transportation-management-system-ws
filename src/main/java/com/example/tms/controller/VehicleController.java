
package com.example.tms.controller;

import com.example.tms.model.Vehicle;
import com.example.tms.model.Driver;
import com.example.tms.model.AppUser;
import com.example.tms.repository.VehicleRepository;
import com.example.tms.repository.DriverRepository;
import com.example.tms.security.CurrentUserSupport;
import com.example.tms.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = {"http://localhost:8100", "http://localhost:4200"})
@RequestMapping("/vehicles")
public class VehicleController {

 private final VehicleRepository vehicleRepo;
 private final DriverRepository driverRepo;
 private final CurrentUserSupport currentUserSupport;
 private final AuditLogService auditLogService;

 public VehicleController(VehicleRepository v, DriverRepository d, CurrentUserSupport currentUserSupport, AuditLogService auditLogService){
  vehicleRepo=v;
  driverRepo=d;
  this.currentUserSupport=currentUserSupport;
  this.auditLogService = auditLogService;
 }

 @PostMapping
 public Vehicle create(@RequestBody Vehicle v, HttpServletRequest request){
  AppUser user = currentUserSupport.currentUser(request);
  currentUserSupport.requireRole(user, "OWNER", "MANAGER", "DISPATCHER");
  if(v.getDriver() != null && v.getDriver().getId() != null){
   Driver driver = driverRepo.findById(v.getDriver().getId()).orElseThrow();
   if(driver.getOrganization().getId().equals(user.getOrganization().getId())){ v.setDriver(driver); }
  }
  v.setOrganization(user.getOrganization());
  Vehicle saved = vehicleRepo.save(v);
  auditLogService.log(user, "CREATE", "VEHICLE", saved.getId(), "Created vehicle " + saved.getPlateNumber());
  return saved;
 }

 @PutMapping("/{id}")
 public Vehicle update(@PathVariable Long id, @RequestBody Vehicle incoming, HttpServletRequest request){
  AppUser user = currentUserSupport.currentUser(request);
  currentUserSupport.requireRole(user, "OWNER", "MANAGER", "DISPATCHER");
  Vehicle existing = vehicleRepo.findById(id).orElseThrow();
  if(!existing.getOrganization().getId().equals(user.getOrganization().getId())){ throw new RuntimeException("Forbidden"); }
  existing.setMake(incoming.getMake());
  existing.setModel(incoming.getModel());
  existing.setPlateNumber(incoming.getPlateNumber());
  existing.setStatus(incoming.getStatus());
  existing.setCurrentMileage(incoming.getCurrentMileage());
  if(incoming.getDriver() != null && incoming.getDriver().getId() != null){
   Driver driver = driverRepo.findById(incoming.getDriver().getId()).orElseThrow();
   if(driver.getOrganization().getId().equals(user.getOrganization().getId())){ existing.setDriver(driver); }
  } else {
   existing.setDriver(null);
  }
  Vehicle saved = vehicleRepo.save(existing);
  auditLogService.log(user, "UPDATE", "VEHICLE", saved.getId(), "Updated vehicle " + saved.getPlateNumber());
  return saved;
 }

 @DeleteMapping("/{id}")
 public void delete(@PathVariable Long id, HttpServletRequest request){
  AppUser user = currentUserSupport.currentUser(request);
  currentUserSupport.requireRole(user, "OWNER", "MANAGER");
  Vehicle existing = vehicleRepo.findById(id).orElseThrow();
  if(existing.getOrganization().getId().equals(user.getOrganization().getId())){
   auditLogService.log(user, "DELETE", "VEHICLE", existing.getId(), "Deleted vehicle " + existing.getPlateNumber());
   vehicleRepo.delete(existing);
  }
 }

 @PostMapping("/{vehicleId}/assignDriver/{driverId}")
 public Vehicle assignDriver(@PathVariable Long vehicleId, @PathVariable Long driverId, HttpServletRequest request){
  AppUser user = currentUserSupport.currentUser(request);
  currentUserSupport.requireRole(user, "OWNER", "MANAGER", "DISPATCHER");

  Vehicle v = vehicleRepo.findById(vehicleId).orElseThrow();
  Driver d = driverRepo.findById(driverId).orElseThrow();
  if(!v.getOrganization().getId().equals(user.getOrganization().getId()) || !d.getOrganization().getId().equals(user.getOrganization().getId())){ throw new RuntimeException("Forbidden"); }

  v.setDriver(d);
  Vehicle saved = vehicleRepo.save(v);
  auditLogService.log(user, "ASSIGN", "VEHICLE", saved.getId(), "Assigned driver " + d.getName() + " to vehicle " + saved.getPlateNumber());
  return saved;
 }

 @GetMapping
 public List<Vehicle> getAll(
   @RequestParam(required = false) String status,
   @RequestParam(required = false) String search,
   @RequestParam(required = false) Long driverId,
   HttpServletRequest request
 ){
  AppUser user = currentUserSupport.currentUser(request);
  return vehicleRepo.findAll().stream()
   .filter(vehicle -> vehicle.getOrganization().getId().equals(user.getOrganization().getId()))
   .filter(vehicle -> status == null || status.isBlank() || status.equalsIgnoreCase(vehicle.getStatus()))
   .filter(vehicle -> driverId == null || (vehicle.getDriver() != null && driverId.equals(vehicle.getDriver().getId())))
   .filter(vehicle -> {
    if(search == null || search.isBlank()){
     return true;
    }
    String q = search.toLowerCase();
    return (vehicle.getMake() != null && vehicle.getMake().toLowerCase().contains(q))
     || (vehicle.getModel() != null && vehicle.getModel().toLowerCase().contains(q))
     || (vehicle.getPlateNumber() != null && vehicle.getPlateNumber().toLowerCase().contains(q));
   })
   .toList();
 }
}
