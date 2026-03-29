package com.example.tms.controller;

import com.example.tms.model.AppUser;
import com.example.tms.model.Organization;
import com.example.tms.repository.AppUserRepository;
import com.example.tms.repository.OrganizationRepository;
import com.example.tms.security.CurrentUserSupport;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@CrossOrigin(origins = {"http://localhost:8100", "http://localhost:4200"})
@RequestMapping("/auth")
public class AuthController {

 private final OrganizationRepository organizationRepository;
 private final AppUserRepository userRepository;
 private final CurrentUserSupport currentUserSupport;
 private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

 public AuthController(
   OrganizationRepository organizationRepository,
   AppUserRepository userRepository,
   CurrentUserSupport currentUserSupport
 ){
  this.organizationRepository = organizationRepository;
  this.userRepository = userRepository;
  this.currentUserSupport = currentUserSupport;
 }

 @PostMapping("/register")
 public Map<String, Object> register(@RequestBody Map<String, String> payload){
  String organizationName = payload.get("organizationName");
  String fullName = payload.get("fullName");
  String email = payload.get("email");
  String password = payload.get("password");

  if(organizationName == null || fullName == null || email == null || password == null){
   throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing registration fields");
  }

  if(organizationRepository.findByName(organizationName).isPresent()){
   throw new ResponseStatusException(HttpStatus.CONFLICT, "Organization already exists");
  }

 if(userRepository.findByEmail(email).isPresent()){
  throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
 }

  boolean platformAdmin = "Youssouf Diarra".equalsIgnoreCase(fullName)
   && "dyoussouf12@gmail.com".equalsIgnoreCase(email);

  Organization organization = new Organization();
  organization.setName(organizationName);
  organization.setStatus(platformAdmin ? "APPROVED" : "PENDING");
  organization = organizationRepository.save(organization);

  AppUser user = new AppUser();
  user.setFullName(fullName);
  user.setEmail(email);
  user.setRole("OWNER");
  user.setStatus("ACTIVE");
  user.setPasswordHash(passwordEncoder.encode(password));
  user.setAuthToken(UUID.randomUUID().toString());
  user.setOrganization(organization);
  user = userRepository.save(user);

  if(!organization.isApproved()){
   Map<String, Object> response = new LinkedHashMap<>();
   response.put("pendingApproval", true);
   response.put("organizationStatus", organization.getStatus());
   response.put("message", "Workspace request sent for approval");
   return response;
  }

  return authResponse(user);
 }

 @PostMapping("/login")
 public Map<String, Object> login(@RequestBody Map<String, String> payload){
  String email = payload.get("email");
  String password = payload.get("password");

  AppUser user = userRepository.findByEmail(email).orElseThrow(() ->
   new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials")
  );

  if(!passwordEncoder.matches(password, user.getPasswordHash())){
   throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
  }

  if(!user.isActive()){
   throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User access has been revoked");
  }

  if(!user.getOrganization().isApproved()){
   throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Organization access is awaiting approval");
  }

  user.setAuthToken(UUID.randomUUID().toString());
  user = userRepository.save(user);

  return authResponse(user);
 }

 @GetMapping("/me")
 public Map<String, Object> me(HttpServletRequest request){
  return authResponse(currentUserSupport.currentUser(request));
 }

 private Map<String, Object> authResponse(AppUser user){
  Map<String, Object> payload = new LinkedHashMap<>();
  payload.put("token", user.getAuthToken());

  Map<String, Object> userMap = new LinkedHashMap<>();
  userMap.put("id", user.getId());
  userMap.put("fullName", user.getFullName());
  userMap.put("email", user.getEmail());
  userMap.put("role", user.getRole());
  userMap.put("status", user.getStatus() == null ? "ACTIVE" : user.getStatus());
  userMap.put("organizationName", user.getOrganization().getName());
  userMap.put("organizationId", user.getOrganization().getId());
  userMap.put("organizationStatus", user.getOrganization().getStatus() == null ? "APPROVED" : user.getOrganization().getStatus());
  payload.put("user", userMap);
  return payload;
 }
}
