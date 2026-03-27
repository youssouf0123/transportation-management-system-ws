
package com.example.tms.controller;

import com.example.tms.model.Driver;
import com.example.tms.model.AppUser;
import com.example.tms.repository.DriverRepository;
import com.example.tms.security.CurrentUserSupport;
import com.example.tms.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = {"http://localhost:8100", "http://localhost:4200"})
@RequestMapping("/drivers")
public class DriverController {

 private final DriverRepository repo;
 private final CurrentUserSupport currentUserSupport;
 private final AuditLogService auditLogService;

 public DriverController(DriverRepository repo, CurrentUserSupport currentUserSupport, AuditLogService auditLogService){
  this.repo=repo;
  this.currentUserSupport=currentUserSupport;
  this.auditLogService = auditLogService;
 }

 @PostMapping
 public Driver create(@RequestBody Driver d, HttpServletRequest request){
  AppUser user = currentUserSupport.currentUser(request);
  currentUserSupport.requireRole(user, "OWNER", "MANAGER", "DISPATCHER");
  d.setOrganization(user.getOrganization());
  Driver saved = repo.save(d);
  auditLogService.log(user, "CREATE", "DRIVER", saved.getId(), "Created driver " + saved.getName());
  return saved;
 }

 @PutMapping("/{id}")
 public Driver update(@PathVariable Long id, @RequestBody Driver incoming, HttpServletRequest request){
  AppUser user = currentUserSupport.currentUser(request);
  currentUserSupport.requireRole(user, "OWNER", "MANAGER", "DISPATCHER");
  Driver existing = repo.findById(id).orElseThrow();
  if(!existing.getOrganization().getId().equals(user.getOrganization().getId())){ throw new RuntimeException("Forbidden"); }
  existing.setName(incoming.getName());
  existing.setLicenseNumber(incoming.getLicenseNumber());
  existing.setStatus(incoming.getStatus());
  existing.setPhoneNumber(incoming.getPhoneNumber());
  Driver saved = repo.save(existing);
  auditLogService.log(user, "UPDATE", "DRIVER", saved.getId(), "Updated driver " + saved.getName());
  return saved;
 }

 @DeleteMapping("/{id}")
 public void delete(@PathVariable Long id, HttpServletRequest request){
  AppUser user = currentUserSupport.currentUser(request);
  currentUserSupport.requireRole(user, "OWNER", "MANAGER");
  Driver existing = repo.findById(id).orElseThrow();
  if(existing.getOrganization().getId().equals(user.getOrganization().getId())){
   auditLogService.log(user, "DELETE", "DRIVER", existing.getId(), "Deleted driver " + existing.getName());
   repo.delete(existing);
  }
 }

 @GetMapping
 public List<Driver> getAll(
   @RequestParam(required = false) String status,
   @RequestParam(required = false) String search,
   HttpServletRequest request
 ){
  AppUser user = currentUserSupport.currentUser(request);
  return repo.findAll().stream()
   .filter(driver -> driver.getOrganization().getId().equals(user.getOrganization().getId()))
   .filter(driver -> status == null || status.isBlank() || status.equalsIgnoreCase(driver.getStatus()))
   .filter(driver -> {
    if(search == null || search.isBlank()){
     return true;
    }
    String q = search.toLowerCase();
    return (driver.getName() != null && driver.getName().toLowerCase().contains(q))
     || (driver.getLicenseNumber() != null && driver.getLicenseNumber().toLowerCase().contains(q))
     || (driver.getPhoneNumber() != null && driver.getPhoneNumber().toLowerCase().contains(q));
   })
   .toList();
 }
}
