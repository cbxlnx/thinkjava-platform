package com.thinkjava.platform.learn;

import com.thinkjava.platform.diagnostic.result.DiagnosticResult;
import com.thinkjava.platform.diagnostic.result.DiagnosticResultRepository;
import com.thinkjava.platform.learn.dto.AllLessonsResponse;
import com.thinkjava.platform.learn.dto.LearnPathResponse;
import com.thinkjava.platform.learn.dto.LessonQuizSubmitRequest;
import com.thinkjava.platform.learn.dto.LessonQuizSubmitResponse;
import com.thinkjava.platform.learn.lesson.Lesson;
import com.thinkjava.platform.learn.lesson.LessonRepository;
import com.thinkjava.platform.learn.mastery.Mastery;
import com.thinkjava.platform.learn.mastery.MasteryRepository;
import com.thinkjava.platform.learn.model.Checkpoint;
import com.thinkjava.platform.learn.model.LessonStatus;
import com.thinkjava.platform.learn.progress.LessonProgress;
import com.thinkjava.platform.learn.progress.LessonProgressRepository;
import com.thinkjava.platform.learn.quiz.LessonQuizQuestion;
import com.thinkjava.platform.learn.quiz.LessonQuizQuestionRepository;
import com.thinkjava.platform.learn.section.LessonBlockRepository;
import com.thinkjava.platform.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LearnServiceTest {

        @Mock
        private LessonRepository lessonRepository;
        @Mock
        private LessonBlockRepository blockRepository;
        @Mock
        private LessonQuizQuestionRepository quizRepository;
        @Mock
        private LessonProgressRepository progressRepository;
        @Mock
        private MasteryRepository masteryRepository;
        @Mock
        private DiagnosticResultRepository diagnosticRepository;

        private LearnService learnService;
        private User user;

        @BeforeEach
        void setUp() {
                learnService = new LearnService(
                                lessonRepository,
                                blockRepository,
                                quizRepository,
                                progressRepository,
                                masteryRepository,
                                diagnosticRepository);

                user = new User();
                user.setId(1L);
                user.setEmail("test@example.com");
                user.setPassword("pw");
        }

        // Test to ensure that when a user with an intermediate diagnostic result
        // accesses the lessons, the system bootstraps their mastery levels and keeps
        // advanced lessons locked until they achieve the required mastery
        @Test
        void getAllLessons_bootstrapsMasteryFromDiagnostic_andKeepsAdvancedLocked_whenAverageIsIntermediate() {
                Lesson beginner = lesson("Variables & Data Types", Checkpoint.fundamentals, 1, 1);
                Lesson intermediate = lesson("Arrays", Checkpoint.arrays, 5, 2);
                Lesson advanced = lesson("Collections & Generics", Checkpoint.arrays, 6, 3);

                when(masteryRepository.findByUser(user))
                                .thenReturn(List.of()) // first call -> empty, so bootstrap should happen
                                .thenReturn(List.of( // second call inside getAllLessons
                                                mastery(Checkpoint.fundamentals, 0.60),
                                                mastery(Checkpoint.loops, 0.60),
                                                mastery(Checkpoint.arrays, 0.60),
                                                mastery(Checkpoint.methods, 0.60),
                                                mastery(Checkpoint.oop, 0.60)));

                when(diagnosticRepository.findByUserId(user.getId()))
                                .thenReturn(Optional.of(diagnostic("Medium", "Medium", "Medium", "Medium", "Medium",
                                                "fundamentals")));

                when(progressRepository.findByUser(user)).thenReturn(List.of());
                when(lessonRepository.findByActiveTrueOrderByCheckpointAscOrderIndexAsc())
                                .thenReturn(List.of(beginner, intermediate, advanced));

                AllLessonsResponse response = learnService.getAllLessons(user);

                assertEquals("Intermediate", response.userLevel());

                var advancedLesson = response.lessons().stream()
                                .filter(l -> l.title().equals("Collections & Generics"))
                                .findFirst()
                                .orElseThrow();

                assertTrue(advancedLesson.locked(), "Advanced lesson should still be locked");

                verify(masteryRepository, times(5)).save(any(Mastery.class));
        }
        // If average mastery is at least 0.75, user should be classified as advanced and advanced lessons should be unlocked
        @Test
        void getAllLessons_unlocksAdvanced_whenAverageMasteryIsAtLeast075() {
                Lesson advanced = lesson("Inheritance", Checkpoint.oop, 8, 3);

                when(masteryRepository.findByUser(user))
                                .thenReturn(List.of(
                                                mastery(Checkpoint.fundamentals, 0.80),
                                                mastery(Checkpoint.loops, 0.78),
                                                mastery(Checkpoint.arrays, 0.76),
                                                mastery(Checkpoint.methods, 0.82),
                                                mastery(Checkpoint.oop, 0.79)));

                when(progressRepository.findByUser(user)).thenReturn(List.of());
                when(lessonRepository.findByActiveTrueOrderByCheckpointAscOrderIndexAsc())
                                .thenReturn(List.of(advanced));

                AllLessonsResponse response = learnService.getAllLessons(user);

                assertEquals("Advanced", response.userLevel());
                assertFalse(response.lessons().get(0).locked(), "Advanced lesson should be unlocked");
        }
        //      If average mastery is below 0.45, user should be classified as beginner and advanced lessons should be locked
        @Test
        void submitQuiz_passedQuiz_marksLessonCompleted_andUpdatesCheckpointMastery() {
                Lesson arraysLesson = lesson("Arrays", Checkpoint.arrays, 5, 2);

                LessonQuizQuestion q1 = question(arraysLesson, "Q1", "\"A\"", 1);
                LessonQuizQuestion q2 = question(arraysLesson, "Q2", "\"B\"", 1);
                LessonQuizQuestion q3 = question(arraysLesson, "Q3", "\"C\"", 2);
                LessonQuizQuestion q4 = question(arraysLesson, "Q4", "\"D\"", 2);
                LessonQuizQuestion q5 = question(arraysLesson, "Q5", "\"E\"", 3);

                Lesson oopLesson = lesson("OOP Basics", Checkpoint.oop, 7, 2);

                when(lessonRepository.findById(arraysLesson.getId())).thenReturn(Optional.of(arraysLesson));
                when(quizRepository.findByLessonIdOrderByDifficultyAsc(arraysLesson.getId()))
                                .thenReturn(List.of(q1, q2, q3, q4, q5));

                when(progressRepository.findByUserAndLessonId(user, arraysLesson.getId()))
                                .thenReturn(Optional.empty());

                when(masteryRepository.findByUserAndCheckpoint(user, Checkpoint.arrays))
                                .thenReturn(Optional.of(mastery(Checkpoint.arrays, 0.60)));

                when(masteryRepository.findByUser(user))
                                .thenReturn(List.of(
                                                mastery(Checkpoint.fundamentals, 0.60),
                                                mastery(Checkpoint.loops, 0.60),
                                                mastery(Checkpoint.arrays, 0.68), // after perfect pass, this is what we
                                                                                  // expect
                                                mastery(Checkpoint.methods, 0.60),
                                                mastery(Checkpoint.oop, 0.60)));

                when(diagnosticRepository.findByUserId(user.getId()))
                                .thenReturn(Optional.of(diagnostic("Medium", "Medium", "Medium", "Medium", "Medium",
                                                "fundamentals")));

                when(lessonRepository.findByActiveTrueOrderByCheckpointAscOrderIndexAsc())
                                .thenReturn(List.of(arraysLesson, oopLesson));

                LessonQuizSubmitRequest request = new LessonQuizSubmitRequest(Map.of(
                                q1.getId(), "A",
                                q2.getId(), "B",
                                q3.getId(), "C",
                                q4.getId(), "D",
                                q5.getId(), "E"));

                LessonQuizSubmitResponse response = learnService.submitQuiz(user, arraysLesson.getId(), request);

                assertTrue(response.passed());
                assertEquals(1.0, response.score(), 0.0001);
                assertEquals(0.68, response.updatedCheckpointMastery(), 0.0001);

                ArgumentCaptor<LessonProgress> progressCaptor = ArgumentCaptor.forClass(LessonProgress.class);
                verify(progressRepository, atLeastOnce()).save(progressCaptor.capture());

                LessonProgress savedProgress = progressCaptor.getAllValues()
                                .get(progressCaptor.getAllValues().size() - 1);
                assertEquals(LessonStatus.completed, savedProgress.getStatus());
                assertEquals(1.0, savedProgress.getBestQuizScore(), 0.0001);

                ArgumentCaptor<Mastery> masteryCaptor = ArgumentCaptor.forClass(Mastery.class);
                verify(masteryRepository, atLeastOnce()).save(masteryCaptor.capture());

                Mastery savedMastery = masteryCaptor.getAllValues().get(masteryCaptor.getAllValues().size() - 1);
                assertEquals(Checkpoint.arrays, savedMastery.getCheckpoint());
                assertEquals(0.68, savedMastery.getMasteryValue(), 0.0001);
        }
        // If a user fails a quiz, the lesson should be marked as in_progress (not completed) 
        // and their checkpoint mastery should decrease but not go below the diagnostic baseline for that checkpoint
        @Test
        void submitQuiz_failedQuiz_keepsLessonInProgress_andCanDecreaseCheckpointMastery() {
                Lesson loopsLesson = lesson("Loops", Checkpoint.loops, 3, 1);

                LessonQuizQuestion q1 = question(loopsLesson, "Q1", "\"A\"", 1);
                LessonQuizQuestion q2 = question(loopsLesson, "Q2", "\"B\"", 1);
                LessonQuizQuestion q3 = question(loopsLesson, "Q3", "\"C\"", 1);
                LessonQuizQuestion q4 = question(loopsLesson, "Q4", "\"D\"", 1);
                LessonQuizQuestion q5 = question(loopsLesson, "Q5", "\"E\"", 1);

                when(lessonRepository.findById(loopsLesson.getId())).thenReturn(Optional.of(loopsLesson));
                when(quizRepository.findByLessonIdOrderByDifficultyAsc(loopsLesson.getId()))
                                .thenReturn(List.of(q1, q2, q3, q4, q5));

                LessonProgress existing = new LessonProgress();
                existing.setUser(user);
                existing.setLesson(loopsLesson);
                existing.setStatus(LessonStatus.in_progress);
                existing.setBestQuizScore(0.20);

                when(progressRepository.findByUserAndLessonId(user, loopsLesson.getId()))
                                .thenReturn(Optional.of(existing));

                when(masteryRepository.findByUserAndCheckpoint(user, Checkpoint.loops))
                                .thenReturn(Optional.of(mastery(Checkpoint.loops, 0.60)));

                when(masteryRepository.findByUser(user))
                                .thenReturn(List.of(
                                                mastery(Checkpoint.fundamentals, 0.60),
                                                mastery(Checkpoint.loops, 0.56), // expected after score=0.40
                                                mastery(Checkpoint.arrays, 0.60),
                                                mastery(Checkpoint.methods, 0.60),
                                                mastery(Checkpoint.oop, 0.60)));

                when(diagnosticRepository.findByUserId(user.getId()))
                                .thenReturn(Optional.of(diagnostic("Medium", "Medium", "Medium", "Medium", "Medium",
                                                "fundamentals")));

                when(lessonRepository.findByActiveTrueOrderByCheckpointAscOrderIndexAsc())
                                .thenReturn(List.of(loopsLesson));

                LessonQuizSubmitRequest request = new LessonQuizSubmitRequest(Map.of(
                                q1.getId(), "A",
                                q2.getId(), "B"
                // 2/5 correct => 0.4, fail
                ));

                LessonQuizSubmitResponse response = learnService.submitQuiz(user, loopsLesson.getId(), request);

                assertFalse(response.passed());
                assertEquals(0.4, response.score(), 0.0001);
                assertEquals(0.56, response.updatedCheckpointMastery(), 0.0001);

                ArgumentCaptor<LessonProgress> progressCaptor = ArgumentCaptor.forClass(LessonProgress.class);
                verify(progressRepository, atLeastOnce()).save(progressCaptor.capture());

                LessonProgress savedProgress = progressCaptor.getAllValues()
                                .get(progressCaptor.getAllValues().size() - 1);
                assertEquals(LessonStatus.in_progress, savedProgress.getStatus());
                assertEquals(0.40, savedProgress.getBestQuizScore(), 0.0001);
        }

        // Recomputing mastery from diagnostic should set mastery to baseline values
        // without needing
        // checkpoint completion, and should allow increasing mastery on quiz submission
        // even if diagnostic was weak in that area
        @Test
        void getPath_recommendsFirstIncompleteLessonFromStartCheckpoint() {
                Lesson fundamentals1 = lesson("Variables", Checkpoint.fundamentals, 1, 1);
                Lesson fundamentals2 = lesson("Operators", Checkpoint.fundamentals, 2, 1);
                Lesson loops1 = lesson("Loops", Checkpoint.loops, 3, 1);

                LessonProgress completedProgress = new LessonProgress();
                completedProgress.setUser(user);
                completedProgress.setLesson(fundamentals1);
                completedProgress.setStatus(LessonStatus.completed);

                when(masteryRepository.findByUser(user))
                                .thenReturn(List.of(
                                                mastery(Checkpoint.fundamentals, 0.60),
                                                mastery(Checkpoint.loops, 0.30),
                                                mastery(Checkpoint.arrays, 0.30),
                                                mastery(Checkpoint.methods, 0.30),
                                                mastery(Checkpoint.oop, 0.30)));

                when(diagnosticRepository.findByUserId(user.getId()))
                                .thenReturn(Optional.of(
                                                diagnostic("Medium", "Weak", "Weak", "Weak", "Weak", "fundamentals")));

                when(progressRepository.findByUser(user)).thenReturn(List.of(completedProgress));
                when(lessonRepository.findByActiveTrueOrderByCheckpointAscOrderIndexAsc())
                                .thenReturn(List.of(fundamentals1, fundamentals2, loops1));

                LearnPathResponse response = learnService.getPath(user);

                assertEquals(Checkpoint.fundamentals, response.startCheckpoint());
                assertEquals(fundamentals2.getId(), response.recommendedLessonId());
        }
        // If a user submits a perfect quiz multiple times, their mastery should only
        // increase on the first submission and not on subsequent submissions with the
        // same score

        @Test
        void repeatedPerfectSubmission_shouldNotIncreaseMasteryAgain_afterFirstCompletion() {
                Lesson arraysLesson = lesson("Arrays", Checkpoint.arrays, 5, 2);

                LessonQuizQuestion q1 = question(arraysLesson, "Q1", "\"A\"", 1);
                LessonQuizQuestion q2 = question(arraysLesson, "Q2", "\"B\"", 1);
                LessonQuizQuestion q3 = question(arraysLesson, "Q3", "\"C\"", 2);
                LessonQuizQuestion q4 = question(arraysLesson, "Q4", "\"D\"", 2);
                LessonQuizQuestion q5 = question(arraysLesson, "Q5", "\"E\"", 3);

                when(lessonRepository.findById(arraysLesson.getId())).thenReturn(Optional.of(arraysLesson));
                when(quizRepository.findByLessonIdOrderByDifficultyAsc(arraysLesson.getId()))
                                .thenReturn(List.of(q1, q2, q3, q4, q5));

                when(diagnosticRepository.findByUserId(user.getId()))
                                .thenReturn(Optional.of(diagnostic("Medium", "Medium", "Medium", "Medium", "Medium",
                                                "fundamentals")));

                when(lessonRepository.findByActiveTrueOrderByCheckpointAscOrderIndexAsc())
                                .thenReturn(List.of(arraysLesson));

                LessonQuizSubmitRequest perfectRequest = new LessonQuizSubmitRequest(Map.of(
                                q1.getId(), "A",
                                q2.getId(), "B",
                                q3.getId(), "C",
                                q4.getId(), "D",
                                q5.getId(), "E"));

                AtomicReference<LessonProgress> progressState = new AtomicReference<>();
                AtomicReference<Mastery> masteryState = new AtomicReference<>(mastery(Checkpoint.arrays, 0.60));

                when(progressRepository.findByUserAndLessonId(user, arraysLesson.getId()))
                                .thenAnswer(inv -> Optional.ofNullable(progressState.get()));

                when(progressRepository.save(any(LessonProgress.class)))
                                .thenAnswer(inv -> {
                                        LessonProgress p = inv.getArgument(0);
                                        progressState.set(p);
                                        return p;
                                });

                when(masteryRepository.findByUserAndCheckpoint(user, Checkpoint.arrays))
                                .thenAnswer(inv -> Optional.of(masteryState.get()));

                when(masteryRepository.save(any(Mastery.class)))
                                .thenAnswer(inv -> {
                                        Mastery m = inv.getArgument(0);
                                        masteryState.set(m);
                                        return m;
                                });

                when(masteryRepository.findByUser(user))
                                .thenAnswer(inv -> List.of(
                                                mastery(Checkpoint.fundamentals, 0.60),
                                                mastery(Checkpoint.loops, 0.60),
                                                masteryState.get(),
                                                mastery(Checkpoint.methods, 0.60),
                                                mastery(Checkpoint.oop, 0.60)));

                LessonQuizSubmitResponse first = learnService.submitQuiz(user, arraysLesson.getId(), perfectRequest);
                LessonQuizSubmitResponse second = learnService.submitQuiz(user, arraysLesson.getId(), perfectRequest);

                assertEquals(0.68, first.updatedCheckpointMastery(), 0.0001);
                assertEquals(0.68, second.updatedCheckpointMastery(), 0.0001,
                                "Mastery should not increase again for the same lesson when score did not improve");
        }

        // ---------------- helpers ----------------

        private Lesson lesson(String title, Checkpoint checkpoint, int orderIndex, int difficulty) {
                Lesson l = new Lesson();
                l.setId(UUID.randomUUID());
                l.setTitle(title);
                l.setCheckpoint(checkpoint);
                l.setOrderIndex(orderIndex);
                l.setEstimatedMinutes(15);
                l.setDifficulty(difficulty);
                l.setActive(true);
                return l;
        }

        private LessonQuizQuestion question(Lesson lesson, String prompt, String correctJson, int difficulty) {
                LessonQuizQuestion q = new LessonQuizQuestion();
                q.setId(UUID.randomUUID());
                q.setLesson(lesson);
                q.setType("MCQ");
                q.setPrompt(prompt);
                q.setOptionsJson("[\"A\",\"B\",\"C\",\"D\",\"E\"]");
                q.setCorrectJson(correctJson);
                q.setDifficulty(difficulty);
                return q;
        }

        private Mastery mastery(Checkpoint checkpoint, double value) {
                Mastery m = new Mastery();
                m.setUser(user);
                m.setCheckpoint(checkpoint);
                m.setMasteryValue(value);
                m.setUpdatedAt(Instant.now());
                return m;
        }

        private DiagnosticResult diagnostic(
                        String fundamentals,
                        String loops,
                        String arrays,
                        String methods,
                        String oop,
                        String startModule) {
                DiagnosticResult dr = new DiagnosticResult();
                dr.setUser(user);
                dr.setFundamentals(fundamentals);
                dr.setLoops(loops);
                dr.setArrays(arrays);
                dr.setMethods(methods);
                dr.setOop(oop);
                dr.setStartModule(startModule);
                dr.setCompletedAt(Instant.now());
                return dr;
        }

        private LessonProgress progress(Lesson lesson, LessonStatus status) {
                LessonProgress p = new LessonProgress();
                p.setUser(user);
                p.setLesson(lesson);
                p.setStatus(status);
                return p;
        }

        // Recommends weakest unfinished unlocked lesson first
        @Test
        void getRecommendations_shouldPrioritizeWeakestUnlockedUnfinishedLessons() {
                Lesson arrays = lesson("Arrays", Checkpoint.arrays, 5, 2);
                Lesson methods = lesson("Methods", Checkpoint.methods, 6, 2);
                Lesson oop = lesson("OOP Basics", Checkpoint.oop, 7, 2);

                when(masteryRepository.findByUser(user)).thenReturn(List.of(
                                mastery(Checkpoint.fundamentals, 0.90),
                                mastery(Checkpoint.loops, 0.80),
                                mastery(Checkpoint.arrays, 0.30),
                                mastery(Checkpoint.methods, 0.40),
                                mastery(Checkpoint.oop, 0.60)));

                when(progressRepository.findByUser(user)).thenReturn(List.of());

                when(lessonRepository.findByActiveTrueOrderByCheckpointAscOrderIndexAsc())
                                .thenReturn(List.of(arrays, methods, oop));

                var response = learnService.getRecommendations(user);

                assertNotNull(response);
                assertEquals(Checkpoint.arrays, response.primaryCheckpoint());
                assertFalse(response.recommendedLessons().isEmpty());
                assertEquals("Arrays", response.recommendedLessons().get(0).title());
        }

        // Completed lessons should not be recommended, even if they are weak areas
        @Test
        void getRecommendations_shouldSkipCompletedLessons() {
                Lesson arrays = lesson("Arrays", Checkpoint.arrays, 5, 2);
                Lesson methods = lesson("Methods", Checkpoint.methods, 6, 2);

                LessonProgress arraysCompleted = progress(arrays, LessonStatus.completed);

                when(masteryRepository.findByUser(user)).thenReturn(List.of(
                                mastery(Checkpoint.fundamentals, 0.90),
                                mastery(Checkpoint.loops, 0.80),
                                mastery(Checkpoint.arrays, 0.20),
                                mastery(Checkpoint.methods, 0.40),
                                mastery(Checkpoint.oop, 0.70)));

                when(progressRepository.findByUser(user)).thenReturn(List.of(arraysCompleted));

                when(lessonRepository.findByActiveTrueOrderByCheckpointAscOrderIndexAsc())
                                .thenReturn(List.of(arrays, methods));

                var response = learnService.getRecommendations(user);

                assertNotNull(response);
                assertFalse(response.recommendedLessons().isEmpty());
                assertEquals("Methods", response.recommendedLessons().get(0).title());
                assertTrue(response.recommendedLessons().stream().noneMatch(l -> l.title().equals("Arrays")));
        }

        // Locked lessons should not be recommended, even if they are weak areas
        @Test
        void getRecommendations_shouldSkipLockedLessons() {
                Lesson advancedArrays = lesson("Collections & Generics", Checkpoint.arrays, 12, 3);
                Lesson methods = lesson("Methods", Checkpoint.methods, 6, 2);

                when(masteryRepository.findByUser(user)).thenReturn(List.of(
                                mastery(Checkpoint.fundamentals, 0.60),
                                mastery(Checkpoint.loops, 0.60),
                                mastery(Checkpoint.arrays, 0.20),
                                mastery(Checkpoint.methods, 0.40),
                                mastery(Checkpoint.oop, 0.60)));

                when(progressRepository.findByUser(user)).thenReturn(List.of());

                when(lessonRepository.findByActiveTrueOrderByCheckpointAscOrderIndexAsc())
                                .thenReturn(List.of(advancedArrays, methods));

                var response = learnService.getRecommendations(user);

                assertNotNull(response);
                assertFalse(response.recommendedLessons().isEmpty());
                assertEquals("Methods", response.recommendedLessons().get(0).title());
                assertTrue(response.recommendedLessons().stream()
                                .noneMatch(l -> l.title().equals("Collections & Generics")));
        }

        // If all lessons are completed, there should be no recommendations even if some
        // areas are weak
        @Test
        void getRecommendations_shouldReturnEmpty_whenEverythingCompleted() {
                Lesson arrays = lesson("Arrays", Checkpoint.arrays, 5, 2);
                Lesson methods = lesson("Methods", Checkpoint.methods, 6, 2);

                LessonProgress arraysCompleted = progress(arrays, LessonStatus.completed);
                LessonProgress methodsCompleted = progress(methods, LessonStatus.completed);

                when(masteryRepository.findByUser(user)).thenReturn(List.of(
                                mastery(Checkpoint.fundamentals, 0.95),
                                mastery(Checkpoint.loops, 0.95),
                                mastery(Checkpoint.arrays, 0.95),
                                mastery(Checkpoint.methods, 0.95),
                                mastery(Checkpoint.oop, 0.95)));

                when(progressRepository.findByUser(user)).thenReturn(List.of(arraysCompleted, methodsCompleted));

                when(lessonRepository.findByActiveTrueOrderByCheckpointAscOrderIndexAsc())
                                .thenReturn(List.of(arrays, methods));

                var response = learnService.getRecommendations(user);

                assertNotNull(response);
                assertTrue(response.recommendedLessons().isEmpty());
                assertNull(response.primaryLessonId());
                assertNull(response.primaryCheckpoint());
        }

        // If there are unlocked incomplete lessons, there should be recommendations
        // even if mastery is low in some areas
        @Test
        void getRecommendations_shouldNotBeEmpty_whenUserHasUnlockedIncompleteLessons() {
                Lesson arrays = lesson("Arrays", Checkpoint.arrays, 5, 2);
                Lesson oop = lesson("OOP Basics", Checkpoint.oop, 7, 2);

                when(masteryRepository.findByUser(user)).thenReturn(List.of(
                                mastery(Checkpoint.fundamentals, 0.90),
                                mastery(Checkpoint.loops, 0.80),
                                mastery(Checkpoint.arrays, 0.30),
                                mastery(Checkpoint.methods, 0.80),
                                mastery(Checkpoint.oop, 0.50)));

                when(progressRepository.findByUser(user)).thenReturn(List.of());
                when(lessonRepository.findByActiveTrueOrderByCheckpointAscOrderIndexAsc())
                                .thenReturn(List.of(arrays, oop));

                var response = learnService.getRecommendations(user);

                assertFalse(response.recommendedLessons().isEmpty(),
                                "Expected recommendations because user still has unlocked incomplete lessons");
        }
}