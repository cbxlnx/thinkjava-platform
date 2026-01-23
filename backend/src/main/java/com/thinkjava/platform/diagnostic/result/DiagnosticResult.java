package com.thinkjava.platform.diagnostic.result;

import com.thinkjava.platform.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "diagnostic_results")
@Getter @Setter @NoArgsConstructor
public class DiagnosticResult {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // one result per user
  @OneToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", unique = true)
  private User user;

  @Column(nullable = false)
  private String fundamentals; // Strong/Medium/Weak/Unknown

  @Column(nullable = false)
  private String loops;

  @Column(nullable = false)
  private String arrays;

  @Column(nullable = false)
  private String methods;

  @Column(nullable = false)
  private String oop;

  @Column(name = "start_module", nullable = false)
  private String startModule; // fundamentals|loops|arrays|methods|oop

  @Column(name = "completed_at", nullable = false)
  private Instant completedAt = Instant.now();
}
