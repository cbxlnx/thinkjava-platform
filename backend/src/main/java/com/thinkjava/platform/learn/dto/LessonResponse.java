package com.thinkjava.platform.learn.dto;

import com.thinkjava.platform.learn.model.Checkpoint;
import com.thinkjava.platform.learn.model.LessonBlockType;

import java.util.List;
import java.util.UUID;

/**
 * API response returned by:
 * GET /api/learn/lesson/{id}
 *
 * Contains:
 * - lesson metadata
 * - ordered typed blocks (markdown / video / inline quiz)
 * - end-of-lesson quiz questions
 */
public record LessonResponse(
        LessonMeta lesson,
        List<BlockDto> blocks,
        QuizDto quiz) {

    public record LessonMeta(
            UUID id,
            Checkpoint checkpoint,
            String title,
            int orderIndex,
            Integer estimatedMinutes) {
    }

    /**
     * Unified lesson block
     */
    public record BlockDto(
            int order,
            LessonBlockType type,
            String markdown,
            String videoTitle,
            String videoUrl,
            String payload) {
    }

    public record QuizDto(
            List<QuizQuestionDto> questions) {
    }

    public record QuizQuestionDto(
            UUID id,
            String prompt,
            List<String> options) {
    }
}