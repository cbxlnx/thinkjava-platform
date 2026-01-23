package com.thinkjava.platform.diagnostic.session;

import com.thinkjava.platform.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "diagnostic_sessions")
@Getter @Setter @NoArgsConstructor
public class DiagnosticSession {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user;

  @Column(nullable = false)
  private String stage; // "A".."E"

  @Column(name = "q_in_stage", nullable = false)
  private int qInStage = 0; // 0..2

  @Column(name = "fundamentals_correct", nullable = false)
  private int fundamentalsCorrect = 0;

  @Column(name = "loops_correct", nullable = false)
  private int loopsCorrect = 0;

  @Column(name = "arrays_correct", nullable = false)
  private int arraysCorrect = 0;

  @Column(name = "methods_correct", nullable = false)
  private int methodsCorrect = 0;

  @Column(name = "oop_correct", nullable = false)
  private int oopCorrect = 0;

  @Column(name = "is_finished", nullable = false)
  private boolean finished = false;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  @PreUpdate
  public void onUpdate() {
    this.updatedAt = Instant.now();
  }
}
