package com.example.tms.security;

import com.example.tms.model.AppUser;
import com.example.tms.model.Organization;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Component
public class CurrentUserSupport {

 private static final String PLATFORM_ADMIN_NAME = "Youssouf Diarra";
 private static final String PLATFORM_ADMIN_EMAIL = "dyoussouf12@gmail.com";

 public AppUser currentUser(HttpServletRequest request){
  Object user = request.getAttribute("currentUser");
  if(user instanceof AppUser appUser){
   return appUser;
  }
  throw new ResponseStatusException(UNAUTHORIZED, "Unauthorized");
 }

 public Organization organization(HttpServletRequest request){
  return currentUser(request).getOrganization();
 }

 public void requireRole(AppUser user, String... roles){
  for(String role : roles){
   if(role.equalsIgnoreCase(user.getRole())){
    return;
   }
  }
  throw new ResponseStatusException(FORBIDDEN, "Insufficient permissions");
 }

 public boolean isPlatformAdmin(AppUser user){
  return user != null
   && PLATFORM_ADMIN_NAME.equalsIgnoreCase(user.getFullName())
   && PLATFORM_ADMIN_EMAIL.equalsIgnoreCase(user.getEmail());
 }

 public void requirePlatformAdmin(AppUser user){
  if(isPlatformAdmin(user)){
   return;
  }
  throw new ResponseStatusException(FORBIDDEN, "Platform admin access required");
 }
}
