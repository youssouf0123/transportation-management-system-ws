
package com.example.tms.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
public class Driver {

 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 private Long id;

 private String name;
 private String licenseNumber;
 private String status = "AVAILABLE";
 private String phoneNumber;

 @JsonIgnore
 @ManyToOne(optional = false)
 private Organization organization;

 public Long getId(){ return id; }
 public void setId(Long id){ this.id=id; }

 public String getName(){ return name; }
 public void setName(String name){ this.name=name; }

 public String getLicenseNumber(){ return licenseNumber; }
 public void setLicenseNumber(String licenseNumber){ this.licenseNumber=licenseNumber; }

 public String getStatus(){ return status; }
 public void setStatus(String status){ this.status=status; }

 public String getPhoneNumber(){ return phoneNumber; }
 public void setPhoneNumber(String phoneNumber){ this.phoneNumber=phoneNumber; }

 public Organization getOrganization(){ return organization; }
 public void setOrganization(Organization organization){ this.organization=organization; }
}
