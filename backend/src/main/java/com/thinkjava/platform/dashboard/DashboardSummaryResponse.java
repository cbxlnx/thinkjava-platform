package com.thinkjava.platform.dashboard;

import com.thinkjava.platform.learn.dto.LessonSummaryResponse;
import java.util.List;
import java.util.Map;

// Main DTO for the dashboard summary response
public record DashboardSummaryResponse(
    int masteryPercent,
    String masteryLabel,
    String topicsCompletedText,
    String quizScoreAvgText,
    int weeklyMinutes,
    int weeklyLessons,
    LessonSummaryResponse currentFocus,
    List<ActivityItemResponse> recentActivity,
    Map<String, Integer> checkpointMastery
) {}