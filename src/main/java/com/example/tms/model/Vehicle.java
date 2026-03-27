
package com.example.tms.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
public class Vehicle {

 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 private Long id;

 private String make;
 private String model;
 private String plateNumber;
 private String status = "ACTIVE";
 private Integer currentMileage;

 @JsonIgnore
 @ManyToOne(optional = false)
 private Organization organization;

 @ManyToOne
 private Driver driver;

 public Long getId(){ return id; }
 public void setId(Long id){ this.id=id; }

 public String getMake(){ return make; }
 public void setMake(String make){ this.make=make; }

 public String getModel(){ return model; }
 public void setModel(String model){ this.model=model; }

 public String getPlateNumber(){ return plateNumber; }
 public void setPlateNumber(String plateNumber){ this.plateNumber=plateNumber; }

 public String getStatus(){ return status; }
 public void setStatus(String status){ this.status=status; }

 public Integer getCurrentMileage(){ return currentMileage; }
 public void setCurrentMileage(Integer currentMileage){ this.currentMileage=currentMileage; }

 public Organization getOrganization(){ return organization; }
 public void setOrganization(Organization organization){ this.organization=organization; }

 public Driver getDriver(){ return driver; }
 public void setDriver(Driver driver){ this.driver=driver; }
}
