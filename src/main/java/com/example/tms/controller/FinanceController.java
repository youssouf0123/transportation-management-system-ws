
package com.example.tms.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.tms.model.AppUser;
import com.example.tms.model.FinanceRecord;
import com.example.tms.model.Vehicle;
import com.example.tms.repository.FinanceRepository;
import com.example.tms.repository.VehicleRepository;
import com.example.tms.security.CurrentUserSupport;
import com.example.tms.service.AuditLogService;
import com.example.tms.service.DeepLTranslationService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@CrossOrigin(origins = {"http://localhost:8100", "http://localhost:4200"})
@RequestMapping("/finance")
public class FinanceController {

 private final FinanceRepository financeRepo;
 private final VehicleRepository vehicleRepo;
 private final CurrentUserSupport currentUserSupport;
 private final AuditLogService auditLogService;
 private final DeepLTranslationService translationService;

 public FinanceController(
  FinanceRepository f,
  VehicleRepository v,
  CurrentUserSupport currentUserSupport,
  AuditLogService auditLogService,
  DeepLTranslationService translationService
 ){
  financeRepo=f;
  vehicleRepo=v;
  this.currentUserSupport=currentUserSupport;
  this.auditLogService = auditLogService;
  this.translationService = translationService;
 }

 @PostMapping("/{vehicleId}")
 public FinanceRecord addRecord(@PathVariable Long vehicleId, @RequestBody FinanceRecord record, HttpServletRequest request){
  AppUser user = currentUserSupport.currentUser(request);
  currentUserSupport.requireRole(user, "OWNER", "MANAGER", "FINANCE");

  Vehicle v = vehicleRepo.findById(vehicleId).orElseThrow();
  if(!v.getOrganization().getId().equals(user.getOrganization().getId())){ throw new RuntimeException("Forbidden"); }
  record.setVehicle(v);
  record.setOrganization(user.getOrganization());
  record.setRecordScope("VEHICLE");
  if(record.getCategory() == null || record.getCategory().isBlank()){
   record.setCategory("OPERATIONS");
  }
  applyDescriptionTranslationsOnCreate(record, record.getDescription(), record.getInputLanguage());

  FinanceRecord saved = financeRepo.save(record);
  auditLogService.log(user, "CREATE", "FINANCE", saved.getId(), "Added " + saved.getType() + " record for vehicle " + v.getPlateNumber());
  return saved;
 }

 @PostMapping("/misc")
 public FinanceRecord addMiscRecord(@RequestBody FinanceRecord record, HttpServletRequest request){
  AppUser user = currentUserSupport.currentUser(request);
  currentUserSupport.requireRole(user, "OWNER", "MANAGER", "FINANCE");
  record.setVehicle(null);
  record.setOrganization(user.getOrganization());
  record.setRecordScope("MISC");
  if(record.getCategory() == null || record.getCategory().isBlank()){
   record.setCategory("OTHER");
  }
  applyDescriptionTranslationsOnCreate(record, record.getDescription(), record.getInputLanguage());
  FinanceRecord saved = financeRepo.save(record);
  auditLogService.log(user, "CREATE", "FINANCE", saved.getId(), "Added miscellaneous finance record " + saved.getDescription());
  return saved;
 }

 @PutMapping("/{id}")
 public FinanceRecord updateRecord(@PathVariable Long id, @RequestBody FinanceRecord incoming, HttpServletRequest request){
  AppUser user = currentUserSupport.currentUser(request);
  currentUserSupport.requireRole(user, "OWNER", "MANAGER", "FINANCE");
  FinanceRecord existing = financeRepo.findById(id).orElseThrow();
  if(!existing.getOrganization().getId().equals(user.getOrganization().getId())){ throw new RuntimeException("Forbidden"); }
  existing.setType(incoming.getType());
  existing.setRecordScope(incoming.getRecordScope() == null || incoming.getRecordScope().isBlank() ? existing.getRecordScope() : incoming.getRecordScope());
  existing.setCategory(incoming.getCategory());
  existing.setAmount(incoming.getAmount());
  existing.setDate(incoming.getDate());
  if (descriptionChanged(existing, incoming.getDescription(), incoming.getInputLanguage())) {
   applyDescriptionTranslationsOnCreate(existing, incoming.getDescription(), incoming.getInputLanguage());
  }
  if(incoming.getVehicle() != null && incoming.getVehicle().getId() != null){
   existing.setVehicle(vehicleRepo.findById(incoming.getVehicle().getId()).orElseThrow());
   existing.setRecordScope("VEHICLE");
  }
  if("MISC".equalsIgnoreCase(existing.getRecordScope())){
   existing.setVehicle(null);
  }
  FinanceRecord saved = financeRepo.save(existing);
  auditLogService.log(user, "UPDATE", "FINANCE", saved.getId(), "Updated finance record " + saved.getId());
  return saved;
 }

 @DeleteMapping("/{id}")
 public void deleteRecord(@PathVariable Long id, HttpServletRequest request){
  AppUser user = currentUserSupport.currentUser(request);
  currentUserSupport.requireRole(user, "OWNER", "MANAGER", "FINANCE");
  FinanceRecord existing = financeRepo.findById(id).orElseThrow();
  if(existing.getOrganization().getId().equals(user.getOrganization().getId())){
   auditLogService.log(user, "DELETE", "FINANCE", existing.getId(), "Deleted finance record " + existing.getId());
   financeRepo.delete(existing);
  }
 }

 @GetMapping
 public List<FinanceRecord> getAll(
   @RequestParam(required = false) Long vehicleId,
   @RequestParam(required = false) String type,
   @RequestParam(required = false) String scope,
   @RequestParam(required = false) String date,
   @RequestParam(required = false) String start,
   @RequestParam(required = false) String end,
   HttpServletRequest request
 ){
  AppUser user = currentUserSupport.currentUser(request);
  LocalDate parsedDate = (date == null || date.isBlank()) ? null : LocalDate.parse(date);
  LocalDate parsedStart = (start == null || start.isBlank()) ? null : LocalDate.parse(start);
  LocalDate parsedEnd = (end == null || end.isBlank()) ? null : LocalDate.parse(end);

  return financeRepo.findAll().stream()
   .filter(record -> record.getOrganization().getId().equals(user.getOrganization().getId()))
   .filter(record -> vehicleId == null || (record.getVehicle() != null && vehicleId.equals(record.getVehicle().getId())))
   .filter(record -> type == null || type.isBlank() || type.equalsIgnoreCase(record.getType()))
   .filter(record -> scope == null || scope.isBlank() || scope.equalsIgnoreCase(record.getRecordScope()))
   .filter(record -> parsedDate == null || parsedDate.equals(record.getDate()))
   .filter(record -> parsedStart == null || (record.getDate() != null && !record.getDate().isBefore(parsedStart)))
   .filter(record -> parsedEnd == null || (record.getDate() != null && !record.getDate().isAfter(parsedEnd)))
   .toList();
 }

 @GetMapping("/vehicle/{vehicleId}")
 public List<FinanceRecord> getVehicleRecords(@PathVariable Long vehicleId, HttpServletRequest request){
  AppUser user = currentUserSupport.currentUser(request);
  return financeRepo.findByVehicleId(vehicleId).stream()
   .filter(record -> record.getOrganization().getId().equals(user.getOrganization().getId()))
   .toList();
 }

 @GetMapping("/vehicle/{vehicleId}/day/{date}")
 public List<FinanceRecord> vehicleDay(@PathVariable Long vehicleId, @PathVariable String date, HttpServletRequest request){
  AppUser user = currentUserSupport.currentUser(request);
  return financeRepo.findByVehicleIdAndDate(vehicleId, LocalDate.parse(date)).stream()
   .filter(record -> record.getOrganization().getId().equals(user.getOrganization().getId()))
   .toList();
 }

 @GetMapping("/vehicle/{vehicleId}/range")
 public List<FinanceRecord> vehicleRange(
   @PathVariable Long vehicleId,
   @RequestParam String start,
   @RequestParam String end,
   HttpServletRequest request){

  AppUser user = currentUserSupport.currentUser(request);
  return financeRepo.findByVehicleIdAndDateBetween(vehicleId,
   LocalDate.parse(start),
   LocalDate.parse(end)).stream()
   .filter(record -> record.getOrganization().getId().equals(user.getOrganization().getId()))
   .toList();
 }

 @GetMapping("/day/{date}")
 public List<FinanceRecord> allVehiclesDay(@PathVariable String date, HttpServletRequest request){
  AppUser user = currentUserSupport.currentUser(request);
  return financeRepo.findByDate(LocalDate.parse(date)).stream()
   .filter(record -> record.getOrganization().getId().equals(user.getOrganization().getId()))
   .toList();
 }

 @GetMapping("/range")
 public List<FinanceRecord> allVehiclesRange(
   @RequestParam String start,
   @RequestParam String end,
   HttpServletRequest request){

  AppUser user = currentUserSupport.currentUser(request);
  return financeRepo.findByDateBetween(LocalDate.parse(start), LocalDate.parse(end)).stream()
   .filter(record -> record.getOrganization().getId().equals(user.getOrganization().getId()))
   .toList();
 }

 private void applyDescriptionTranslationsOnCreate(FinanceRecord record, String incomingDescription, String inputLanguage) {
  String description = incomingDescription == null ? "" : incomingDescription.trim();
  if (description.isBlank()) {
   record.setDescription("");
   record.setDescriptionEn("");
   record.setDescriptionFr("");
   return;
  }

  String language = normalizeLanguage(inputLanguage);
  if ("fr".equals(language)) {
   record.setDescriptionFr(description);
   record.setDescriptionEn(translationService.translate(description, "FR", "EN").orElse(description));
  } else {
   record.setDescriptionEn(description);
   record.setDescriptionFr(translationService.translate(description, "EN", "FR").orElse(description));
  }

  record.setDescription(description);
 }

 private String normalizeLanguage(String inputLanguage) {
  if (inputLanguage == null || inputLanguage.isBlank()) {
   return "en";
  }
  return inputLanguage.trim().toLowerCase().startsWith("fr") ? "fr" : "en";
 }

 private boolean descriptionChanged(FinanceRecord existing, String incomingDescription, String inputLanguage) {
  String normalizedIncoming = incomingDescription == null ? "" : incomingDescription.trim();
  String language = normalizeLanguage(inputLanguage);
  String currentValue = "fr".equals(language) ? existing.getDescriptionFr() : existing.getDescriptionEn();
  String normalizedCurrent = currentValue == null ? "" : currentValue.trim();
  return !normalizedCurrent.equals(normalizedIncoming);
 }
}
