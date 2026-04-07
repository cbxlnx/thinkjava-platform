package com.thinkjava.platform.learn.mastery;

import com.thinkjava.platform.learn.model.Checkpoint;
import com.thinkjava.platform.user.User;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    // stores one mastery row per user and checkpoint
    name = "mastery",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "checkpoint"})
)
public class Mastery {

  // primary key for the mastery row
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  // learner that owns this checkpoint mastery
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  // learning checkpoint this mastery value belongs to
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Checkpoint checkpoint;

  // normalized mastery score for the checkpoint
  @Column(name = "mastery_value", nullable = false)
  private double masteryValue;

  // last time this mastery value was recalculated
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
 
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public Checkpoint getCheckpoint() {
    return checkpoint;
  }

  public void setCheckpoint(Checkpoint checkpoint) {
    this.checkpoint = checkpoint;
  }

  public double getMasteryValue() {
    return masteryValue;
  }

  public void setMasteryValue(double masteryValue) {
    this.masteryValue = masteryValue;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
