package com.thinkjava.platform.user;

import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter; import lombok.NoArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.GrantedAuthority;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Entity @Table(name="users")
@Getter @Setter @NoArgsConstructor
public class User implements UserDetails {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(unique = true, nullable = false)
  private String email;

  @Column(nullable = false)
  private String password;

  @Column(nullable = false)
  private Instant createdAt = Instant.now();

  // UserDetails
  @Override public Collection<? extends GrantedAuthority> getAuthorities() { return List.of(); }
  @Override public String getUsername() { return email; }
  @Override public boolean isAccountNonExpired() { return true; }
  @Override public boolean isAccountNonLocked() { return true; }
  @Override public boolean isCredentialsNonExpired() { return true; }
  @Override public boolean isEnabled() { return true; }
}
