package com.thinkjava.platform.diagnostic;

import com.thinkjava.platform.diagnostic.dto.DiagnosticCompleteRequest;
import com.thinkjava.platform.diagnostic.dto.DiagnosticResultResponse;
import com.thinkjava.platform.diagnostic.dto.DiagnosticStatusResponse;
import com.thinkjava.platform.user.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

//responsible for handling all HTTP requests related to the diagnostic quiz
@RestController
@RequestMapping("/api/diagnostic")
@CrossOrigin(origins = "http://localhost:4200")
public class DiagnosticController {

  private final DiagnosticService diagnosticService;

  public DiagnosticController(DiagnosticService diagnosticService) {
    this.diagnosticService = diagnosticService;
  }

  //check if the diagnostic is completed, to decide whether to show the quiz or skip to lessons
  @GetMapping("/status")
  public DiagnosticStatusResponse status(@AuthenticationPrincipal User user) {
    boolean completed = diagnosticService.hasCompleted(user);
    return completed
        ? new DiagnosticStatusResponse(false, "COMPLETED")
        : new DiagnosticStatusResponse(true, "NOT_STARTED");
  }

  //submit diagnostic answers
  @PostMapping("/complete")

  //this endpoint finalizes the diagnostic and stores the results.
  public DiagnosticResultResponse complete(
      @AuthenticationPrincipal User user,
      @RequestBody DiagnosticCompleteRequest request
  ) {
    return diagnosticService.complete(user, request);
  }

  //retrieve stored diagnostic results(used when the user refreshes the page or logs back in later)
  @GetMapping("/result")
  public DiagnosticResultResponse result(@AuthenticationPrincipal User user) {
    return diagnosticService.getResult(user);
  }
}

