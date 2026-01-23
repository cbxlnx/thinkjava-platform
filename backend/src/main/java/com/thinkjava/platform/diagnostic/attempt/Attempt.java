package com.thinkjava.platform.diagnostic.attempt;

import com.thinkjava.platform.diagnostic.question.Question;
import com.thinkjava.platform.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

//records a single answer attempt a learner makes during the diagnostic 
@Entity
@Table(name = "attempts")
@Getter @Setter @NoArgsConstructor
public class Attempt {

  //auto‑incrementing id for each attempt
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  //many attempts belong to one user
  @ManyToOne(optional = false, fetch = FetchType.LAZY) //user data is only fetched when needed
  @JoinColumn(name = "user_id")
  private User user;

  //same structure as the user field but linking to the question entity(which diagnostic question the user answered)
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "question_id")
  private Question question;

  @Column(name = "selected_option", nullable = false)
  private String selectedOption; // "A"/"B"/"C"/"D"

  @Column(name = "is_correct", nullable = false)
  private boolean isCorrect;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();
}
