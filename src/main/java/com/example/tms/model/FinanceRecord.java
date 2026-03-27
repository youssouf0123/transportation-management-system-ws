
package com.example.tms.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDate;

@Entity
public class FinanceRecord {

 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 private Long id;

 private String type; // EARNING or EXPENSE
 private String recordScope = "VEHICLE";
 private String category = "OPERATIONS";
 private String description;
 private String descriptionEn;
 private String descriptionFr;
 private double amount;

 private LocalDate date;

 @JsonIgnore
 @ManyToOne(optional = false)
 private Organization organization;

 @ManyToOne
 private Vehicle vehicle;

 @Transient
 private String inputLanguage;

 public Long getId(){ return id; }
 public void setId(Long id){ this.id=id; }

 public String getType(){ return type; }
 public void setType(String type){ this.type=type; }

 public String getRecordScope(){ return recordScope; }
 public void setRecordScope(String recordScope){ this.recordScope=recordScope; }

 public String getCategory(){ return category; }
 public void setCategory(String category){ this.category=category; }

 public String getDescription(){ return description; }
 public void setDescription(String description){ this.description=description; }

 public String getDescriptionEn(){ return descriptionEn; }
 public void setDescriptionEn(String descriptionEn){ this.descriptionEn=descriptionEn; }

 public String getDescriptionFr(){ return descriptionFr; }
 public void setDescriptionFr(String descriptionFr){ this.descriptionFr=descriptionFr; }

 public double getAmount(){ return amount; }
 public void setAmount(double amount){ this.amount=amount; }

 public LocalDate getDate(){ return date; }
 public void setDate(LocalDate date){ this.date=date; }

 public Organization getOrganization(){ return organization; }
 public void setOrganization(Organization organization){ this.organization=organization; }

 public Vehicle getVehicle(){ return vehicle; }
 public void setVehicle(Vehicle vehicle){ this.vehicle=vehicle; }

 public String getInputLanguage(){ return inputLanguage; }
 public void setInputLanguage(String inputLanguage){ this.inputLanguage=inputLanguage; }
}
