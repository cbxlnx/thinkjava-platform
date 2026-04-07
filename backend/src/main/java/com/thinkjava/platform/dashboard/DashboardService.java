package com.thinkjava.platform.dashboard;

import com.thinkjava.platform.diagnostic.result.DiagnosticResult;
import com.thinkjava.platform.diagnostic.result.DiagnosticResultRepository;
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

// builds the dashboard summary from progress, mastery, and diagnostic data
@Service
public class DashboardService {

        private final MasteryRepository masteryRepository;
        private final LessonProgressRepository progressRepository;
        private final LessonRepository lessonRepository;
        private final LearnService learnService;
        private final DiagnosticResultRepository diagnosticResultRepository;

        public DashboardService(
                        MasteryRepository masteryRepository,
                        LessonProgressRepository progressRepository,
                        LessonRepository lessonRepository,
                        LearnService learnService,
                        DiagnosticResultRepository diagnosticResultRepository) {
                this.masteryRepository = masteryRepository;
                this.progressRepository = progressRepository;
                this.lessonRepository = lessonRepository;
                this.learnService = learnService;
                this.diagnosticResultRepository = diagnosticResultRepository;
        }

        // collects the data needed to render the dashboard
        public DashboardSummaryResponse getSummary(User user) {
                // make sure mastery rows exist before building the response
                learnService.ensureMasteryInitialized(user);

                List<LessonProgress> progressList = progressRepository.findByUser(user);
                var masteryList = masteryRepository.findByUser(user);

                // diagnostic data drives the top-level mastery label and percent
                DiagnosticResult diagnosticResult = diagnosticResultRepository.findByUserId(user.getId())
                                .orElse(null);

                // count fully completed lessons
                long completedCount = progressList.stream()
                                .filter(p -> p.getStatus() == LessonStatus.completed)
                                .count();

                // use the active lesson catalog as the total lesson count
                long totalLessons = lessonRepository.findByActiveTrueOrderByCheckpointAscOrderIndexAsc().size();

                // average the learner's best quiz scores across attempted lessons
                double avgQuiz = progressList.stream()
                                .filter(p -> p.getBestQuizScore() != null)
                                .mapToDouble(LessonProgress::getBestQuizScore)
                                .average()
                                .orElse(0.0);

                // look at the last 7 days for weekly activity stats
                Instant weekAgo = Instant.now().minus(Duration.ofDays(7));
                List<LessonProgress> weeklyProgress = progressList.stream()
                                .filter(p -> p.getLastSeenAt() != null && p.getLastSeenAt().isAfter(weekAgo))
                                .toList();

                // sum lesson time estimates for recent activity
                int weeklyMinutes = weeklyProgress.stream()
                                .mapToInt(p -> p.getLesson().getEstimatedMinutes() == null
                                                ? 0
                                                : p.getLesson().getEstimatedMinutes())
                                .sum();

                // count recent lesson touches
                int weeklyLessons = weeklyProgress.size();

                // reuse learn service logic to identify the next recommended focus
                LessonSummaryResponse currentFocus = learnService.getCurrentFocus(user);

                // build a short recent activity feed from the latest lesson interactions
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

                // expose mastery values as simple percentages per checkpoint
                Map<String, Integer> checkpointMastery = masteryList.stream()
                                .collect(Collectors.toMap(
                                                m -> m.getCheckpoint().name(),
                                                m -> (int) Math.round(m.getMasteryValue() * 100.0)));

                // prefer the saved diagnostic percent for the headline score
                int masteryPercent = diagnosticResult != null && diagnosticResult.getDiagnosticPercent() != null
                                ? diagnosticResult.getDiagnosticPercent()
                                : 0;

                // translate checkpoint outcomes into a learner level label
                String masteryLabel = resolveDiagnosticLevel(diagnosticResult);

                return new DashboardSummaryResponse(
                                masteryPercent,
                                masteryLabel,
                                completedCount + "/" + totalLessons,
                                ((int) Math.round(avgQuiz * 100)) + "%",
                                weeklyMinutes,
                                weeklyLessons,
                                currentFocus,
                                recentActivity,
                                checkpointMastery);
        }

        private String resolveDiagnosticLevel(DiagnosticResult diagnosticResult) {
                if (diagnosticResult == null) {
                        return "Unknown";
                }

                // count how many checkpoints were clearly strong
                long strongCount = List.of(
                                diagnosticResult.getFundamentals(),
                                diagnosticResult.getLoops(),
                                diagnosticResult.getArrays(),
                                diagnosticResult.getMethods(),
                                diagnosticResult.getOop()).stream().filter("Strong"::equals).count();

                // count weak and unknown checkpoints together for beginner placement
                long weakOrUnknownCount = List.of(
                                diagnosticResult.getFundamentals(),
                                diagnosticResult.getLoops(),
                                diagnosticResult.getArrays(),
                                diagnosticResult.getMethods(),
                                diagnosticResult.getOop()).stream().filter(v -> "Weak".equals(v) || "Unknown".equals(v))
                                .count();

                // mostly strong checkpoints maps to advanced
                if (strongCount >= 4) {
                        return "Advanced";
                }

                // several weak or unknown checkpoints maps to beginner
                if (weakOrUnknownCount >= 3) {
                        return "Beginner";
                }

                // everything else lands in the middle
                return "Intermediate";
        }
}
