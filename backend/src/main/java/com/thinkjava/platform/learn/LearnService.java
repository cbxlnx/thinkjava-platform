package com.thinkjava.platform.learn;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thinkjava.platform.diagnostic.result.DiagnosticResult;
import com.thinkjava.platform.diagnostic.result.DiagnosticResultRepository;
import com.thinkjava.platform.learn.dto.AllLessonsResponse;
import com.thinkjava.platform.learn.dto.LearnPathResponse;
import com.thinkjava.platform.learn.dto.LearnRecommendationsResponse;
import com.thinkjava.platform.learn.dto.LessonQuizSubmitRequest;
import com.thinkjava.platform.learn.dto.LessonQuizSubmitResponse;
import com.thinkjava.platform.learn.dto.LessonResponse;
import com.thinkjava.platform.learn.dto.LessonSummaryResponse;
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
import com.thinkjava.platform.learn.section.LessonBlock;
import com.thinkjava.platform.learn.section.LessonBlockRepository;
import com.thinkjava.platform.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LearnService {

    private final LessonRepository lessonRepository;
    private final LessonBlockRepository blockRepository;
    private final LessonQuizQuestionRepository quizRepository;
    private final LessonProgressRepository progressRepository;
    private final MasteryRepository masteryRepository;
    private final DiagnosticResultRepository diagnosticRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final double PASS_THRESHOLD = 0.80;

    public LearnService(
            LessonRepository lessonRepository,
            LessonBlockRepository blockRepository,
            LessonQuizQuestionRepository quizRepository,
            LessonProgressRepository progressRepository,
            MasteryRepository masteryRepository,
            DiagnosticResultRepository diagnosticRepository) {
        this.lessonRepository = lessonRepository;
        this.blockRepository = blockRepository;
        this.quizRepository = quizRepository;
        this.progressRepository = progressRepository;
        this.masteryRepository = masteryRepository;
        this.diagnosticRepository = diagnosticRepository;
    }

    // ---------------------------------------------------------
    // PATH
    // ---------------------------------------------------------
    @Transactional
    public LearnPathResponse getPath(User user) {
        ensureMasteryBootstrapped(user);

        Map<Checkpoint, Double> masteryMap = masteryRepository.findByUser(user)
                .stream()
                .collect(Collectors.toMap(Mastery::getCheckpoint, Mastery::getMasteryValue));

        Checkpoint startCheckpoint = getStartCheckpoint(user);
        UUID recommendedLessonId = recommendNextLessonId(user, startCheckpoint, masteryMap);

        return new LearnPathResponse(recommendedLessonId, startCheckpoint, masteryMap);
    }

    private Checkpoint getStartCheckpoint(User user) {
        // DiagnosticResultRepository in your project uses userId (Long) keep as-is
        return diagnosticRepository.findByUserId(user.getId())
                .map(DiagnosticResult::getStartModule)
                .map(this::toCheckpointSafe)
                .orElse(Checkpoint.fundamentals);
    }

    private Checkpoint toCheckpointSafe(String raw) {
        if (raw == null)
            return Checkpoint.fundamentals;
        try {
            return Checkpoint.valueOf(raw.trim());
        } catch (Exception e) {
            return Checkpoint.fundamentals;
        }
    }

    private UUID recommendNextLessonId(User user, Checkpoint start, Map<Checkpoint, Double> masteryMap) {
        List<Lesson> allLessons = lessonRepository.findByActiveTrueOrderByCheckpointAscOrderIndexAsc();
        if (allLessons.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No lessons available");
        }

        Set<UUID> completedLessonIds = progressRepository.findByUser(user).stream()
                .filter(p -> p.getStatus() == LessonStatus.completed)// if there is an incomplete lesson , return it.
                .map(p -> p.getLesson().getId())
                .collect(Collectors.toSet());

        List<Checkpoint> order = List.of(
                Checkpoint.fundamentals, Checkpoint.loops, Checkpoint.arrays, Checkpoint.methods, Checkpoint.oop);

        int startIdx = order.indexOf(start);
        if (startIdx < 0)
            startIdx = 0;

        for (int i = startIdx; i < order.size(); i++) {
            Checkpoint cp = order.get(i);
            double mastery = masteryMap.getOrDefault(cp, 0.0);

            List<Lesson> cpLessons = allLessons.stream()
                    .filter(l -> l.getCheckpoint() == cp)
                    .sorted(Comparator.comparingInt(Lesson::getOrderIndex))
                    .toList();

            if (cpLessons.isEmpty())
                continue;

            Optional<Lesson> firstIncomplete = cpLessons.stream()
                    .filter(l -> !completedLessonIds.contains(l.getId()))
                    .findFirst();

            if (firstIncomplete.isPresent())
                return firstIncomplete.get().getId();

            // all completed: move forward only if mastery is high
            if (mastery >= 0.80)
                continue;

            // mastery not high suggest last lesson for review
            return cpLessons.get(cpLessons.size() - 1).getId();
        }

        // everything done last lesson
        return allLessons.get(allLessons.size() - 1).getId();
    }

    // ---------------------------------------------------------
    // LESSON FETCH
    // ---------------------------------------------------------
    @Transactional
    public LessonResponse getLesson(User user, UUID lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));

        // load blocks ordered by section_order (mapped as orderIndex in LessonBlock)
        List<LessonBlock> blocks = blockRepository.findByLessonIdOrderByOrderIndexAsc(lessonId);

        List<LessonResponse.BlockDto> blockDtos = blocks.stream()
                .map(b -> new LessonResponse.BlockDto(
                        b.getOrderIndex(),
                        b.getType(),
                        b.getMarkdown(),
                        b.getVideoTitle(),
                        b.getVideoUrl(),
                        b.getPayload()))
                .toList();
        // load quiz questions ordered by difficulty
        List<LessonQuizQuestion> quizEntities = quizRepository.findByLessonIdOrderByDifficultyAsc(lessonId);
        List<LessonResponse.QuizQuestionDto> quizQuestions = quizEntities.stream()
                .map(q -> new LessonResponse.QuizQuestionDto(
                        q.getId(),
                        q.getPrompt(),
                        parseOptions(q.getOptionsJson())))
                .toList();

        upsertInProgress(user, lesson);

        return new LessonResponse(
                new LessonResponse.LessonMeta(
                        lesson.getId(),
                        lesson.getCheckpoint(),
                        lesson.getTitle(),
                        lesson.getOrderIndex(),
                        lesson.getEstimatedMinutes()),
                blockDtos,
                new LessonResponse.QuizDto(quizQuestions));
    }

    // helper to mark lesson as in_progress if not already, and update last seen
    // timestamp
    private void upsertInProgress(User user, Lesson lesson) {
        LessonProgress p = progressRepository.findByUserAndLessonId(user, lesson.getId())
                .orElseGet(LessonProgress::new);

        p.setUser(user);
        p.setLesson(lesson);

        if (p.getStatus() == null)
            p.setStatus(LessonStatus.in_progress);

        p.setLastSeenAt(Instant.now());
        progressRepository.save(p);
    }

    // helper to parse options JSON into List<String>
    private List<String> parseOptions(String optionsJson) {
        try {
            return objectMapper.readValue(optionsJson, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }

    // ---------------------------------------------------------
    // QUIZ SUBMIT
    // ---------------------------------------------------------
    // main method to handle quiz submission, calculate score, update progress and
    // mastery, and recommend next lesson
    @Transactional
    public LessonQuizSubmitResponse submitQuiz(User user, UUID lessonId, LessonQuizSubmitRequest req) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));

        List<LessonQuizQuestion> questions = quizRepository.findByLessonIdOrderByDifficultyAsc(lessonId);
        if (questions.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lesson has no quiz questions");
        }

        Map<UUID, String> answers = (req.answers() == null) ? Map.of() : req.answers();

        int correct = 0;
        for (LessonQuizQuestion q : questions) {
            String userAnswer = answers.get(q.getId());
            String correctAnswer = parseCorrect(q.getCorrectJson());
            if (userAnswer != null && userAnswer.equals(correctAnswer)) {
                correct++;
            }
        }

        double score = (double) correct / questions.size();
        boolean passed = score >= PASS_THRESHOLD;

        LessonProgress p = progressRepository.findByUserAndLessonId(user, lessonId)
                .orElseGet(LessonProgress::new);

        Double previousBest = p.getBestQuizScore();

        p.setUser(user);
        p.setLesson(lesson);
        p.setLastSeenAt(Instant.now());
        p.setBestQuizScore(previousBest == null ? score : Math.max(previousBest, score));

        if (passed) {
            p.setStatus(LessonStatus.completed);
        } else if (p.getStatus() == null) {
            p.setStatus(LessonStatus.in_progress);
        }

        progressRepository.save(p);

        double updatedCheckpointMastery = recomputeCheckpointMastery(user, lesson.getCheckpoint());

        Map<Checkpoint, Double> masteryMap = masteryRepository.findByUser(user)
                .stream()
                .collect(Collectors.toMap(Mastery::getCheckpoint, Mastery::getMasteryValue));

        UUID nextLessonId = recommendNextLessonId(user, getStartCheckpoint(user), masteryMap);

        return new LessonQuizSubmitResponse(score, passed, updatedCheckpointMastery, nextLessonId);
    }

    // helper to parse correct answer JSON (could be string or more complex
    // structure)
    private String parseCorrect(String correctJson) {
        try {
            return objectMapper.readValue(correctJson, String.class);
        } catch (Exception e) {
            return correctJson;
        }
    }

    // ---------------------------------------------------------
    // MASTERY UPDATE
    // ---------------------------------------------------------

    // method to recompute mastery for a checkpoint based on diagnostic baseline and
    // lesson completion ratio, and update the Mastery entity
    private double recomputeCheckpointMastery(User user, Checkpoint checkpoint) {
    DiagnosticResult dr = diagnosticRepository.findByUserId(user.getId()).orElseThrow();

    double baseline = switch (checkpoint) {
        case fundamentals -> mapLevel(dr.getFundamentals());
        case loops -> mapLevel(dr.getLoops());
        case arrays -> mapLevel(dr.getArrays());
        case methods -> mapLevel(dr.getMethods());
        case oop -> mapLevel(dr.getOop());
    };

    List<Lesson> checkpointLessons = lessonRepository
            .findByActiveTrueOrderByCheckpointAscOrderIndexAsc()
            .stream()
            .filter(l -> l.getCheckpoint() == checkpoint)
            .toList();

    Map<UUID, LessonProgress> progressMap = progressRepository.findByUser(user)
            .stream()
            .collect(Collectors.toMap(p -> p.getLesson().getId(), p -> p));

    long completed = checkpointLessons.stream()
            .filter(l -> {
                LessonProgress p = progressMap.get(l.getId());
                return p != null && p.getStatus() == LessonStatus.completed;
            })
            .count();

    Mastery m = masteryRepository.findByUserAndCheckpoint(user, checkpoint)
            .orElseGet(Mastery::new);

    m.setUser(user);
    m.setCheckpoint(checkpoint);

    double mastery;

    // no lessons in this checkpoint -> keep baseline
    if (checkpointLessons.isEmpty()) {
        mastery = baseline;
    }
    // checkpoint fully completed -> boost mastery
    else if (completed == checkpointLessons.size()) {
        mastery = Math.max(baseline, 0.85);
    }
    // checkpoint NOT fully completed -> keep current stored mastery (or baseline if missing)
    else {
        Double currentMastery = m.getMasteryValue();
        mastery = (currentMastery != null) ? currentMastery : baseline;
    }

    mastery = clamp(mastery, 0.0, 1.0);

    m.setMasteryValue(mastery);
    m.setUpdatedAt(Instant.now());
    masteryRepository.save(m);

    return mastery;
}

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    @Transactional
    public void recomputeAllCheckpointMastery(User user) {
        for (Checkpoint cp : Checkpoint.values()) {
            recomputeCheckpointMastery(user, cp);
        }
    }

    // ---------------------------------------------------------
    // BOOTSTRAP mastery from DiagnosticResult (first time only)
    // ---------------------------------------------------------
    private void ensureMasteryBootstrapped(User user) {
        if (!masteryRepository.findByUser(user).isEmpty())
            return;

        DiagnosticResult dr = diagnosticRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Diagnostic not completed"));

        Instant now = Instant.now();

        createMastery(user, Checkpoint.fundamentals, mapLevel(dr.getFundamentals()), now);
        createMastery(user, Checkpoint.loops, mapLevel(dr.getLoops()), now);
        createMastery(user, Checkpoint.arrays, mapLevel(dr.getArrays()), now);
        createMastery(user, Checkpoint.methods, mapLevel(dr.getMethods()), now);
        createMastery(user, Checkpoint.oop, mapLevel(dr.getOop()), now);
    }

    private void createMastery(User user, Checkpoint cp, double value, Instant now) {
        Mastery m = new Mastery();
        m.setUser(user);
        m.setCheckpoint(cp);
        m.setMasteryValue(value);
        m.setUpdatedAt(now);
        masteryRepository.save(m);
    }

    private double mapLevel(String level) {
        if (level == null)
            return 0.10;
        return switch (level) {
            case "Strong" -> 0.85;
            case "Medium" -> 0.60;
            case "Weak" -> 0.30;
            case "Unknown" -> 0.10;
            default -> 0.10;
        };
    }

    private int progressForStatus(String status) {
        return switch (status) {
            case "completed" -> 100;
            case "in_progress" -> 50;
            default -> 0;
        };
    }

    // ---------------------------------------------------------
    // ALL LESSONS
    // ---------------------------------------------------------
    @Transactional
    public AllLessonsResponse getAllLessons(User user) {
        ensureMasteryBootstrapped(user);

        Map<Checkpoint, Double> mastery = masteryRepository.findByUser(user)
                .stream()
                .collect(Collectors.toMap(Mastery::getCheckpoint, Mastery::getMasteryValue));

        Map<UUID, LessonProgress> progressMap = progressRepository.findByUser(user).stream()
                .collect(Collectors.toMap(p -> p.getLesson().getId(), p -> p));

        int userMaxDifficulty = getEffectiveMaxDifficulty(user, mastery, progressMap);

        String userLevel = userMaxDifficulty == 3 ? "Advanced"
                : userMaxDifficulty == 2 ? "Intermediate"
                        : "Beginner";
        List<Lesson> lessons = lessonRepository.findByActiveTrueOrderByCheckpointAscOrderIndexAsc();

        var out = lessons.stream().map(l -> {
            LessonProgress p = progressMap.get(l.getId());
            String status = (p == null) ? "not_started" : p.getStatus().name().toLowerCase();
            int progressPercent = progressForStatus(status);

            boolean locked = isLocked(l, status, userMaxDifficulty);

            String levelTag = l.getDifficulty() == 1 ? "Beginner"
                    : l.getDifficulty() == 2 ? "Intermediate" : "Advanced";

            return new LessonSummaryResponse(
                    l.getId(),
                    l.getTitle(),
                    l.getCheckpoint(),
                    l.getOrderIndex(),
                    l.getEstimatedMinutes(),
                    l.getDifficulty(),
                    levelTag,
                    status,
                    progressPercent,
                    locked);
        }).toList();

        return new AllLessonsResponse(userLevel, out);
    }

    private boolean isLocked(Lesson lesson, String status, int userMaxDifficulty) {
        boolean completed = "completed".equals(status);
        return !completed && lesson.getDifficulty() > userMaxDifficulty;
    }

    @Transactional
    public LearnRecommendationsResponse getRecommendations(User user) {
        ensureMasteryBootstrapped(user);

        Map<Checkpoint, Double> mastery = masteryRepository.findByUser(user)
                .stream()
                .collect(Collectors.toMap(Mastery::getCheckpoint, Mastery::getMasteryValue));

        Map<UUID, LessonProgress> progressMap = progressRepository.findByUser(user).stream()
                .collect(Collectors.toMap(p -> p.getLesson().getId(), p -> p));
        int userMaxDifficulty = getEffectiveMaxDifficulty(user, mastery, progressMap);
        List<Checkpoint> orderedWeakAreas = mastery.entrySet().stream()
                .sorted(Map.Entry.comparingByValue()) // weakest first
                .map(Map.Entry::getKey)
                .toList();

        List<Lesson> allLessons = lessonRepository.findByActiveTrueOrderByCheckpointAscOrderIndexAsc();

        List<LessonSummaryResponse> recommended = new ArrayList<>();

        for (Checkpoint cp : orderedWeakAreas) {
            List<Lesson> cpLessons = allLessons.stream()
                    .filter(l -> l.getCheckpoint() == cp)
                    .sorted(Comparator.comparingInt(Lesson::getOrderIndex))
                    .toList();

            for (Lesson l : cpLessons) {
                LessonProgress p = progressMap.get(l.getId());
                String status = (p == null) ? "not_started" : p.getStatus().name().toLowerCase();

                boolean completed = "completed".equals(status);
                boolean locked = !completed && l.getDifficulty() > userMaxDifficulty;

                if (locked || completed)
                    continue;

                int progressPercent = progressForStatus(status);
                String levelTag = l.getDifficulty() == 1 ? "Beginner"
                        : l.getDifficulty() == 2 ? "Intermediate" : "Advanced";

                recommended.add(new LessonSummaryResponse(
                        l.getId(),
                        l.getTitle(),
                        l.getCheckpoint(),
                        l.getOrderIndex(),
                        l.getEstimatedMinutes(),
                        l.getDifficulty(),
                        levelTag,
                        status,
                        progressPercent,
                        locked));

                if (recommended.size() >= 3)
                    break;
            }

            if (recommended.size() >= 3)
                break;
        }

        UUID primaryLessonId = recommended.isEmpty() ? null : recommended.get(0).id();
        Checkpoint primaryCheckpoint = recommended.isEmpty() ? null : recommended.get(0).checkpoint();

        return new LearnRecommendationsResponse(
                primaryLessonId,
                primaryCheckpoint,
                "Recommended from weakest diagnostic/mastery areas",
                orderedWeakAreas.stream().limit(2).toList(),
                recommended);
    }

    // main method to determine the current focus lesson for the user, preferring
    // in-progress lessons, then recommendations, and applying locking logic based
    // on mastery and progression
    @Transactional
    public LessonSummaryResponse getCurrentFocus(User user) {
        ensureMasteryBootstrapped(user);

        Map<Checkpoint, Double> mastery = masteryRepository.findByUser(user).stream()
                .collect(Collectors.toMap(Mastery::getCheckpoint, Mastery::getMasteryValue));

        Map<UUID, LessonProgress> progressMap = progressRepository.findByUser(user).stream()
                .collect(Collectors.toMap(p -> p.getLesson().getId(), p -> p));

        int userMaxDifficulty = getEffectiveMaxDifficulty(user, mastery, progressMap);

        List<Lesson> lessons = lessonRepository.findByActiveTrueOrderByCheckpointAscOrderIndexAsc();

        // prefer an in progress lesson that is still accessible
        for (Lesson l : lessons) {
            LessonProgress p = progressMap.get(l.getId());
            if (p != null && p.getStatus() == LessonStatus.in_progress) {
                boolean locked = l.getDifficulty() > userMaxDifficulty;
                if (!locked) {
                    return mapToSummary(l, p, userMaxDifficulty);
                }
            }
        }

        // fallback to recommendations
        LearnRecommendationsResponse rec = getRecommendations(user);
        if (!rec.recommendedLessons().isEmpty()) {
            return rec.recommendedLessons().get(0);
        }

        return null;
    }

    @Transactional
    public void resetMasteryFromDiagnostic(User user) {
        DiagnosticResult dr = diagnosticRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Diagnostic not completed"));

        Instant now = Instant.now();

        upsertMastery(user, Checkpoint.fundamentals, mapLevel(dr.getFundamentals()), now);
        upsertMastery(user, Checkpoint.loops, mapLevel(dr.getLoops()), now);
        upsertMastery(user, Checkpoint.arrays, mapLevel(dr.getArrays()), now);
        upsertMastery(user, Checkpoint.methods, mapLevel(dr.getMethods()), now);
        upsertMastery(user, Checkpoint.oop, mapLevel(dr.getOop()), now);
    }

    private void upsertMastery(User user, Checkpoint checkpoint, double value, Instant now) {
        Mastery mastery = masteryRepository.findByUserAndCheckpoint(user, checkpoint)
                .orElseGet(Mastery::new);

        mastery.setUser(user);
        mastery.setCheckpoint(checkpoint);
        mastery.setMasteryValue(value);
        mastery.setUpdatedAt(now);

        masteryRepository.save(mastery);
    }

    // helper to determine max difficulty unlock based on mastery and progression
    // (fallback if all the lessons are completed but mastery is low)
    private int getEffectiveMaxDifficulty(User user, Map<Checkpoint, Double> mastery,
            Map<UUID, LessonProgress> progressMap) {
        double avg = mastery.values().stream().mapToDouble(d -> d).average().orElse(0.0);

        int userMaxDifficulty = (avg >= 0.75) ? 3 : (avg >= 0.45) ? 2 : 1;

        List<Lesson> allLessons = lessonRepository.findByActiveTrueOrderByCheckpointAscOrderIndexAsc();

        boolean allBeginnerCompleted = allLessons.stream()
                .filter(l -> l.getDifficulty() == 1)
                .allMatch(l -> {
                    LessonProgress p = progressMap.get(l.getId());
                    return p != null && p.getStatus() == LessonStatus.completed;
                });

        boolean allIntermediateCompleted = allLessons.stream()
                .filter(l -> l.getDifficulty() == 2)
                .allMatch(l -> {
                    LessonProgress p = progressMap.get(l.getId());
                    return p != null && p.getStatus() == LessonStatus.completed;
                });

        // fallback progression unlock
        if (userMaxDifficulty < 2 && allBeginnerCompleted) {
            userMaxDifficulty = 2;
        }

        if (userMaxDifficulty < 3 && allBeginnerCompleted && allIntermediateCompleted) {
            userMaxDifficulty = 3;
        }

        return userMaxDifficulty;
    }

    // helper to map Lesson and Progress to LessonSummaryResponse
    private LessonSummaryResponse mapToSummary(
            Lesson lesson,
            LessonProgress p,
            int userMaxDifficulty) {
        String status = (p == null) ? "not_started" : p.getStatus().name().toLowerCase();
        int progressPercent = progressForStatus(status);

        boolean completed = "completed".equals(status);
        boolean locked = !completed && lesson.getDifficulty() > userMaxDifficulty;

        String levelTag = lesson.getDifficulty() == 1 ? "Beginner"
                : lesson.getDifficulty() == 2 ? "Intermediate"
                        : "Advanced";

        return new LessonSummaryResponse(
                lesson.getId(),
                lesson.getTitle(),
                lesson.getCheckpoint(),
                lesson.getOrderIndex(),
                lesson.getEstimatedMinutes(),
                lesson.getDifficulty(),
                levelTag,
                status,
                progressPercent,
                locked);
    }
}