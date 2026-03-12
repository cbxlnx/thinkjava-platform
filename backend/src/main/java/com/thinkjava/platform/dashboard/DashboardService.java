package com.thinkjava.platform.dashboard;

import com.thinkjava.platform.learn.LearnService;
import com.thinkjava.platform.learn.dto.LessonSummaryResponse;
import com.thinkjava.platform.learn.lesson.LessonRepository;
import com.thinkjava.platform.learn.mastery.MasteryRepository;
import com.thinkjava.platform.learn.model.LessonStatus;
import com.thinkjava.platform.learn.progress.LessonProgress;
import com.thinkjava.platform.learn.progress.LessonProgressRepository;
import com.thinkjava.platform.user.User;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Service class for handling dashboard-related operations
@Service
public class DashboardService {

    private final MasteryRepository masteryRepository;
    private final LessonProgressRepository progressRepository;
    private final LessonRepository lessonRepository;
    private final LearnService learnService;

    public DashboardService(
            MasteryRepository masteryRepository,
            LessonProgressRepository progressRepository,
            LessonRepository lessonRepository,
            LearnService learnService
    ) {
        this.masteryRepository = masteryRepository;
        this.progressRepository = progressRepository;
        this.lessonRepository = lessonRepository;
        this.learnService = learnService;
    }
    // Main method to get the dashboard summary for a user
    public DashboardSummaryResponse getSummary(User user) {
        double avgMastery = masteryRepository.findByUser(user).stream()
                .mapToDouble(m -> m.getMasteryValue())
                .average()
                .orElse(0.0);

        int masteryPercent = (int) Math.round(avgMastery * 100.0);

        String masteryLabel =
                avgMastery >= 0.75 ? "Advanced" :
                avgMastery >= 0.45 ? "Intermediate" : "Beginner";
        
        List<LessonProgress> progressList = progressRepository.findByUser(user);

        long completedCount = progressList.stream()
                .filter(p -> p.getStatus() == LessonStatus.completed)
                .count();

        long totalLessons = lessonRepository.findByActiveTrueOrderByCheckpointAscOrderIndexAsc().size();

        double avgQuiz = progressList.stream()
                .filter(p -> p.getBestQuizScore() != null)
                .mapToDouble(LessonProgress::getBestQuizScore)
                .average()
                .orElse(0.0);

        Instant weekAgo = Instant.now().minus(Duration.ofDays(7));
        List<LessonProgress> weeklyProgress = progressList.stream()
                .filter(p -> p.getLastSeenAt() != null && p.getLastSeenAt().isAfter(weekAgo))
                .toList();

        int weeklyMinutes = weeklyProgress.stream()
                .mapToInt(p -> p.getLesson().getEstimatedMinutes() == null ? 0 : p.getLesson().getEstimatedMinutes())
                .sum();

        int weeklyLessons = weeklyProgress.size();

        LessonSummaryResponse currentFocus = learnService.getCurrentFocus(user);
        // Get recent activity (last 5 lessons with a lastSeenAt timestamp, sorted by most recent)
        List<ActivityItemResponse> recentActivity = progressList.stream()
                .filter(p -> p.getLastSeenAt() != null)
                .sorted(Comparator.comparing(LessonProgress::getLastSeenAt).reversed())
                .limit(5)
                .map(p -> {
                    String title = switch (p.getStatus()) {
                        case completed -> "Completed: " + p.getLesson().getTitle();
                        case in_progress -> "Started: " + p.getLesson().getTitle();
                        default -> "Viewed: " + p.getLesson().getTitle();
                    };

                    String pill = p.getBestQuizScore() == null
                            ? ""
                            : ((int) Math.round(p.getBestQuizScore() * 100)) + "%";

                    return new ActivityItemResponse(title, "Recently", pill);
                })
                .toList();
        // Get checkpoint mastery as a map of checkpoint name to mastery percentage
        Map<String, Integer> checkpointMastery = masteryRepository.findByUser(user).stream()
        .collect(Collectors.toMap(
                m -> m.getCheckpoint().name(),
                m -> (int) Math.round(m.getMasteryValue() * 100.0)
        ));

            return new DashboardSummaryResponse(
                    masteryPercent,
                    masteryLabel,
                    completedCount + "/" + totalLessons,
                    ((int) Math.round(avgQuiz * 100)) + "%",
                    weeklyMinutes,
                    weeklyLessons,
                    currentFocus,
                    recentActivity,
                    checkpointMastery
            );
    }
}