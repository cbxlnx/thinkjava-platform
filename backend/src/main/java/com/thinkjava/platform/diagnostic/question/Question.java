package com.thinkjava.platform.diagnostic.question;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "questions")
@Getter @Setter @NoArgsConstructor
public class Question {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String checkpoint; // A,B,C,D,E

  @Column(nullable = false)
  private String topic; // fundamentals, loops, arrays, methods, oop

  private String subskill;

  @Column(nullable = false)
  private Integer difficulty = 1;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String prompt;

  @Column(name = "option_a", nullable = false, columnDefinition = "TEXT")
  private String optionA;

  @Column(name = "option_b", nullable = false, columnDefinition = "TEXT")
  private String optionB;

  @Column(name = "option_c", nullable = false, columnDefinition = "TEXT")
  private String optionC;

  @Column(name = "option_d", nullable = false, columnDefinition = "TEXT")
  private String optionD;

  @Column(name = "correct_option", nullable = false)
  private String correctOption; // "A"/"B"/"C"/"D"

  @Column(columnDefinition = "TEXT")
  private String explanation;
}
