package com.thinkjava.platform.learn;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thinkjava.platform.diagnostic.result.DiagnosticResult;
import com.thinkjava.platform.diagnostic.result.DiagnosticResultRepository;
import com.thinkjava.platform.learn.dto.AllLessonsResponse;
import com.thinkjava.platform.learn.dto.LearnPathResponse;
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
import com.thinkjava.platform.learn.section.LessonSection;
import com.thinkjava.platform.learn.section.LessonSectionRepository;
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
    private final LessonSectionRepository sectionRepository;
    private final LessonQuizQuestionRepository quizRepository;
    private final LessonProgressRepository progressRepository;
    private final MasteryRepository masteryRepository;
    private final DiagnosticResultRepository diagnosticRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final double PASS_THRESHOLD = 0.80;

    public LearnService(
            LessonRepository lessonRepository,
            LessonSectionRepository sectionRepository,
            LessonQuizQuestionRepository quizRepository,
            LessonProgressRepository progressRepository,
            MasteryRepository masteryRepository,
            DiagnosticResultRepository diagnosticRepository) {
        this.lessonRepository = lessonRepository;
        this.sectionRepository = sectionRepository;
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
        // DiagnosticResultRepository in your project uses userId (Long) -> keep as-is
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
                .filter(p -> p.getStatus() == LessonStatus.completed)
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

            // mastery not high -> suggest last lesson for review
            return cpLessons.get(cpLessons.size() - 1).getId();
        }

        // everything done -> last lesson
        return allLessons.get(allLessons.size() - 1).getId();
    }

    // ---------------------------------------------------------
    // LESSON FETCH
    // ---------------------------------------------------------
    @Transactional
    public LessonResponse getLesson(User user, UUID lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found"));

        List<LessonSection> sectionEntities = sectionRepository.findByLessonIdOrderBySectionOrderAsc(lessonId);
        List<LessonResponse.SectionDto> sections = sectionEntities.stream()
                .map(s -> new LessonResponse.SectionDto(s.getSectionOrder(), s.getMarkdown()))
                .toList();

        List<LessonQuizQuestion> quizEntities = quizRepository.findByLessonIdOrderByDifficultyAsc(lessonId);
        List<LessonResponse.QuizQuestionDto> quizQuestions = quizEntities.stream()
                .map(q -> new LessonResponse.QuizQuestionDto(q.getId(), q.getPrompt(),
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
                sections,
                new LessonResponse.QuizDto(quizQuestions));
    }

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
            if (userAnswer != null && userAnswer.equals(correctAnswer))
                correct++;
        }

        double score = (double) correct / questions.size();
        boolean passed = score >= PASS_THRESHOLD;

        LessonProgress p = progressRepository.findByUserAndLessonId(user, lessonId)
                .orElseGet(LessonProgress::new);

        p.setUser(user);
        p.setLesson(lesson);
        p.setLastSeenAt(Instant.now());
        p.setBestQuizScore(p.getBestQuizScore() == null ? score : Math.max(p.getBestQuizScore(), score));

        if (passed)
            p.setStatus(LessonStatus.completed);
        else if (p.getStatus() == null)
            p.setStatus(LessonStatus.in_progress);

        progressRepository.save(p);

        double updatedCheckpointMastery = updateMastery(user, lesson.getCheckpoint(), score);

        Map<Checkpoint, Double> masteryMap = masteryRepository.findByUser(user)
                .stream()
                .collect(Collectors.toMap(Mastery::getCheckpoint, Mastery::getMasteryValue));

        UUID nextLessonId = recommendNextLessonId(user, getStartCheckpoint(user), masteryMap);

        return new LessonQuizSubmitResponse(score, passed, updatedCheckpointMastery, nextLessonId);
    }

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
    private double updateMastery(User user, Checkpoint checkpoint, double score) {
        // IMPORTANT: repository uses User, not userId
        Mastery m = masteryRepository.findByUserAndCheckpoint(user, checkpoint)
                .orElseGet(Mastery::new);

        m.setUser(user);
        m.setCheckpoint(checkpoint);

        double old = (m.getUpdatedAt() == null) ? 0.10 : m.getMasteryValue();

        double delta = (score - 0.60) * 0.20;
        double next = clamp(old + delta, 0.0, 1.0);

        m.setMasteryValue(next);
        m.setUpdatedAt(Instant.now());
        masteryRepository.save(m);

        return next;
    }

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    // ---------------------------------------------------------
    // BOOTSTRAP mastery from DiagnosticResult (first time only)
    // ---------------------------------------------------------
    private void ensureMasteryBootstrapped(User user) {
        if (!masteryRepository.findByUser(user).isEmpty())
            return;

        DiagnosticResult dr = diagnosticRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Diagnostic not completed"));

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

    @Transactional
    public AllLessonsResponse getAllLessons(User user) {
        ensureMasteryBootstrapped(user);

        // compute userLevel from mastery avg
        Map<Checkpoint, Double> mastery = masteryRepository.findByUser(user)
                .stream()
                .collect(Collectors.toMap(Mastery::getCheckpoint, Mastery::getMasteryValue));

        double avg = mastery.values().stream().mapToDouble(d -> d).average().orElse(0.0);
        String userLevel = (avg >= 0.75) ? "Advanced" : (avg >= 0.45) ? "Intermediate" : "Beginner";

        int userMaxDifficulty = userLevel.equals("Beginner") ? 1 : userLevel.equals("Intermediate") ? 2 : 3;

        Map<UUID, LessonProgress> progressMap = progressRepository.findByUser(user).stream()
                .collect(Collectors.toMap(p -> p.getLesson().getId(), p -> p));

        List<Lesson> lessons = lessonRepository.findByActiveTrueOrderByCheckpointAscOrderIndexAsc();

        var out = lessons.stream().map(l -> {
            LessonProgress p = progressMap.get(l.getId());
            String status = (p == null) ? "not_started" : p.getStatus().name().toLowerCase();

            int masteryPercent = (int) Math.round(mastery.getOrDefault(l.getCheckpoint(), 0.0) * 100.0);

            boolean locked = l.getDifficulty() > userMaxDifficulty;

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
                    masteryPercent,
                    locked);
        }).toList();

        return new AllLessonsResponse(userLevel, out);
    }
}