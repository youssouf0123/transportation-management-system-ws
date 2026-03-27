package com.example.tms.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
public class AppUser {

 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 private Long id;

 private String fullName;

 @Column(unique = true, nullable = false)
 private String email;

 private String role;

 @JsonIgnore
 private String passwordHash;

 @JsonIgnore
 private String authToken;

 @ManyToOne(optional = false)
 private Organization organization;

 public Long getId(){ return id; }
 public void setId(Long id){ this.id=id; }

 public String getFullName(){ return fullName; }
 public void setFullName(String fullName){ this.fullName=fullName; }

 public String getEmail(){ return email; }
 public void setEmail(String email){ this.email=email; }

 public String getRole(){ return role; }
 public void setRole(String role){ this.role=role; }

 public String getPasswordHash(){ return passwordHash; }
 public void setPasswordHash(String passwordHash){ this.passwordHash=passwordHash; }

 public String getAuthToken(){ return authToken; }
 public void setAuthToken(String authToken){ this.authToken=authToken; }

 public Organization getOrganization(){ return organization; }
 public void setOrganization(Organization organization){ this.organization=organization; }
}
