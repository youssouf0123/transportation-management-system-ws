package com.example.tms.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class AuditLog {

 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 private Long id;

 private String action;
 private String entityType;
 private Long entityId;
 private String description;
 private String actorName;
 private String actorEmail;
 private String actorRole;
 private LocalDateTime createdAt = LocalDateTime.now();

 @JsonIgnore
 @ManyToOne(optional = false)
 private Organization organization;

 public Long getId(){ return id; }
 public void setId(Long id){ this.id = id; }

 public String getAction(){ return action; }
 public void setAction(String action){ this.action = action; }

 public String getEntityType(){ return entityType; }
 public void setEntityType(String entityType){ this.entityType = entityType; }

 public Long getEntityId(){ return entityId; }
 public void setEntityId(Long entityId){ this.entityId = entityId; }

 public String getDescription(){ return description; }
 public void setDescription(String description){ this.description = description; }

 public String getActorName(){ return actorName; }
 public void setActorName(String actorName){ this.actorName = actorName; }

 public String getActorEmail(){ return actorEmail; }
 public void setActorEmail(String actorEmail){ this.actorEmail = actorEmail; }

 public String getActorRole(){ return actorRole; }
 public void setActorRole(String actorRole){ this.actorRole = actorRole; }

 public LocalDateTime getCreatedAt(){ return createdAt; }
 public void setCreatedAt(LocalDateTime createdAt){ this.createdAt = createdAt; }

 public Organization getOrganization(){ return organization; }
 public void setOrganization(Organization organization){ this.organization = organization; }
}
