package com.thinkjava.platform.learn.section;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "inline_quiz_question")
public class InlineQuizQuestion {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "block_id", nullable = false)
  private LessonBlock block;

  @Column(columnDefinition = "text", nullable = false)
  private String prompt;

  // store as JSON string for simplicity (["A","B","C","D"])
  @Column(columnDefinition = "text", nullable = false)
  private String optionsJson;

  @Column(nullable = false)
  private String correctOption;

  @Column(columnDefinition = "text")
  private String explanation;

  public UUID getId() { return id; }

  public LessonBlock getBlock() { return block; }
  public void setBlock(LessonBlock block) { this.block = block; }

  public String getPrompt() { return prompt; }
  public void setPrompt(String prompt) { this.prompt = prompt; }

  public String getOptionsJson() { return optionsJson; }
  public void setOptionsJson(String optionsJson) { this.optionsJson = optionsJson; }

  public String getCorrectOption() { return correctOption; }
  public void setCorrectOption(String correctOption) { this.correctOption = correctOption; }

  public String getExplanation() { return explanation; }
  public void setExplanation(String explanation) { this.explanation = explanation; }
}