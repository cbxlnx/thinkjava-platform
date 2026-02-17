package com.thinkjava.platform.learn.dto;

import com.thinkjava.platform.learn.model.Checkpoint;

import java.util.Map;
import java.util.UUID;

public record LearnPathResponse(
    UUID recommendedLessonId,
    Checkpoint startCheckpoint,
    Map<Checkpoint, Double> mastery
) {}