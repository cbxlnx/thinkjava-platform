package com.thinkjava.platform;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thinkjava.platform.auth.AuthController;
import com.thinkjava.platform.auth.JwtService;
import com.thinkjava.platform.auth.SecureController;
import com.thinkjava.platform.common.ApiExceptionHandler;
import com.thinkjava.platform.dashboard.ActivityItemResponse;
import com.thinkjava.platform.dashboard.DashboardController;
import com.thinkjava.platform.dashboard.DashboardService;
import com.thinkjava.platform.dashboard.DashboardSummaryResponse;
import com.thinkjava.platform.diagnostic.DiagnosticController;
import com.thinkjava.platform.diagnostic.DiagnosticService;
import com.thinkjava.platform.diagnostic.dto.DiagnosticCompleteRequest;
import com.thinkjava.platform.diagnostic.dto.DiagnosticResultResponse;
import com.thinkjava.platform.diagnostic.dto.DiagnosticStatusResponse;
import com.thinkjava.platform.dto.LoginRequest;
import com.thinkjava.platform.dto.RegisterRequest;
import com.thinkjava.platform.learn.LearnController;
import com.thinkjava.platform.learn.LearnService;
import com.thinkjava.platform.learn.dto.AllLessonsResponse;
import com.thinkjava.platform.learn.dto.LearnPathResponse;
import com.thinkjava.platform.learn.dto.LearnRecommendationsResponse;
import com.thinkjava.platform.learn.dto.LessonQuizSubmitRequest;
import com.thinkjava.platform.learn.dto.LessonQuizSubmitResponse;
import com.thinkjava.platform.learn.dto.LessonResponse;
import com.thinkjava.platform.learn.dto.LessonSummaryResponse;
import com.thinkjava.platform.learn.model.Checkpoint;
import com.thinkjava.platform.learn.model.LessonBlockType;
import com.thinkjava.platform.learn.tutor.TutorAskResponse;
import com.thinkjava.platform.learn.tutor.TutorController;
import com.thinkjava.platform.learn.tutor.TutorSearchResultDto;
import com.thinkjava.platform.learn.tutor.TutorService;
import com.thinkjava.platform.user.User;
import com.thinkjava.platform.user.UserController;
import com.thinkjava.platform.user.UserService;
import com.thinkjava.platform.user.dto.UpdateNameRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.mvc.method.annotation.PrincipalMethodArgumentResolver;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AllEndpointsSmokeTest {

    @Mock
    private UserService userService;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private DiagnosticService diagnosticService;
    @Mock
    private DashboardService dashboardService;
    @Mock
    private LearnService learnService;
    @Mock
    private TutorService tutorService;

    private MockMvc mvc;
    private ObjectMapper om;
    private User authenticatedUser;
    private UUID lessonId;

    // set up the MockMvc instance with all controllers and necessary configuration for testing
    @BeforeEach
    void setUp() {
        AuthController authController = new AuthController(userService, jwtService, authenticationManager);
        UserController userController = new UserController(userService);
        DiagnosticController diagnosticController = new DiagnosticController(diagnosticService);
        DashboardController dashboardController = new DashboardController(dashboardService);
        LearnController learnController = new LearnController(learnService, userService);
        TutorController tutorController = new TutorController(tutorService);
        SecureController secureController = new SecureController();

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mvc = MockMvcBuilders.standaloneSetup(
                        new ThinkjavaApplication(),
                        authController,
                        userController,
                        diagnosticController,
                        dashboardController,
                        learnController,
                        tutorController,
                        secureController)
                .setControllerAdvice(new ApiExceptionHandler())
                .setCustomArgumentResolvers(
                        new AuthenticationPrincipalArgumentResolver(),
                        new PrincipalMethodArgumentResolver())
                .setValidator(validator)
                .build();

        om = new ObjectMapper();

        authenticatedUser = new User();
        authenticatedUser.setId(1L);
        authenticatedUser.setEmail("student@example.com");
        authenticatedUser.setFirstName("Kate");
        authenticatedUser.setPassword("encoded");

        lessonId = UUID.randomUUID();
    }
    // clear the security context after each test to prevent authentication state from leaking between tests
    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }
    // test to verify that the /api/ping endpoint is working and returns the expected response
    @Test
    void ping_endpoint_works() throws Exception {
        mvc.perform(get("/api/ping"))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello, ThinkJava!"));
    }
    // test to verify that the /api/auth/register endpoint is working and returns a JWT token upon successful registration
    @Test
    void auth_register_endpoint_works() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("student@example.com");
        request.setPassword("Password123!");

        when(userService.create("student@example.com", "Password123!")).thenReturn(authenticatedUser);
        when(jwtService.generate("student@example.com")).thenReturn("jwt-token");

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void auth_login_endpoint_works() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("student@example.com");
        request.setPassword("Password123!");

        when(jwtService.generate("student@example.com")).thenReturn("jwt-token");

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void users_me_endpoint_works() throws Exception {
        when(userService.findByEmail("student@example.com")).thenReturn(Optional.of(authenticatedUser));

        mvc.perform(get("/api/users/me")
                        .principal(new TestingAuthenticationToken("student@example.com", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("student@example.com"))
                .andExpect(jsonPath("$.firstName").value("Kate"));
    }

    @Test
    void users_update_name_endpoint_works() throws Exception {
        when(userService.findByEmail("student@example.com")).thenReturn(Optional.of(authenticatedUser));
        when(userService.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mvc.perform(patch("/api/users/me/name")
                        .principal(new TestingAuthenticationToken("student@example.com", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(new UpdateNameRequest("NewName"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("NewName"));
    }

    @Test
    void diagnostic_status_endpoint_works() throws Exception {
        authenticateUser();
        when(diagnosticService.hasCompleted(any(User.class))).thenReturn(false);

        mvc.perform(get("/api/diagnostic/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.required").value(true))
                .andExpect(jsonPath("$.status").value("NOT_STARTED"));
    }

    @Test
    void diagnostic_complete_endpoint_works() throws Exception {
        authenticateUser();
        DiagnosticCompleteRequest request = new DiagnosticCompleteRequest();
        request.setFundamentals("Strong");
        request.setLoops("Medium");
        request.setArrays("Weak");
        request.setMethods("Medium");
        request.setOop("Unknown");
        request.setStartModule("arrays");
        request.setDiagnosticPercent(58);

        when(diagnosticService.complete(any(User.class), any(DiagnosticCompleteRequest.class)))
                .thenReturn(new DiagnosticResultResponse(
                        "Strong", "Medium", "Weak", "Medium", "Unknown", "arrays", Instant.now(), 58));

        mvc.perform(post("/api/diagnostic/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startModule").value("arrays"))
                .andExpect(jsonPath("$.diagnosticPercent").value(58));
    }

    @Test
    void diagnostic_result_endpoint_works() throws Exception {
        authenticateUser();
        when(diagnosticService.getResult(any(User.class)))
                .thenReturn(new DiagnosticResultResponse(
                        "Strong", "Medium", "Weak", "Medium", "Unknown", "arrays", Instant.now(), 58));

        mvc.perform(get("/api/diagnostic/result"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fundamentals").value("Strong"))
                .andExpect(jsonPath("$.startModule").value("arrays"));
    }

    @Test
    void dashboard_summary_endpoint_works() throws Exception {
        authenticateUser();
        when(dashboardService.getSummary(any(User.class)))
                .thenReturn(new DashboardSummaryResponse(
                        72,
                        "Intermediate",
                        "1/3",
                        "70%",
                        35,
                        2,
                        lessonSummary("Arrays"),
                        List.of(new ActivityItemResponse("Completed: Arrays", "Recently", "80%")),
                        Map.of("arrays", 80, "loops", 50)));

        mvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.masteryPercent").value(72))
                .andExpect(jsonPath("$.masteryLabel").value("Intermediate"))
                .andExpect(jsonPath("$.topicsCompletedText").value("1/3"));
    }

    @Test
    void learn_path_endpoint_works() throws Exception {
        authenticateUser();
        when(learnService.getPath(any(User.class)))
                .thenReturn(new LearnPathResponse(lessonId, Checkpoint.fundamentals, Map.of(Checkpoint.fundamentals, 0.6)));

        mvc.perform(get("/api/learn/path"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendedLessonId").value(lessonId.toString()))
                .andExpect(jsonPath("$.startCheckpoint").value("fundamentals"));
    }

    @Test
    void learn_lesson_endpoint_works() throws Exception {
        authenticateUser();
        when(learnService.getLesson(any(User.class), org.mockito.ArgumentMatchers.eq(lessonId)))
                .thenReturn(new LessonResponse(
                        new LessonResponse.LessonMeta(lessonId, Checkpoint.fundamentals, "Intro", 1, 15, 1, "Beginner"),
                        List.of(new LessonResponse.BlockDto(
                                1, LessonBlockType.MARKDOWN, "Hello", null, null, null)),
                        new LessonResponse.QuizDto(List.of(
                                new LessonResponse.QuizQuestionDto(UUID.randomUUID(), "Q1", List.of("A", "B"))))));

        mvc.perform(get("/api/learn/lesson/{id}", lessonId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lesson.id").value(lessonId.toString()))
                .andExpect(jsonPath("$.lesson.title").value("Intro"))
                .andExpect(jsonPath("$.blocks[0].type").value("MARKDOWN"));
    }

    @Test
    void learn_quiz_submit_endpoint_works() throws Exception {
        authenticateUser();
        // mock the learnService to return a successful quiz submission response when the submitQuiz method is called with any User, lessonId, and LessonQuizSubmitRequest
        when(learnService.submitQuiz(any(User.class), any(UUID.class), any(LessonQuizSubmitRequest.class)))
                .thenReturn(new LessonQuizSubmitResponse(0.8, true, 0.9, lessonId));

        mvc.perform(post("/api/learn/lesson/{id}/quiz/submit", lessonId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"answers":{"%s":"A"}}
                            """.formatted(UUID.randomUUID())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(0.8))
                .andExpect(jsonPath("$.passed").value(true))
                .andExpect(jsonPath("$.recommendedNextLessonId").value(lessonId.toString()));
    }

    @Test
    void learn_lessons_endpoint_works() throws Exception {
        authenticateUser();
        when(userService.findByEmail("student@example.com")).thenReturn(Optional.of(authenticatedUser));
        when(learnService.getAllLessons(authenticatedUser))
                .thenReturn(new AllLessonsResponse("Intermediate", List.of(lessonSummary("Arrays"))));

        mvc.perform(get("/api/learn/lessons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userLevel").value("Intermediate"))
                .andExpect(jsonPath("$.lessons[0].title").value("Arrays"));
    }

    @Test
    void learn_recommendations_endpoint_works() throws Exception {
        authenticateUser();
        when(userService.findByEmail("student@example.com")).thenReturn(Optional.of(authenticatedUser));
        when(learnService.getRecommendations(authenticatedUser))
                .thenReturn(new LearnRecommendationsResponse(
                        lessonId,
                        Checkpoint.arrays,
                        "Recommended from weakest diagnostic/mastery areas",
                        List.of(Checkpoint.arrays),
                        List.of(lessonSummary("Arrays"))));

        mvc.perform(get("/api/learn/recommendations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryLessonId").value(lessonId.toString()))
                .andExpect(jsonPath("$.primaryCheckpoint").value("arrays"))
                .andExpect(jsonPath("$.recommendedLessons[0].title").value("Arrays"));
    }

    @Test
    void learn_current_focus_endpoint_works() throws Exception {
        authenticateUser();
        when(learnService.getCurrentFocus(any(User.class))).thenReturn(lessonSummary("Arrays"));

        mvc.perform(get("/api/learn/current-focus"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Arrays"))
                .andExpect(jsonPath("$.checkpoint").value("arrays"));
    }

    @Test
    void learn_recompute_mastery_endpoint_works() throws Exception {
        authenticateUser();
        doNothing().when(learnService).recomputeAllCheckpointMastery(any(User.class));

        mvc.perform(post("/api/learn/debug/recompute-mastery"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    void tutor_ask_endpoint_works() throws Exception {
        when(tutorService.searchRelevantSections(any(UUID.class), any(String.class)))
                .thenReturn(new TutorAskResponse(
                        "What is an array?",
                        "An array stores multiple values of the same type.",
                        List.of(new TutorSearchResultDto(
                                UUID.randomUUID(),
                                lessonId,
                                1,
                                "MARKDOWN",
                                0.91))));

        mvc.perform(post("/api/tutor/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"lessonId":"%s","question":"What is an array?"}
                            """.formatted(lessonId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.question").value("What is an array?"))
                .andExpect(jsonPath("$.answer").exists())
                .andExpect(jsonPath("$.matches[0].lessonId").value(lessonId.toString()));
    }

    @Test
    void secure_me_endpoint_works() throws Exception {
        authenticateUser();
        mvc.perform(get("/api/secure/me"))
                .andExpect(status().isOk())
                .andExpect(content().string("You are student@example.com"));
    }

    private void authenticateUser() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(authenticatedUser, null));
    }

    private LessonSummaryResponse lessonSummary(String title) {
        return new LessonSummaryResponse(
                lessonId,
                title,
                Checkpoint.arrays,
                1,
                15,
                2,
                "Intermediate",
                "in_progress",
                50,
                false);
    }
}
