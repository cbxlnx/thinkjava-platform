package com.thinkjava.platform.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/secure")
public class SecureController {

  @GetMapping("/me")
  public ResponseEntity<?> me(@AuthenticationPrincipal UserDetails user) {
    if (user == null) {
      return ResponseEntity.status(401).body("Unauthenticated");
    }
    return ResponseEntity.ok("You are " + user.getUsername());
  }
}
