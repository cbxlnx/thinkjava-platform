package com.thinkjava.platform.learn.quiz;

import com.thinkjava.platform.learn.lesson.Lesson;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "lesson_quiz_question")
public class LessonQuizQuestion {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "lesson_id", nullable = false)
  private Lesson lesson;

  @Column(nullable = false)
  private String type; // "MCQ" for MVP

  @Column(nullable = false, columnDefinition = "text")
  private String prompt;

  @Column(name = "options_json", nullable = false, columnDefinition = "text")
  private String optionsJson;

  @Column(name = "correct_json", nullable = false, columnDefinition = "text")
  private String correctJson;

  @Column(columnDefinition = "text")
  private String explanation;

  private Integer difficulty;

  // ---- getters/setters ----

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public Lesson getLesson() {
    return lesson;
  }

  public void setLesson(Lesson lesson) {
    this.lesson = lesson;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getPrompt() {
    return prompt;
  }

  public void setPrompt(String prompt) {
    this.prompt = prompt;
  }

  public String getOptionsJson() {
    return optionsJson;
  }

  public void setOptionsJson(String optionsJson) {
    this.optionsJson = optionsJson;
  }

  public String getCorrectJson() {
    return correctJson;
  }

  public void setCorrectJson(String correctJson) {
    this.correctJson = correctJson;
  }

  public String getExplanation() {
    return explanation;
  }

  public void setExplanation(String explanation) {
    this.explanation = explanation;
  }

  public Integer getDifficulty() {
    return difficulty;
  }

  public void setDifficulty(Integer difficulty) {
    this.difficulty = difficulty;
  }
}