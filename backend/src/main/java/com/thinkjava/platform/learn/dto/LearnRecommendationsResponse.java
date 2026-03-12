package com.thinkjava.platform.learn.dto;

import com.thinkjava.platform.learn.model.Checkpoint;
import java.util.List;
import java.util.UUID;

public record LearnRecommendationsResponse(
    UUID primaryLessonId,
    Checkpoint primaryCheckpoint,
    String reason,
    List<Checkpoint> weakAreas,
    List<LessonSummaryResponse> recommendedLessons
) {}