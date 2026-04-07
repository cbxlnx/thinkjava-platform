package com.thinkjava.platform.user;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
  // method to find a user by their email, 
  // returning an Optional that may be empty if no user is found
  Optional<User> findByEmail(String email);
  boolean existsByEmail(String email);
}
