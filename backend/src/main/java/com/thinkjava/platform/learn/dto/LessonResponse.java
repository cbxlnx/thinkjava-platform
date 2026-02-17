package com.thinkjava.platform.learn.dto;

import com.thinkjava.platform.learn.model.Checkpoint;

import java.util.List;
import java.util.UUID;

/**
 * API response returned by:
 * GET /api/learn/lesson/{id}
 *
 * Contains:
 * - lesson metadata
 * - ordered markdown sections
 * - end-of-lesson quiz questions (without answers)
 */
public record LessonResponse(
    LessonMeta lesson,
    List<SectionDto> sections,
    QuizDto quiz
) {

  /**
   * Basic lesson info (used for header, progress, routing)
   */
  public record LessonMeta(
      UUID id,
      Checkpoint checkpoint,
      String title,
      int orderIndex,
      Integer estimatedMinutes
  ) {}

  /**
   * One markdown block of lesson content
   */
  public record SectionDto(
      int order,
      String markdown
  ) {}

  /**
   * End-of-lesson quiz wrapper
   */
  public record QuizDto(
      List<QuizQuestionDto> questions
  ) {}

  /**
   * Quiz question sent to frontend
   * (NO correct answer here — checked on submit)
   */
  public record QuizQuestionDto(
      UUID id,
      String prompt,
      List<String> options
  ) {}
}