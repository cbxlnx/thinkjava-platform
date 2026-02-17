package com.thinkjava.platform.user;

import com.thinkjava.platform.user.dto.UpdateNameRequest;
import jakarta.validation.Valid;
import com.thinkjava.platform.user.dto.UserMeResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @GetMapping("/me")
  public ResponseEntity<UserMeResponse> me(Authentication authentication) {
    String email = authentication.getName();

    User user = userService.findByEmail(email)
        .orElseThrow(() -> new RuntimeException("User not found"));

    return ResponseEntity.ok(new UserMeResponse(user.getEmail(), user.getFirstName()));
  }

  @PatchMapping("/me/name")
  public ResponseEntity<UserMeResponse> updateName(
      Authentication authentication,
      @Valid @RequestBody UpdateNameRequest body) {
    String email = authentication.getName();

    User user = userService.findByEmail(email)
        .orElseThrow(() -> new RuntimeException("User not found"));

    String name = body.firstName() == null ? "" : body.firstName().trim();
    if (name.isEmpty())
      throw new IllegalArgumentException("firstName cannot be blank");

    user.setFirstName(name);
    userService.save(user);

    return ResponseEntity.ok(new UserMeResponse(user.getEmail(), user.getFirstName()));
  }

}