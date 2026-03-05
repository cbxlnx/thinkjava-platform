package com.thinkjava.platform.learn.progress;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
  name = "lesson_block_progress",
  uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "block_id"})
)
public class LessonBlockProgress {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "lesson_id", nullable = false)
  private UUID lessonId;

  @Column(name = "block_id", nullable = false)
  private UUID blockId;

  @Column(nullable = false)
  private boolean completed = false;

  @Column(name = "completed_at")
  private Instant completedAt;

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }

  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }

  public UUID getLessonId() { return lessonId; }
  public void setLessonId(UUID lessonId) { this.lessonId = lessonId; }

  public UUID getBlockId() { return blockId; }
  public void setBlockId(UUID blockId) { this.blockId = blockId; }

  public boolean isCompleted() { return completed; }
  public void setCompleted(boolean completed) { this.completed = completed; }

  public Instant getCompletedAt() { return completedAt; }
  public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}