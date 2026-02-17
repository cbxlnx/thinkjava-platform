package com.thinkjava.platform.learn.dto;

import com.thinkjava.platform.learn.model.Checkpoint;

import java.util.UUID;

public record LessonSummaryResponse(
    UUID id,
    String title,
    Checkpoint checkpoint,
    int orderIndex,
    Integer estimatedMinutes,
    int difficulty,          // 1..3
    String levelTag,         // Beginner/Intermediate/Advanced
    String status,           // completed/in_progress/not_started
    int masteryPercent,      // 0..100 (checkpoint mastery)
    boolean locked
) {}