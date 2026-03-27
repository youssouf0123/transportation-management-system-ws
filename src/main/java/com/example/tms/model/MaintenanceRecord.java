package com.example.tms.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
public class MaintenanceRecord {

 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 private Long id;

 private String serviceType;
 private String status = "SCHEDULED";
 private LocalDate serviceDate;
 private Integer mileage;
 private double cost;
 private String notes;

 @JsonIgnore
 @ManyToOne(optional = false)
 private Organization organization;

 @ManyToOne
 private Vehicle vehicle;

 public Long getId(){ return id; }
 public void setId(Long id){ this.id=id; }

 public String getServiceType(){ return serviceType; }
 public void setServiceType(String serviceType){ this.serviceType=serviceType; }

 public String getStatus(){ return status; }
 public void setStatus(String status){ this.status=status; }

 public LocalDate getServiceDate(){ return serviceDate; }
 public void setServiceDate(LocalDate serviceDate){ this.serviceDate=serviceDate; }

 public Integer getMileage(){ return mileage; }
 public void setMileage(Integer mileage){ this.mileage=mileage; }

 public double getCost(){ return cost; }
 public void setCost(double cost){ this.cost=cost; }

 public String getNotes(){ return notes; }
 public void setNotes(String notes){ this.notes=notes; }

 public Organization getOrganization(){ return organization; }
 public void setOrganization(Organization organization){ this.organization=organization; }

 public Vehicle getVehicle(){ return vehicle; }
 public void setVehicle(Vehicle vehicle){ this.vehicle=vehicle; }
}
