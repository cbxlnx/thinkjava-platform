package com.thinkjava.platform.learn.dto;

import java.util.UUID;

/**
 * API response returned by:
 * POST /api/learn/lesson/{id}/quiz/submit
 *
 * Used to:
 * - show quiz result to user
 * - update mastery ring
 * - decide what lesson to open next
 */
public record LessonQuizSubmitResponse(

    /**
     * Final quiz score (0.0 – 1.0)
     * Example: 0.8 means 80%
     */
    double score,

    /**
     * Whether the lesson is considered completed
     * (score >= pass threshold)
     */
    boolean passed,

    /**
     * Updated mastery value for the lesson checkpoint (0.0 – 1.0)
     */
    double updatedCheckpointMastery,

    /**
     * Next lesson the user should be routed to
     * (can be same lesson if remediation / retake)
     */
    UUID recommendedNextLessonId

) {}