package com.example.tms.security;

import com.example.tms.model.AppUser;
import com.example.tms.repository.AppUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

 private final AppUserRepository userRepository;

 public AuthInterceptor(AppUserRepository userRepository){
  this.userRepository = userRepository;
 }

 @Override
 public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
  String path = request.getRequestURI();
  if("OPTIONS".equalsIgnoreCase(request.getMethod())){
   response.setStatus(HttpServletResponse.SC_OK);
   return true;
  }
  if(path.startsWith("/auth") || path.startsWith("/h2-console")){
   return true;
  }

  String authHeader = request.getHeader("Authorization");
  if(authHeader == null || !authHeader.startsWith("Bearer ")){
   response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing auth token");
   return false;
  }

  String token = authHeader.substring(7);
  AppUser user = userRepository.findByAuthToken(token).orElse(null);
  if(user == null){
   response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid auth token");
   return false;
  }

  if(!user.isActive()){
   user.setAuthToken(null);
   userRepository.save(user);
   response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User access has been revoked");
   return false;
  }

  if(!user.getOrganization().isApproved()){
   user.setAuthToken(null);
   userRepository.save(user);
   response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Organization access has been revoked");
   return false;
  }

  request.setAttribute("currentUser", user);
  return true;
 }
}
