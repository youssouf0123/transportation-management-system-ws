package com.example.tms.controller;

import com.example.tms.model.AppUser;
import com.example.tms.repository.AppUserRepository;
import com.example.tms.repository.AuditLogRepository;
import com.example.tms.repository.DocumentRecordRepository;
import com.example.tms.repository.DriverRepository;
import com.example.tms.repository.FinanceRepository;
import com.example.tms.repository.MaintenanceRecordRepository;
import com.example.tms.repository.TripRepository;
import com.example.tms.repository.VehicleRepository;
import com.example.tms.security.CurrentUserSupport;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@CrossOrigin(origins = {"http://localhost:8100", "http://localhost:4200"})
@RequestMapping("/search")
public class SearchController {

 private final DriverRepository driverRepository;
 private final VehicleRepository vehicleRepository;
 private final TripRepository tripRepository;
 private final FinanceRepository financeRepository;
 private final MaintenanceRecordRepository maintenanceRecordRepository;
 private final DocumentRecordRepository documentRecordRepository;
 private final AppUserRepository appUserRepository;
 private final AuditLogRepository auditLogRepository;
 private final CurrentUserSupport currentUserSupport;

 public SearchController(
  DriverRepository driverRepository,
  VehicleRepository vehicleRepository,
  TripRepository tripRepository,
  FinanceRepository financeRepository,
  MaintenanceRecordRepository maintenanceRecordRepository,
  DocumentRecordRepository documentRecordRepository,
  AppUserRepository appUserRepository,
  AuditLogRepository auditLogRepository,
  CurrentUserSupport currentUserSupport
 ) {
  this.driverRepository = driverRepository;
  this.vehicleRepository = vehicleRepository;
  this.tripRepository = tripRepository;
  this.financeRepository = financeRepository;
  this.maintenanceRecordRepository = maintenanceRecordRepository;
  this.documentRecordRepository = documentRecordRepository;
  this.appUserRepository = appUserRepository;
  this.auditLogRepository = auditLogRepository;
  this.currentUserSupport = currentUserSupport;
 }

 @GetMapping
 public SearchResponse search(@RequestParam String q, @RequestParam(defaultValue = "en") String lang, HttpServletRequest request) {
  AppUser user = currentUserSupport.currentUser(request);
  String query = q == null ? "" : q.trim();
  boolean french = "fr".equalsIgnoreCase(lang);
  if(query.isBlank()) {
   return new SearchResponse(query, 0, List.of());
  }

  String needle = query.toLowerCase(Locale.ROOT);
  List<SearchSection> sections = new ArrayList<>();

  List<SearchItem> driverResults = driverRepository.findAll().stream()
   .filter(driver -> driver.getOrganization().getId().equals(user.getOrganization().getId()))
   .filter(driver -> contains(needle, driver.getName(), driver.getLicenseNumber(), driver.getPhoneNumber(), driver.getStatus()))
   .limit(5)
   .map(driver -> new SearchItem(
    "driver",
    driver.getId(),
    driver.getName(),
    label(french, "Numéro de permis", "License") + ": " + safe(driver.getLicenseNumber())
     + " • " + label(french, "Statut", "Status") + ": " + localizeStatus(driver.getStatus(), french),
    "/drivers/list",
    Map.of("driverId", driver.getId())
   ))
   .toList();
  if(!driverResults.isEmpty()) {
   sections.add(new SearchSection("drivers", label(french, "Chauffeurs", "Drivers"), driverResults));
  }

  List<SearchItem> vehicleResults = vehicleRepository.findAll().stream()
   .filter(vehicle -> vehicle.getOrganization().getId().equals(user.getOrganization().getId()))
   .filter(vehicle -> contains(needle, vehicle.getMake(), vehicle.getModel(), vehicle.getPlateNumber(), vehicle.getStatus(),
    vehicle.getDriver() != null ? vehicle.getDriver().getName() : null))
   .limit(5)
   .map(vehicle -> new SearchItem(
    "vehicle",
    vehicle.getId(),
    safe(vehicle.getMake()) + " " + safe(vehicle.getModel()),
    label(french, "Plaque", "Plate") + ": " + safe(vehicle.getPlateNumber())
     + " • " + label(french, "Chauffeur", "Driver") + ": "
     + safe(vehicle.getDriver() != null ? vehicle.getDriver().getName() : label(french, "Non affecté", "Unassigned")),
    "/vehicles/list",
    Map.of("vehicleId", vehicle.getId())
   ))
   .toList();
  if(!vehicleResults.isEmpty()) {
   sections.add(new SearchSection("vehicles", label(french, "Véhicules", "Vehicles"), vehicleResults));
  }

  List<SearchItem> tripResults = tripRepository.findAll().stream()
   .filter(trip -> trip.getOrganization().getId().equals(user.getOrganization().getId()))
   .filter(trip -> contains(needle, trip.getOrigin(), trip.getDestination(), trip.getStatus(), trip.getCargoDescription(), trip.getNotes(),
    trip.getDriver() != null ? trip.getDriver().getName() : null,
    trip.getVehicle() != null ? trip.getVehicle().getPlateNumber() : null))
   .limit(5)
   .map(trip -> new SearchItem(
    "trip",
    trip.getId(),
    safe(trip.getOrigin()) + " " + label(french, "vers", "to") + " " + safe(trip.getDestination()),
    label(french, "Date", "Date") + ": " + String.valueOf(trip.getScheduledDate())
     + " • " + label(french, "Statut", "Status") + ": " + localizeStatus(trip.getStatus(), french)
     + " • " + label(french, "Chauffeur", "Driver") + ": "
     + safe(trip.getDriver() != null ? trip.getDriver().getName() : label(french, "Non affecté", "Unassigned")),
    "/trips/list",
    Map.of("tripId", trip.getId())
   ))
   .toList();
  if(!tripResults.isEmpty()) {
   sections.add(new SearchSection("trips", label(french, "Trajets", "Trips"), tripResults));
  }

  List<SearchItem> financeResults = financeRepository.findAll().stream()
   .filter(record -> record.getOrganization().getId().equals(user.getOrganization().getId()))
   .filter(record -> contains(needle, record.getDescription(), record.getDescriptionEn(), record.getDescriptionFr(),
    record.getType(), record.getCategory(), record.getRecordScope(),
    record.getVehicle() != null ? record.getVehicle().getPlateNumber() : null,
    record.getVehicle() != null && record.getVehicle().getDriver() != null ? record.getVehicle().getDriver().getName() : null,
    String.valueOf(record.getAmount())))
   .limit(5)
   .map(record -> new SearchItem(
    "finance",
    record.getId(),
    safe(firstPresent(
      french ? record.getDescriptionFr() : record.getDescriptionEn(),
      record.getDescription(),
      record.getDescriptionEn(),
      record.getDescriptionFr(),
      french ? "Écriture financière" : "Finance Record"
    )),
    label(french, "Type", "Type") + ": " + localizeFinanceType(record.getType(), french)
     + " • " + label(french, "Montant", "Amount") + ": " + record.getAmount()
     + " • " + label(french, "Chauffeur", "Driver") + ": "
     + safe(record.getVehicle() != null && record.getVehicle().getDriver() != null ? record.getVehicle().getDriver().getName() : label(french, "Non affecté", "Unassigned")),
    "/finance/list",
    Map.of("recordId", record.getId())
   ))
   .toList();
  if(!financeResults.isEmpty()) {
   sections.add(new SearchSection("finance", label(french, "Finance", "Finance"), financeResults));
  }

  List<SearchItem> maintenanceResults = maintenanceRecordRepository.findAll().stream()
   .filter(record -> record.getOrganization().getId().equals(user.getOrganization().getId()))
   .filter(record -> contains(needle, record.getServiceType(), record.getStatus(), record.getNotes(),
    record.getVehicle() != null ? record.getVehicle().getPlateNumber() : null))
   .limit(5)
   .map(record -> new SearchItem(
    "maintenance",
    record.getId(),
    safe(record.getServiceType()),
    label(french, "Date", "Date") + ": " + String.valueOf(record.getServiceDate())
     + " • " + label(french, "Véhicule", "Vehicle") + ": "
     + safe(record.getVehicle() != null ? record.getVehicle().getPlateNumber() : label(french, "Aucun véhicule", "No vehicle")),
    "/maintenance/list",
    Map.of("recordId", record.getId())
   ))
   .toList();
  if(!maintenanceResults.isEmpty()) {
   sections.add(new SearchSection("maintenance", label(french, "Maintenance", "Maintenance"), maintenanceResults));
  }

  List<SearchItem> documentResults = documentRecordRepository.findAll().stream()
   .filter(document -> document.getOrganization().getId().equals(user.getOrganization().getId()))
   .filter(document -> contains(needle, document.getTitle(), document.getDocumentType(), document.getEntityType(), document.getStatus(),
    document.getFileName(), document.getNotes()))
   .limit(5)
   .map(document -> new SearchItem(
    "document",
    document.getId(),
    safe(document.getTitle()),
    label(french, "Type", "Type") + ": " + localizeDocumentType(document.getDocumentType(), french)
     + " • " + label(french, "Fichier", "File") + ": " + safe(document.getFileName())
     + " • " + label(french, "Chauffeur", "Driver") + ": "
     + safe(resolveRelatedDriverName(document.getEntityType(), document.getEntityId(), user.getOrganization().getId(), french)),
    "/documents",
    Map.of("documentId", document.getId())
   ))
   .toList();
  if(!documentResults.isEmpty()) {
   sections.add(new SearchSection("documents", label(french, "Documents", "Documents"), documentResults));
  }

  if(isManager(user)) {
   List<SearchItem> userResults = appUserRepository.findAll().stream()
    .filter(appUser -> appUser.getOrganization().getId().equals(user.getOrganization().getId()))
    .filter(appUser -> contains(needle, appUser.getFullName(), appUser.getEmail(), appUser.getRole()))
    .limit(5)
    .map(appUser -> new SearchItem(
     "user",
     appUser.getId(),
     safe(appUser.getFullName()),
     safe(appUser.getEmail()) + " • " + localizeRole(appUser.getRole(), french),
     "/users",
     Map.of("userId", appUser.getId())
    ))
    .toList();
   if(!userResults.isEmpty()) {
    sections.add(new SearchSection("users", label(french, "Utilisateurs", "Users"), userResults));
   }

   List<SearchItem> auditResults = auditLogRepository.findAll().stream()
    .filter(log -> log.getOrganization().getId().equals(user.getOrganization().getId()))
    .filter(log -> contains(needle, log.getAction(), log.getEntityType(), log.getDescription(), log.getActorName(), log.getActorEmail()))
    .limit(5)
    .map(log -> new SearchItem(
     "audit",
     log.getId(),
     localizeEntity(log.getEntityType(), french) + " • " + localizeAction(log.getAction(), french),
     localizeAuditDescription(log.getDescription(), french)
      + " • " + label(french, "Chauffeur", "Driver") + ": "
      + safe(resolveRelatedDriverName(log.getEntityType(), log.getEntityId(), user.getOrganization().getId(), french)),
     "/audit",
     Map.of("auditId", log.getId())
    ))
    .toList();
   if(!auditResults.isEmpty()) {
    sections.add(new SearchSection("audit", label(french, "Journal d'audit", "Audit Log"), auditResults));
   }
  }

  int totalResults = sections.stream().mapToInt(section -> section.results().size()).sum();
  return new SearchResponse(query, totalResults, sections);
 }

 private boolean contains(String needle, String... values) {
  for(String value : values) {
   if(value != null && value.toLowerCase(Locale.ROOT).contains(needle)) {
    return true;
   }
  }
  return false;
 }

 private boolean isManager(AppUser user) {
  return "OWNER".equalsIgnoreCase(user.getRole()) || "MANAGER".equalsIgnoreCase(user.getRole());
 }

 private String safe(String value) {
  return value == null || value.isBlank() ? "N/A" : value;
 }

 private String firstPresent(String... values) {
  for(String value : values) {
   if(value != null && !value.isBlank()) {
    return value;
   }
  }
  return "";
 }

 private String label(boolean french, String frenchText, String englishText) {
  return french ? frenchText : englishText;
 }

 private String localizeStatus(String status, boolean french) {
  if(status == null) return "";
  return switch (status.toUpperCase(Locale.ROOT)) {
   case "AVAILABLE" -> label(french, "Disponible", "Available");
   case "ON_TRIP" -> label(french, "En trajet", "On Trip");
   case "OFF_DUTY" -> label(french, "Hors service", "Off Duty");
   case "ACTIVE" -> label(french, "Actif", "Active");
   case "IN_SERVICE" -> label(french, "En service", "In Service");
   case "OUT_OF_SERVICE" -> label(french, "Hors service", "Out Of Service");
   case "SCHEDULED" -> label(french, "Planifié", "Scheduled");
   case "COMPLETED" -> label(french, "Terminé", "Completed");
   case "PLANNED" -> label(french, "Planifié", "Planned");
   case "IN_PROGRESS" -> label(french, "En cours", "In Progress");
   case "EXPIRED" -> label(french, "Expiré", "Expired");
   case "ARCHIVED" -> label(french, "Archivé", "Archived");
   default -> safe(status);
  };
 }

 private String localizeFinanceType(String type, boolean french) {
  if(type == null) return "";
  return switch (type.toUpperCase(Locale.ROOT)) {
   case "EARNING" -> label(french, "Recette", "Earning");
   case "EXPENSE" -> label(french, "Dépense", "Expense");
   default -> safe(type);
  };
 }

 private String localizeDocumentType(String type, boolean french) {
  if(type == null) return "";
  return switch (type.toUpperCase(Locale.ROOT)) {
   case "LICENSE" -> label(french, "Permis", "License");
   case "INSURANCE" -> label(french, "Assurance", "Insurance");
   case "REGISTRATION" -> label(french, "Immatriculation", "Registration");
   case "CONTRACT" -> label(french, "Contrat", "Contract");
   case "RECEIPT" -> label(french, "Reçu", "Receipt");
   default -> safe(type);
  };
 }

 private String localizeRole(String role, boolean french) {
  if(role == null) return "";
  return switch (role.toUpperCase(Locale.ROOT)) {
   case "OWNER" -> label(french, "Propriétaire", "Owner");
   case "MANAGER" -> label(french, "Gestionnaire", "Manager");
   case "DISPATCHER" -> label(french, "Dispatcher", "Dispatcher");
   case "FINANCE" -> label(french, "Finance", "Finance");
   case "VIEWER" -> label(french, "Lecteur", "Viewer");
   default -> safe(role);
  };
 }

 private String localizeEntity(String entity, boolean french) {
  if(entity == null) return "";
  return switch (entity.toUpperCase(Locale.ROOT)) {
   case "DRIVER" -> label(french, "Chauffeur", "Driver");
   case "VEHICLE" -> label(french, "Véhicule", "Vehicle");
   case "TRIP" -> label(french, "Trajet", "Trip");
   case "FINANCE" -> label(french, "Finance", "Finance");
   case "WORKSPACE" -> label(french, "Espace de travail", "Workspace");
   case "MAINTENANCE" -> label(french, "Maintenance", "Maintenance");
   case "DOCUMENT" -> label(french, "Document", "Document");
   case "USER" -> label(french, "Utilisateur", "User");
   default -> safe(entity);
  };
 }

 private String localizeAction(String action, boolean french) {
  if(action == null) return "";
  return switch (action.toUpperCase(Locale.ROOT)) {
   case "CREATE" -> label(french, "Création", "Create");
   case "UPDATE" -> label(french, "Mise à jour", "Update");
   case "DELETE" -> label(french, "Suppression", "Delete");
   case "ASSIGN" -> label(french, "Affectation", "Assign");
   case "UPDATE_ROLE" -> label(french, "Changement de rôle", "Role Change");
   default -> safe(action);
  };
 }

 private String localizeAuditDescription(String description, boolean french) {
  if(description == null || description.isBlank() || !french) {
   return safe(description);
  }

  return description
   .replace("Created trip ", "Trajet créé ")
   .replace("Updated trip ", "Trajet mis à jour ")
   .replace("Deleted trip ", "Trajet supprimé ")
   .replace(" to ", " vers ")
   .replace("Created driver ", "Chauffeur créé ")
   .replace("Updated driver ", "Chauffeur mis à jour ")
   .replace("Deleted driver ", "Chauffeur supprimé ")
   .replace("Created vehicle ", "Véhicule créé ")
   .replace("Updated vehicle ", "Véhicule mis à jour ")
   .replace("Deleted vehicle ", "Véhicule supprimé ")
   .replace("Assigned driver ", "Chauffeur affecté ")
   .replace(" to vehicle ", " au véhicule ")
   .replace("Created maintenance record ", "Maintenance créée ")
   .replace("Updated maintenance record ", "Maintenance mise à jour ")
   .replace("Deleted maintenance record ", "Maintenance supprimée ")
   .replace("Added document ", "Document ajouté ")
   .replace("Updated document ", "Document mis à jour ")
   .replace("Deleted document ", "Document supprimé ")
   .replace("Added miscellaneous finance record ", "Écriture diverse ajoutée ")
   .replace("Updated finance record ", "Écriture financière mise à jour ")
   .replace("Deleted finance record ", "Écriture financière supprimée ")
   .replace("Added team member ", "Membre ajouté ")
   .replace("Changed role for ", "Rôle modifié pour ")
   .replace(" to OWNER", " en Propriétaire")
   .replace(" to MANAGER", " en Gestionnaire")
   .replace(" to DISPATCHER", " en Dispatcher")
   .replace(" to FINANCE", " en Finance")
   .replace(" to VIEWER", " en Lecteur")
   .replace("Updated team member ", "Membre mis à jour ")
   .replace("Deleted team member ", "Membre supprimé ")
   .replace("Changed user access for ", "Accès utilisateur modifié pour ")
   .replace("Changed platform user access for ", "Accès utilisateur plateforme modifié pour ")
   .replace("Updated platform user ", "Utilisateur plateforme mis à jour ")
   .replace("Deleted platform user ", "Utilisateur plateforme supprimé ")
   .replace("Updated workspace ", "Espace de travail mis à jour ")
   .replace("Changed workspace ", "Espace de travail modifié ")
   .replace(" status to ", " statut vers ")
   .replace("Deleted workspace ", "Espace de travail supprimé ")
   .replace("Added EARNING record for vehicle ", "Recette ajoutée pour le véhicule ")
   .replace("Added EXPENSE record for vehicle ", "Dépense ajoutée pour le véhicule ");
 }

 private String resolveRelatedDriverName(String entityType, Long entityId, Long organizationId, boolean french) {
  if(entityType == null || entityId == null) {
   return label(french, "Non affecté", "Unassigned");
  }

  return switch (entityType.toUpperCase(Locale.ROOT)) {
   case "DRIVER" -> driverRepository.findAll().stream()
    .filter(driver -> driver.getOrganization().getId().equals(organizationId))
    .filter(driver -> driver.getId().equals(entityId))
    .map(driver -> safe(driver.getName()))
    .findFirst()
    .orElse(label(french, "Non affecté", "Unassigned"));
   case "VEHICLE" -> vehicleRepository.findAll().stream()
    .filter(vehicle -> vehicle.getOrganization().getId().equals(organizationId))
    .filter(vehicle -> vehicle.getId().equals(entityId))
    .map(vehicle -> vehicle.getDriver() != null ? safe(vehicle.getDriver().getName()) : label(french, "Non affecté", "Unassigned"))
    .findFirst()
    .orElse(label(french, "Non affecté", "Unassigned"));
   case "TRIP" -> tripRepository.findAll().stream()
    .filter(trip -> trip.getOrganization().getId().equals(organizationId))
    .filter(trip -> trip.getId().equals(entityId))
    .map(trip -> trip.getDriver() != null ? safe(trip.getDriver().getName()) : label(french, "Non affecté", "Unassigned"))
    .findFirst()
    .orElse(label(french, "Non affecté", "Unassigned"));
   case "FINANCE" -> financeRepository.findAll().stream()
    .filter(record -> record.getOrganization().getId().equals(organizationId))
    .filter(record -> record.getId().equals(entityId))
    .map(record -> record.getVehicle() != null && record.getVehicle().getDriver() != null
     ? safe(record.getVehicle().getDriver().getName())
     : label(french, "Non affecté", "Unassigned"))
    .findFirst()
    .orElse(label(french, "Non affecté", "Unassigned"));
   case "MAINTENANCE" -> maintenanceRecordRepository.findAll().stream()
    .filter(record -> record.getOrganization().getId().equals(organizationId))
    .filter(record -> record.getId().equals(entityId))
    .map(record -> record.getVehicle() != null && record.getVehicle().getDriver() != null
     ? safe(record.getVehicle().getDriver().getName())
     : label(french, "Non affecté", "Unassigned"))
    .findFirst()
    .orElse(label(french, "Non affecté", "Unassigned"));
   default -> label(french, "Non affecté", "Unassigned");
  };
 }

 public record SearchResponse(String query, int totalResults, List<SearchSection> sections) {}
 public record SearchSection(String key, String title, List<SearchItem> results) {}
 public record SearchItem(String type, Long id, String title, String subtitle, String route, Map<String, Object> queryParams) {}
}
