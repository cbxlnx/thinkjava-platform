package com.thinkjava.platform.learn;

import com.thinkjava.platform.learn.dto.AllLessonsResponse;
import com.thinkjava.platform.learn.dto.LearnPathResponse;
import com.thinkjava.platform.learn.dto.LessonQuizSubmitRequest;
import com.thinkjava.platform.learn.dto.LessonQuizSubmitResponse;
import com.thinkjava.platform.learn.dto.LessonResponse;
import com.thinkjava.platform.user.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/learn")
@CrossOrigin(origins = "http://localhost:4200")
public class LearnController {

  private final LearnService learnService;

  public LearnController(LearnService learnService) {
    this.learnService = learnService;
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
      @RequestBody LessonQuizSubmitRequest req
  ) {
    return learnService.submitQuiz(user, id, req);
  }

  @GetMapping("/lessons")
    public AllLessonsResponse allLessons(@AuthenticationPrincipal User user) {
    return learnService.getAllLessons(user);
    }

}