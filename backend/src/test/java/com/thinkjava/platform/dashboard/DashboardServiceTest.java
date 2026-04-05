package com.thinkjava.platform.dashboard;

import com.thinkjava.platform.diagnostic.result.DiagnosticResult;
import com.thinkjava.platform.diagnostic.result.DiagnosticResultRepository;
import com.thinkjava.platform.learn.LearnService;
import com.thinkjava.platform.learn.dto.LessonSummaryResponse;
import com.thinkjava.platform.learn.lesson.Lesson;
import com.thinkjava.platform.learn.lesson.LessonRepository;
import com.thinkjava.platform.learn.mastery.Mastery;
import com.thinkjava.platform.learn.mastery.MasteryRepository;
import com.thinkjava.platform.learn.model.Checkpoint;
import com.thinkjava.platform.learn.model.LessonStatus;
import com.thinkjava.platform.learn.progress.LessonProgress;
import com.thinkjava.platform.learn.progress.LessonProgressRepository;
import com.thinkjava.platform.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private MasteryRepository masteryRepository;
    @Mock
    private LessonProgressRepository progressRepository;
    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private LearnService learnService;
    @Mock
    private DiagnosticResultRepository diagnosticResultRepository;

    private DashboardService dashboardService;
    private User user;
    // setup method to initialize the DashboardService and a test user before each test case
    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(
                masteryRepository,
                progressRepository,
                lessonRepository,
                learnService,
                diagnosticResultRepository);

        user = new User();
        user.setId(1L);
        user.setEmail("student@example.com");
    }
    // test case to verify that the getSummary method correctly builds the dashboard 
    // metrics based on the user's lesson progress, mastery levels, and diagnostic results
    @Test
    void getSummary_buildsDashboardMetricsFromProgressMasteryAndDiagnostic() {
        Lesson arraysLesson = lesson("Arrays", 20);
        Lesson loopsLesson = lesson("Loops", 15);

        LessonProgress completed = progress(arraysLesson, LessonStatus.completed, 0.8, Instant.now().minusSeconds(60));
        LessonProgress inProgress = progress(loopsLesson, LessonStatus.in_progress, 0.6, Instant.now().minusSeconds(120));

        LessonSummaryResponse currentFocus = new LessonSummaryResponse(
                arraysLesson.getId(),
                "Arrays",
                Checkpoint.arrays,
                3,
                20,
                2,
                "Intermediate",
                "in_progress",
                50,
                false);

        when(progressRepository.findByUser(user)).thenReturn(List.of(completed, inProgress));
        when(masteryRepository.findByUser(user)).thenReturn(List.of(
                mastery(Checkpoint.fundamentals, 0.75),
                mastery(Checkpoint.loops, 0.50),
                mastery(Checkpoint.arrays, 0.80)));
        when(lessonRepository.findByActiveTrueOrderByCheckpointAscOrderIndexAsc())
                .thenReturn(List.of(arraysLesson, loopsLesson, lesson("Methods", 10)));
        when(learnService.getCurrentFocus(user)).thenReturn(currentFocus);
        when(diagnosticResultRepository.findByUserId(user.getId()))
                .thenReturn(Optional.of(diagnosticResult(72, "Strong", "Medium", "Weak", "Strong", "Strong")));

        DashboardSummaryResponse response = dashboardService.getSummary(user);
        // verify that the learnService.ensureMasteryInitialized method was called to initialize mastery data for the user
        verify(learnService).ensureMasteryInitialized(user);
        assertEquals(72, response.masteryPercent());
        assertEquals("Intermediate", response.masteryLabel());
        assertEquals("1/3", response.topicsCompletedText());
        assertEquals("70%", response.quizScoreAvgText());
        assertEquals(35, response.weeklyMinutes());
        assertEquals(2, response.weeklyLessons());
        assertEquals("Arrays", response.currentFocus().title());
        assertEquals(2, response.recentActivity().size());
        assertEquals(75, response.checkpointMastery().get("fundamentals"));
        assertEquals(50, response.checkpointMastery().get("loops"));
        assertEquals(80, response.checkpointMastery().get("arrays"));
    }
    // test case to verify that if the diagnostic result is missing for the user, 
    // the dashboard summary correctly defaults to "Unknown" label and 0% mastery
    @Test
    void getSummary_returnsUnknownLabelWhenDiagnosticIsMissing() {
        when(progressRepository.findByUser(user)).thenReturn(List.of());
        when(masteryRepository.findByUser(user)).thenReturn(List.of());
        when(lessonRepository.findByActiveTrueOrderByCheckpointAscOrderIndexAsc()).thenReturn(List.of());
        when(learnService.getCurrentFocus(user)).thenReturn(null);
        when(diagnosticResultRepository.findByUserId(user.getId())).thenReturn(Optional.empty());

        DashboardSummaryResponse response = dashboardService.getSummary(user);

        assertEquals(0, response.masteryPercent());
        assertEquals("Unknown", response.masteryLabel());
        assertEquals("0/0", response.topicsCompletedText());
        assertEquals("0%", response.quizScoreAvgText());
        assertEquals(0, response.weeklyMinutes());
        assertEquals(0, response.weeklyLessons());
        assertFalse(response.checkpointMastery().containsKey("fundamentals"));
    }
    // helper methods to create test data for lessons, progress, mastery, and diagnostic results
    private Lesson lesson(String title, Integer minutes) {
        Lesson lesson = new Lesson();
        lesson.setId(java.util.UUID.randomUUID());
        lesson.setTitle(title);
        lesson.setEstimatedMinutes(minutes);
        return lesson;
    }

    private LessonProgress progress(Lesson lesson, LessonStatus status, Double score, Instant lastSeenAt) {
        LessonProgress progress = new LessonProgress();
        progress.setLesson(lesson);
        progress.setStatus(status);
        progress.setBestQuizScore(score);
        progress.setLastSeenAt(lastSeenAt);
        return progress;
    }

    private Mastery mastery(Checkpoint checkpoint, double value) {
        Mastery mastery = new Mastery();
        mastery.setCheckpoint(checkpoint);
        mastery.setMasteryValue(value);
        return mastery;
    }

    private DiagnosticResult diagnosticResult(
            Integer percent,
            String fundamentals,
            String loops,
            String arrays,
            String methods,
            String oop) {
        DiagnosticResult result = new DiagnosticResult();
        result.setDiagnosticPercent(percent);
        result.setFundamentals(fundamentals);
        result.setLoops(loops);
        result.setArrays(arrays);
        result.setMethods(methods);
        result.setOop(oop);
        return result;
    }
}
