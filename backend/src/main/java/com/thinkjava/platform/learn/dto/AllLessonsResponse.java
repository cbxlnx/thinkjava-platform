package com.thinkjava.platform.learn.dto;

import java.util.List;

public record AllLessonsResponse(
    String userLevel,                // Beginner / Intermediate / Advanced
    List<LessonSummaryResponse> lessons
) {}