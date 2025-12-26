package com.thinkjava.platform.user;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class UserService {
  private final UserRepository repo; private final PasswordEncoder encoder;
  public UserService(UserRepository repo, PasswordEncoder encoder){
    this.repo = repo; this.encoder = encoder;
  }
  public User create(String email, String rawPassword){
    if (repo.existsByEmail(email)) throw new IllegalArgumentException("Email already used");
    User u = new User(); u.setEmail(email); u.setPassword(encoder.encode(rawPassword));
    return repo.save(u);
  }
  public Optional<User> findByEmail(String email){ return repo.findByEmail(email); }
}
