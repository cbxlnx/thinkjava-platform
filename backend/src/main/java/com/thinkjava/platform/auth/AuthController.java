package com.thinkjava.platform.auth;

import com.thinkjava.platform.dto.JwtResponse;
import com.thinkjava.platform.dto.LoginRequest;
import com.thinkjava.platform.dto.RegisterRequest;
import com.thinkjava.platform.user.User;
import com.thinkjava.platform.user.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {

  private final UserService userService;
  private final JwtService jwtService;
  private final AuthenticationManager authManager;

  public AuthController(UserService userService, JwtService jwtService, AuthenticationManager authManager) {
    this.userService = userService;
    this.jwtService = jwtService;
    this.authManager = authManager;
  }

  @PostMapping("/register")
  public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
    User user = userService.create(request.getEmail(), request.getPassword());
    String token = jwtService.generate(user.getEmail());
    return ResponseEntity.ok(new JwtResponse(token));
  }

  @PostMapping("/login")
  public ResponseEntity<?> login(@RequestBody LoginRequest request) {
    try {
      authManager.authenticate(
          new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
      );
      String token = jwtService.generate(request.getEmail());
      return ResponseEntity.ok(new JwtResponse(token));
    } catch (AuthenticationException ex) {
      return ResponseEntity.status(401).body("Invalid credentials");
    }
  }
}
