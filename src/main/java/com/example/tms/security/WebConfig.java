package com.example.tms.security;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

 private final AuthInterceptor authInterceptor;
 private final String[] allowedOrigins;

 public WebConfig(
  AuthInterceptor authInterceptor,
  @Value("${app.cors.allowed-origins:http://localhost:8100,http://localhost:4200}") String allowedOrigins
 ){
  this.authInterceptor = authInterceptor;
  this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
   .map(String::trim)
   .filter(origin -> !origin.isBlank())
   .toArray(String[]::new);
 }

 @Override
 public void addInterceptors(InterceptorRegistry registry) {
  registry.addInterceptor(authInterceptor);
 }

 @Override
 public void addCorsMappings(CorsRegistry registry) {
  registry.addMapping("/**")
   .allowedOrigins(allowedOrigins)
   .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
   .allowedHeaders("*")
   .allowCredentials(false);
 }
}
