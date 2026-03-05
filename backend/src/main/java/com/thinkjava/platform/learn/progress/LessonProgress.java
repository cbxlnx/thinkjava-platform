package com.thinkjava.platform.learn.progress;

import com.thinkjava.platform.learn.lesson.Lesson;
import com.thinkjava.platform.learn.model.LessonStatus;
import com.thinkjava.platform.user.User;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "lesson_progress",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "lesson_id"})
)
public class LessonProgress {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "lesson_id", nullable = false)
  private Lesson lesson;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private LessonStatus status = LessonStatus.in_progress;

  @Column(name = "best_quiz_score")
  private Double bestQuizScore;

  @Column(name = "last_seen_at")
  private Instant lastSeenAt;

  @Column(nullable = false)
  private int percent = 0;

 
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

  public Lesson getLesson() {
    return lesson;
  }

  public void setLesson(Lesson lesson) {
    this.lesson = lesson;
  }

  public LessonStatus getStatus() {
    return status;
  }

  public void setStatus(LessonStatus status) {
    this.status = status;
  }

  public Double getBestQuizScore() {
    return bestQuizScore;
  }

  public void setBestQuizScore(Double bestQuizScore) {
    this.bestQuizScore = bestQuizScore;
  }

  public Instant getLastSeenAt() {
    return lastSeenAt;
  }

  public void setLastSeenAt(Instant lastSeenAt) {
    this.lastSeenAt = lastSeenAt;
  }
}