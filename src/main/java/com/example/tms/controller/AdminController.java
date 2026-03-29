package com.example.tms.controller;

import com.example.tms.model.AppUser;
import com.example.tms.model.Organization;
import com.example.tms.repository.AuditLogRepository;
import com.example.tms.repository.AppUserRepository;
import com.example.tms.repository.DocumentRecordRepository;
import com.example.tms.repository.DriverRepository;
import com.example.tms.repository.FinanceRepository;
import com.example.tms.repository.MaintenanceRecordRepository;
import com.example.tms.repository.OrganizationRepository;
import com.example.tms.repository.TripRepository;
import com.example.tms.repository.VehicleRepository;
import com.example.tms.security.CurrentUserSupport;
import com.example.tms.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminController {

 private final OrganizationRepository organizationRepository;
 private final AppUserRepository userRepository;
 private final DriverRepository driverRepository;
 private final VehicleRepository vehicleRepository;
 private final TripRepository tripRepository;
 private final FinanceRepository financeRepository;
 private final MaintenanceRecordRepository maintenanceRecordRepository;
 private final DocumentRecordRepository documentRecordRepository;
 private final AuditLogRepository auditLogRepository;
 private final CurrentUserSupport currentUserSupport;
 private final AuditLogService auditLogService;

 public AdminController(
  OrganizationRepository organizationRepository,
  AppUserRepository userRepository,
  DriverRepository driverRepository,
  VehicleRepository vehicleRepository,
  TripRepository tripRepository,
  FinanceRepository financeRepository,
  MaintenanceRecordRepository maintenanceRecordRepository,
  DocumentRecordRepository documentRecordRepository,
  AuditLogRepository auditLogRepository,
  CurrentUserSupport currentUserSupport,
  AuditLogService auditLogService
 ){
  this.organizationRepository = organizationRepository;
  this.userRepository = userRepository;
  this.driverRepository = driverRepository;
  this.vehicleRepository = vehicleRepository;
  this.tripRepository = tripRepository;
  this.financeRepository = financeRepository;
  this.maintenanceRecordRepository = maintenanceRecordRepository;
  this.documentRecordRepository = documentRecordRepository;
  this.auditLogRepository = auditLogRepository;
  this.currentUserSupport = currentUserSupport;
  this.auditLogService = auditLogService;
 }

 @GetMapping("/workspaces")
 public Map<String, Object> getWorkspaces(HttpServletRequest request){
  AppUser currentUser = currentUserSupport.currentUser(request);
  currentUserSupport.requirePlatformAdmin(currentUser);

  List<Map<String, Object>> organizations = organizationRepository.findAll().stream()
   .sorted(Comparator.comparing(Organization::getName, String.CASE_INSENSITIVE_ORDER))
   .map(this::toOrganizationMap)
   .toList();

  List<Map<String, Object>> requests = organizations.stream()
   .filter(organization -> !"APPROVED".equalsIgnoreCase(String.valueOf(organization.get("status"))))
   .toList();

  Map<String, Object> payload = new LinkedHashMap<>();
  payload.put("organizations", organizations);
  payload.put("requests", requests);
  return payload;
 }

 @PutMapping("/workspaces/{id}")
 public Map<String, Object> updateWorkspace(@PathVariable Long id, @RequestBody Map<String, String> payload, HttpServletRequest request){
  AppUser currentUser = currentUserSupport.currentUser(request);
  currentUserSupport.requirePlatformAdmin(currentUser);

  Organization organization = organizationRepository.findById(id).orElseThrow();
  String name = payload.get("name");
  if(name != null && !name.isBlank()){
   organizationRepository.findByName(name)
    .filter(existing -> !existing.getId().equals(id))
    .ifPresent(existing -> {
     throw new ResponseStatusException(HttpStatus.CONFLICT, "Organization already exists");
    });
   organization.setName(name);
  }
  if(payload.containsKey("status") && payload.get("status") != null && !payload.get("status").isBlank()){
   organization.setStatus(payload.get("status"));
  }
  Organization saved = organizationRepository.save(organization);
  auditLogService.log(currentUser, "UPDATE", "WORKSPACE", saved.getId(), "Updated workspace " + saved.getName());
  return toOrganizationMap(saved);
 }

 @PutMapping("/workspaces/{id}/status")
 public Map<String, Object> updateWorkspaceStatus(@PathVariable Long id, @RequestBody Map<String, String> payload, HttpServletRequest request){
  AppUser currentUser = currentUserSupport.currentUser(request);
  currentUserSupport.requirePlatformAdmin(currentUser);

  Organization organization = organizationRepository.findById(id).orElseThrow();
  String status = payload.getOrDefault("status", "PENDING");
  organization.setStatus(status);
  Organization saved = organizationRepository.save(organization);
  auditLogService.log(currentUser, "UPDATE", "WORKSPACE", saved.getId(), "Changed workspace " + saved.getName() + " status to " + status);
  return toOrganizationMap(saved);
 }

 @DeleteMapping("/workspaces/{id}")
 @ResponseStatus(HttpStatus.NO_CONTENT)
 @Transactional
 public void deleteWorkspace(@PathVariable Long id, HttpServletRequest request){
  AppUser currentUser = currentUserSupport.currentUser(request);
  currentUserSupport.requirePlatformAdmin(currentUser);

  Organization organization = organizationRepository.findById(id).orElseThrow();
  auditLogService.log(currentUser, "DELETE", "WORKSPACE", organization.getId(), "Deleted workspace " + organization.getName());
  documentRecordRepository.deleteByOrganization(organization);
  auditLogRepository.deleteByOrganization(organization);
  financeRepository.deleteByOrganization(organization);
  maintenanceRecordRepository.deleteByOrganization(organization);
  tripRepository.deleteByOrganization(organization);
  vehicleRepository.deleteByOrganization(organization);
  driverRepository.deleteByOrganization(organization);
  userRepository.deleteByOrganization(organization);
  organizationRepository.delete(organization);
 }

 @PutMapping("/users/{id}")
 public Map<String, Object> updateAnyUser(@PathVariable Long id, @RequestBody Map<String, String> payload, HttpServletRequest request){
  AppUser currentUser = currentUserSupport.currentUser(request);
  currentUserSupport.requirePlatformAdmin(currentUser);

  AppUser user = userRepository.findById(id).orElseThrow();
  String incomingEmail = payload.get("email");
  if(incomingEmail != null && !incomingEmail.equalsIgnoreCase(user.getEmail()) && userRepository.findByEmail(incomingEmail).isPresent()){
   throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
  }

  if(payload.containsKey("fullName")){
   user.setFullName(payload.get("fullName"));
  }
  if(incomingEmail != null){
   user.setEmail(incomingEmail);
  }
  if(payload.containsKey("role")){
   user.setRole(payload.get("role"));
  }
  if(payload.containsKey("status") && payload.get("status") != null && !payload.get("status").isBlank()){
   user.setStatus(payload.get("status"));
   if(!"ACTIVE".equalsIgnoreCase(payload.get("status"))){
    user.setAuthToken(null);
   }
  }
  AppUser saved = userRepository.save(user);
  auditLogService.log(currentUser, "UPDATE", "USER", saved.getId(), "Updated platform user " + saved.getEmail());
  return toUserMap(saved);
 }

 @PutMapping("/users/{id}/status")
 public Map<String, Object> updateAnyUserStatus(@PathVariable Long id, @RequestBody Map<String, String> payload, HttpServletRequest request){
  AppUser currentUser = currentUserSupport.currentUser(request);
  currentUserSupport.requirePlatformAdmin(currentUser);

  AppUser user = userRepository.findById(id).orElseThrow();
  if(currentUser.getId().equals(user.getId()) && !"ACTIVE".equalsIgnoreCase(payload.getOrDefault("status", "ACTIVE"))){
   throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot revoke yourself");
  }
  String status = payload.getOrDefault("status", "ACTIVE");
  user.setStatus(status);
  if(!"ACTIVE".equalsIgnoreCase(status)){
   user.setAuthToken(null);
  }
  AppUser saved = userRepository.save(user);
  auditLogService.log(currentUser, "UPDATE", "USER", saved.getId(), "Changed platform user access for " + saved.getEmail() + " to " + status);
  return toUserMap(saved);
 }

 @DeleteMapping("/users/{id}")
 @ResponseStatus(HttpStatus.NO_CONTENT)
 public void deleteAnyUser(@PathVariable Long id, HttpServletRequest request){
  AppUser currentUser = currentUserSupport.currentUser(request);
  currentUserSupport.requirePlatformAdmin(currentUser);

  AppUser user = userRepository.findById(id).orElseThrow();
  if(currentUser.getId().equals(user.getId())){
   throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot delete yourself");
  }
  auditLogService.log(currentUser, "DELETE", "USER", user.getId(), "Deleted platform user " + user.getEmail());
  userRepository.delete(user);
 }

 private Map<String, Object> toOrganizationMap(Organization organization){
  Map<String, Object> data = new LinkedHashMap<>();
  data.put("id", organization.getId());
  data.put("name", organization.getName());
  data.put("status", organization.getStatus() == null ? "APPROVED" : organization.getStatus());
  data.put("users", userRepository.findByOrganization(organization).stream()
   .sorted(Comparator.comparing(AppUser::getFullName, String.CASE_INSENSITIVE_ORDER))
   .map(this::toUserMap)
   .toList());
  return data;
 }

 private Map<String, Object> toUserMap(AppUser user){
  Map<String, Object> data = new LinkedHashMap<>();
  data.put("id", user.getId());
  data.put("fullName", user.getFullName());
  data.put("email", user.getEmail());
  data.put("role", user.getRole());
  data.put("status", user.getStatus() == null ? "ACTIVE" : user.getStatus());
  data.put("organizationId", user.getOrganization().getId());
  data.put("organizationName", user.getOrganization().getName());
  return data;
 }
}
