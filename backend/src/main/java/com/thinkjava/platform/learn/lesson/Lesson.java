package com.thinkjava.platform.learn.lesson;

import com.thinkjava.platform.learn.model.Checkpoint;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "lesson")
public class Lesson {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Checkpoint checkpoint;

  @Column(nullable = false)
  private String title;

  @Column(name = "order_index", nullable = false)
  private int orderIndex;

  @Column(name = "estimated_minutes")
  private Integer estimatedMinutes;

  private Integer difficulty;

  @Column(nullable = false)
  private boolean active = true;


  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public Checkpoint getCheckpoint() {
    return checkpoint;
  }

  public void setCheckpoint(Checkpoint checkpoint) {
    this.checkpoint = checkpoint;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public int getOrderIndex() {
    return orderIndex;
  }

  public void setOrderIndex(int orderIndex) {
    this.orderIndex = orderIndex;
  }

  public Integer getEstimatedMinutes() {
    return estimatedMinutes;
  }

  public void setEstimatedMinutes(Integer estimatedMinutes) {
    this.estimatedMinutes = estimatedMinutes;
  }

  public Integer getDifficulty() {
    return difficulty;
  }

  public void setDifficulty(Integer difficulty) {
    this.difficulty = difficulty;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }
}