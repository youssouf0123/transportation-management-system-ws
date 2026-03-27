package com.example.tms.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

 private final AuthInterceptor authInterceptor;

 public WebConfig(AuthInterceptor authInterceptor){
  this.authInterceptor = authInterceptor;
 }

 @Override
 public void addInterceptors(InterceptorRegistry registry) {
  registry.addInterceptor(authInterceptor);
 }

 @Override
 public void addCorsMappings(CorsRegistry registry) {
  registry.addMapping("/**")
   .allowedOrigins("http://localhost:8100", "http://localhost:4200")
   .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
   .allowedHeaders("*")
   .allowCredentials(false);
 }
}
