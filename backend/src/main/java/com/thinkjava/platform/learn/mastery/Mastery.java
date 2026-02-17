package com.thinkjava.platform.learn.mastery;

import com.thinkjava.platform.learn.model.Checkpoint;
import com.thinkjava.platform.user.User;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "mastery",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "checkpoint"})
)
public class Mastery {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Checkpoint checkpoint;

  @Column(name = "mastery_value", nullable = false)
  private double masteryValue;

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