package com.example.tms.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
public class Trip {

 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 private Long id;

 private String origin;
 private String destination;
 private String status = "PLANNED";
 private LocalDate scheduledDate;
 private String cargoDescription;
 private String notes;

 @JsonIgnore
 @ManyToOne(optional = false)
 private Organization organization;

 @ManyToOne
 private Driver driver;

 @ManyToOne
 private Vehicle vehicle;

 public Long getId(){ return id; }
 public void setId(Long id){ this.id=id; }

 public String getOrigin(){ return origin; }
 public void setOrigin(String origin){ this.origin=origin; }

 public String getDestination(){ return destination; }
 public void setDestination(String destination){ this.destination=destination; }

 public String getStatus(){ return status; }
 public void setStatus(String status){ this.status=status; }

 public LocalDate getScheduledDate(){ return scheduledDate; }
 public void setScheduledDate(LocalDate scheduledDate){ this.scheduledDate=scheduledDate; }

 public String getCargoDescription(){ return cargoDescription; }
 public void setCargoDescription(String cargoDescription){ this.cargoDescription=cargoDescription; }

 public String getNotes(){ return notes; }
 public void setNotes(String notes){ this.notes=notes; }

 public Organization getOrganization(){ return organization; }
 public void setOrganization(Organization organization){ this.organization=organization; }

 public Driver getDriver(){ return driver; }
 public void setDriver(Driver driver){ this.driver=driver; }

 public Vehicle getVehicle(){ return vehicle; }
 public void setVehicle(Vehicle vehicle){ this.vehicle=vehicle; }
}
