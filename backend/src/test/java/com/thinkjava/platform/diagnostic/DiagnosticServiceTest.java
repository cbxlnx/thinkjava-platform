package com.thinkjava.platform.diagnostic;

import com.thinkjava.platform.diagnostic.dto.DiagnosticCompleteRequest;
import com.thinkjava.platform.diagnostic.dto.DiagnosticResultResponse;
import com.thinkjava.platform.diagnostic.result.DiagnosticResult;
import com.thinkjava.platform.diagnostic.result.DiagnosticResultRepository;
import com.thinkjava.platform.learn.LearnService;
import com.thinkjava.platform.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiagnosticServiceTest {

    // mocks the repository used to read and save diagnostic results
    @Mock
    private DiagnosticResultRepository repository;

    // mocks the learning service so we can verify mastery reset behavior
    @Mock
    private LearnService learnService;

    private DiagnosticService diagnosticService;
    private User user;

    @BeforeEach
    void setUp() {
        // creates the service under test with mocked dependencies
        diagnosticService = new DiagnosticService(repository, learnService);

        // creates a reusable test user for all test cases
        user = new User();
        user.setId(1L);
        user.setEmail("student@example.com");
    }

    @Test
    void hasCompleted_returnsTrueWhenResultExists() {
        // simulates an existing diagnostic result for the user
        when(repository.findByUserId(user.getId())).thenReturn(Optional.of(new DiagnosticResult()));

        // verifies the service reports the diagnostic as completed
        assertTrue(diagnosticService.hasCompleted(user));
    }

    @Test
    void hasCompleted_returnsFalseWhenResultDoesNotExist() {
        // simulates no diagnostic result for the user
        when(repository.findByUserId(user.getId())).thenReturn(Optional.empty());

        // verifies the service reports the diagnostic as incomplete
        assertFalse(diagnosticService.hasCompleted(user));
    }

    @Test
    void getResult_throwsNotFoundWhenDiagnosticHasNotBeenCompleted() {
        // simulates a missing saved diagnostic result
        when(repository.findByUserId(user.getId())).thenReturn(Optional.empty());

        // verifies the service throws a 404-style exception
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> diagnosticService.getResult(user));

        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void complete_savesResultAndResetsMasteryFromDiagnostic() {
        // builds a realistic diagnostic submission payload
        DiagnosticCompleteRequest request = new DiagnosticCompleteRequest();
        request.setFundamentals("Strong");
        request.setLoops("Medium");
        request.setArrays("Weak");
        request.setMethods("Medium");
        request.setOop("Unknown");
        request.setStartModule("arrays");
        request.setDiagnosticPercent(58);

        // simulates first-time completion with no previous saved result
        when(repository.findByUserId(user.getId())).thenReturn(Optional.empty());

        // returns the same saved entity so assertions can inspect it
        when(repository.save(org.mockito.ArgumentMatchers.any(DiagnosticResult.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // executes the diagnostic completion flow
        DiagnosticResultResponse response = diagnosticService.complete(user, request);

        // captures the saved entity to verify each persisted field
        ArgumentCaptor<DiagnosticResult> captor = ArgumentCaptor.forClass(DiagnosticResult.class);
        verify(repository).save(captor.capture());

        // verifies the learning system is told to reset mastery from the new diagnostic
        verify(learnService).resetMasteryFromDiagnostic(user);

        DiagnosticResult saved = captor.getValue();
        assertEquals(user, saved.getUser());
        assertEquals("Strong", saved.getFundamentals());
        assertEquals("Medium", saved.getLoops());
        assertEquals("Weak", saved.getArrays());
        assertEquals("Medium", saved.getMethods());
        assertEquals("Unknown", saved.getOop());
        assertEquals("arrays", saved.getStartModule());
        assertEquals(58, saved.getDiagnosticPercent());
        assertNotNull(saved.getCompletedAt());

        assertEquals("arrays", response.getStartModule());
        assertEquals(58, response.getDiagnosticPercent());
    }
}
