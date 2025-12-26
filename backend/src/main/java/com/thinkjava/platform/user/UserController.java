package com.thinkjava.platform.user;

import com.thinkjava.platform.user.dto.UserMeResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @GetMapping("/me")
  public ResponseEntity<UserMeResponse> me(Authentication authentication) {
    String email = authentication.getName(); // same as User.getUsername() email

    User user = userService.findByEmail(email)
        .orElseThrow(() -> new RuntimeException("User not found"));

    return ResponseEntity.ok(new UserMeResponse(user.getId(), user.getEmail()));
  }
}