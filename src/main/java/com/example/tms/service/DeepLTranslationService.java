package com.example.tms.service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class DeepLTranslationService {

 private final HttpClient httpClient = HttpClient.newHttpClient();
 private final ObjectMapper objectMapper = new ObjectMapper();
 private final String apiKey;
 private final String apiBaseUrl;

 public DeepLTranslationService(
  @Value("${app.translation.deepl.api-key:}") String apiKey,
  @Value("${app.translation.deepl.base-url:https://api-free.deepl.com}") String apiBaseUrl
 ) {
  this.apiKey = apiKey == null ? "" : apiKey.trim();
  this.apiBaseUrl = apiBaseUrl == null ? "https://api-free.deepl.com" : apiBaseUrl.trim();
 }

 public Optional<String> translate(String text, String sourceLanguage, String targetLanguage) {
  if (text == null || text.isBlank()) {
   return Optional.empty();
  }

  if (sourceLanguage == null || targetLanguage == null || sourceLanguage.equalsIgnoreCase(targetLanguage)) {
   return Optional.of(text.trim());
  }

  if (apiKey.isBlank()) {
   return Optional.empty();
  }

  try {
   String requestBody =
    "text=" + encode(text.trim()) +
    "&source_lang=" + encode(sourceLanguage.toUpperCase()) +
    "&target_lang=" + encode(targetLanguage.toUpperCase());

   HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create(apiBaseUrl + "/v2/translate"))
    .header("Authorization", "DeepL-Auth-Key " + apiKey)
    .header("Content-Type", "application/x-www-form-urlencoded")
    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
    .build();

   HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
   if (response.statusCode() < 200 || response.statusCode() >= 300) {
    return Optional.empty();
   }

   JsonNode root = objectMapper.readTree(response.body());
   JsonNode translations = root.path("translations");
   if (!translations.isArray() || translations.isEmpty()) {
    return Optional.empty();
   }

   String translatedText = translations.get(0).path("text").asText("").trim();
   return translatedText.isBlank() ? Optional.empty() : Optional.of(translatedText);
  } catch (InterruptedException exception) {
   Thread.currentThread().interrupt();
   return Optional.empty();
  } catch (IOException exception) {
   return Optional.empty();
  } catch (Exception exception) {
   return Optional.empty();
  }
 }

 private String encode(String value) {
  return URLEncoder.encode(value, StandardCharsets.UTF_8);
 }
}
