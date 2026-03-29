package com.example.tms.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
public class Organization {

 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 private Long id;

 @Column(unique = true, nullable = false)
 private String name;

 private String status;

 @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = true)
 private List<AppUser> users = new ArrayList<>();

 @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = true)
 private List<Driver> drivers = new ArrayList<>();

 @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = true)
 private List<Vehicle> vehicles = new ArrayList<>();

 @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = true)
 private List<Trip> trips = new ArrayList<>();

 @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = true)
 private List<FinanceRecord> financeRecords = new ArrayList<>();

 @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = true)
 private List<MaintenanceRecord> maintenanceRecords = new ArrayList<>();

 @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = true)
 private List<DocumentRecord> documents = new ArrayList<>();

 @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = true)
 private List<AuditLog> auditLogs = new ArrayList<>();

 public Long getId(){ return id; }
 public void setId(Long id){ this.id=id; }

 public String getName(){ return name; }
 public void setName(String name){ this.name=name; }

 public String getStatus(){ return status; }
 public void setStatus(String status){ this.status=status; }

 public boolean isApproved(){
  return status == null || "APPROVED".equalsIgnoreCase(status);
 }
}
