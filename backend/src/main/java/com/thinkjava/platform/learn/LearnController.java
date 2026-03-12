package com.thinkjava.platform.learn;

import com.thinkjava.platform.learn.dto.AllLessonsResponse;
import com.thinkjava.platform.learn.dto.LearnPathResponse;
import com.thinkjava.platform.learn.dto.LearnRecommendationsResponse;
import com.thinkjava.platform.learn.dto.LessonQuizSubmitRequest;
import com.thinkjava.platform.learn.dto.LessonQuizSubmitResponse;
import com.thinkjava.platform.learn.dto.LessonResponse;
import com.thinkjava.platform.learn.dto.LessonSummaryResponse;
import com.thinkjava.platform.user.User;
import com.thinkjava.platform.user.UserService;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

// Controller class for handling learning-related API endpoints
@RestController
@RequestMapping("/api/learn")
@CrossOrigin(origins = "http://localhost:4200")
public class LearnController {

  private final LearnService learnService;
  private final UserService userService;

  public LearnController(LearnService learnService, UserService userService) {
    this.learnService = learnService;
    this.userService = userService;
  }

  @GetMapping("/path")
  public LearnPathResponse path(@AuthenticationPrincipal User user) {
    return learnService.getPath(user);
  }

  @GetMapping("/lesson/{id}")
  public LessonResponse lesson(@AuthenticationPrincipal User user, @PathVariable UUID id) {
    return learnService.getLesson(user, id);
  }

  @PostMapping("/lesson/{id}/quiz/submit")
  public LessonQuizSubmitResponse submitQuiz(
      @AuthenticationPrincipal User user,
      @PathVariable UUID id,
      @RequestBody LessonQuizSubmitRequest req) {
    return learnService.submitQuiz(user, id, req);
  }

  @GetMapping("/lessons")
  public AllLessonsResponse allLessons(@AuthenticationPrincipal(expression = "username") String email) {
    if (email == null)
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing/invalid token");
    User user = userService.findByEmail(email).orElseThrow();
    return learnService.getAllLessons(user);
  }

  @GetMapping("/recommendations")
  public LearnRecommendationsResponse recommendations(
      @AuthenticationPrincipal(expression = "username") String email) {
    if (email == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing/invalid token");
    }
    User user = userService.findByEmail(email).orElseThrow();
    return learnService.getRecommendations(user);
  }

  @GetMapping("/current-focus")
  public LessonSummaryResponse currentFocus(@AuthenticationPrincipal User user) {
    if (user == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing user");
    }
    return learnService.getCurrentFocus(user);
  }

  @PostMapping("/debug/recompute-mastery")
  public void recomputeMastery(@AuthenticationPrincipal User user) {
    if (user == null) {
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing user");
    }
    learnService.recomputeAllCheckpointMastery(user);
}

}