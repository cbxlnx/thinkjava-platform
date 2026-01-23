package com.thinkjava.platform.diagnostic;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.thinkjava.platform.diagnostic.dto.DiagnosticCompleteRequest;
import com.thinkjava.platform.diagnostic.dto.DiagnosticResultResponse;
import com.thinkjava.platform.diagnostic.result.DiagnosticResult;
import com.thinkjava.platform.diagnostic.result.DiagnosticResultRepository;
import com.thinkjava.platform.user.User;

//stores retrieves and manages the user’s diagnostic mastery profile
@Service
public class DiagnosticService {

  private final DiagnosticResultRepository results;

  public DiagnosticService(DiagnosticResultRepository results) {
    this.results = results;
  }

  //checking if the diagnostic is completed
  public boolean hasCompleted(User user) {
    return results.findByUserId(user.getId()).isPresent();
  }

  //retrieving the diagnostic result
  //used when the frontend wants to display mastery bars or the recommended starting module
  public DiagnosticResultResponse getResult(User user) {
  return results.findByUserId(user.getId())
      .map(r -> new DiagnosticResultResponse(
          r.getFundamentals(),
          r.getLoops(),
          r.getArrays(),
          r.getMethods(),
          r.getOop(),
          r.getStartModule(),
          r.getCompletedAt()
      ))
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Diagnostic not completed"));
}

//saving diagnostic results
@Transactional
public DiagnosticResultResponse complete(User user, DiagnosticCompleteRequest req) {
  DiagnosticResult r = results.findByUserId(user.getId()).orElseGet(DiagnosticResult::new);

  r.setUser(user);
  r.setFundamentals(req.getFundamentals());
  r.setLoops(req.getLoops());
  r.setArrays(req.getArrays());
  r.setMethods(req.getMethods());
  r.setOop(req.getOop());
  r.setStartModule(req.getStartModule());
  r.setCompletedAt(Instant.now());

  DiagnosticResult saved = results.save(r);

  return new DiagnosticResultResponse(
      saved.getFundamentals(),
      saved.getLoops(),
      saved.getArrays(),
      saved.getMethods(),
      saved.getOop(),
      saved.getStartModule(),
      saved.getCompletedAt()
  );
}
}
