package com.example.tms.controller;

import com.example.tms.model.AppUser;
import com.example.tms.repository.AppUserRepository;
import com.example.tms.security.CurrentUserSupport;
import com.example.tms.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@CrossOrigin(origins = {"http://localhost:8100", "http://localhost:4200"})
@RequestMapping("/users")
public class UserController {

 private final AppUserRepository userRepository;
 private final CurrentUserSupport currentUserSupport;
 private final AuditLogService auditLogService;
 private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

 public UserController(AppUserRepository userRepository, CurrentUserSupport currentUserSupport, AuditLogService auditLogService){
  this.userRepository = userRepository;
  this.currentUserSupport = currentUserSupport;
  this.auditLogService = auditLogService;
 }

 @GetMapping
 public List<Map<String, Object>> getUsers(HttpServletRequest request){
  AppUser currentUser = currentUserSupport.currentUser(request);
  return userRepository.findByOrganization(currentUser.getOrganization()).stream()
   .map(this::toUserMap)
   .toList();
 }

 @PostMapping
 public Map<String, Object> createUser(@RequestBody Map<String, String> payload, HttpServletRequest request){
  AppUser currentUser = currentUserSupport.currentUser(request);
  currentUserSupport.requireRole(currentUser, "OWNER", "MANAGER");

  String fullName = payload.get("fullName");
  String email = payload.get("email");
  String password = payload.get("password");
  String role = payload.getOrDefault("role", "VIEWER");

  if(userRepository.findByEmail(email).isPresent()){
   throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
  }

  AppUser user = new AppUser();
  user.setFullName(fullName);
  user.setEmail(email);
  user.setRole(role);
  user.setStatus("ACTIVE");
  user.setPasswordHash(passwordEncoder.encode(password));
  user.setAuthToken(UUID.randomUUID().toString());
  user.setOrganization(currentUser.getOrganization());
  AppUser saved = userRepository.save(user);
  auditLogService.log(currentUser, "CREATE", "USER", saved.getId(), "Added team member " + saved.getEmail() + " as " + saved.getRole());
  return toUserMap(saved);
 }

 @PutMapping("/{id}/role")
 public Map<String, Object> updateRole(@PathVariable Long id, @RequestBody Map<String, String> payload, HttpServletRequest request){
  AppUser currentUser = currentUserSupport.currentUser(request);
  currentUserSupport.requireRole(currentUser, "OWNER", "MANAGER");

  AppUser user = userRepository.findById(id).orElseThrow();
  if(!user.getOrganization().getId().equals(currentUser.getOrganization().getId())){
   throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong organization");
  }
  user.setRole(payload.get("role"));
  AppUser saved = userRepository.save(user);
  auditLogService.log(currentUser, "UPDATE_ROLE", "USER", saved.getId(), "Changed role for " + saved.getEmail() + " to " + saved.getRole());
  return toUserMap(saved);
 }

 @PutMapping("/{id}")
 public Map<String, Object> updateUser(@PathVariable Long id, @RequestBody Map<String, String> payload, HttpServletRequest request){
  AppUser currentUser = currentUserSupport.currentUser(request);
  currentUserSupport.requireRole(currentUser, "OWNER", "MANAGER");

  AppUser user = userRepository.findById(id).orElseThrow();
  if(!user.getOrganization().getId().equals(currentUser.getOrganization().getId())){
   throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong organization");
  }

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
  }
  if(payload.containsKey("password") && payload.get("password") != null && !payload.get("password").isBlank()){
   user.setPasswordHash(passwordEncoder.encode(payload.get("password")));
  }

  AppUser saved = userRepository.save(user);
  auditLogService.log(currentUser, "UPDATE", "USER", saved.getId(), "Updated team member " + saved.getEmail());
  return toUserMap(saved);
 }

 @DeleteMapping("/{id}")
 @ResponseStatus(HttpStatus.NO_CONTENT)
 public void deleteUser(@PathVariable Long id, HttpServletRequest request){
  AppUser currentUser = currentUserSupport.currentUser(request);
  currentUserSupport.requireRole(currentUser, "OWNER");

  AppUser user = userRepository.findById(id).orElseThrow();
  if(!user.getOrganization().getId().equals(currentUser.getOrganization().getId())){
   throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong organization");
  }
  if(user.getId().equals(currentUser.getId())){
   throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot delete yourself");
  }

  auditLogService.log(currentUser, "DELETE", "USER", user.getId(), "Deleted team member " + user.getEmail());
  userRepository.delete(user);
 }

 @PutMapping("/{id}/status")
 public Map<String, Object> updateUserStatus(@PathVariable Long id, @RequestBody Map<String, String> payload, HttpServletRequest request){
  AppUser currentUser = currentUserSupport.currentUser(request);
  currentUserSupport.requireRole(currentUser, "OWNER", "MANAGER");

  AppUser user = userRepository.findById(id).orElseThrow();
  if(!user.getOrganization().getId().equals(currentUser.getOrganization().getId())){
   throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong organization");
  }
  if(user.getId().equals(currentUser.getId())){
   throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot revoke yourself");
  }

  String status = payload.getOrDefault("status", "ACTIVE");
  user.setStatus(status);
  if(!"ACTIVE".equalsIgnoreCase(status)){
   user.setAuthToken(null);
  }
  AppUser saved = userRepository.save(user);
  auditLogService.log(currentUser, "UPDATE", "USER", saved.getId(), "Changed user access for " + saved.getEmail() + " to " + status);
  return toUserMap(saved);
 }

 private Map<String, Object> toUserMap(AppUser user){
  Map<String, Object> data = new LinkedHashMap<>();
  data.put("id", user.getId());
  data.put("fullName", user.getFullName());
  data.put("email", user.getEmail());
  data.put("role", user.getRole());
  data.put("status", user.getStatus() == null ? "ACTIVE" : user.getStatus());
  data.put("organizationName", user.getOrganization().getName());
  data.put("organizationId", user.getOrganization().getId());
  return data;
 }
}
