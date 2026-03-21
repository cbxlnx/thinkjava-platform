package com.thinkjava.platform.auth;

import com.thinkjava.platform.dto.JwtResponse;
import com.thinkjava.platform.dto.LoginRequest;
import com.thinkjava.platform.dto.RegisterRequest;
import com.thinkjava.platform.user.User;
import com.thinkjava.platform.user.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            User user = userService.create(request.getEmail(), request.getPassword());
            String token = jwtService.generate(user.getEmail());

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(new JwtResponse(token));

        } catch (IllegalArgumentException ex) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            String token = jwtService.generate(request.getEmail());
            return ResponseEntity.ok(new JwtResponse(token));

        } catch (BadCredentialsException ex) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid email or password"));

        } catch (AuthenticationException ex) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authentication failed"));
        }
    }
}