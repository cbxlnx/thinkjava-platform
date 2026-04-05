package com.thinkjava.platform.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thinkjava.platform.common.ApiExceptionHandler;
import com.thinkjava.platform.user.dto.UpdateNameRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    // mocks the user service used by the controller
    @Mock
    private UserService userService;

    private MockMvc mvc;
    private ObjectMapper om;

    @BeforeEach
    void setUp() {
        // creates the controller with mocked dependencies
        UserController controller = new UserController(userService);

        // enables validation for request body tests
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        // builds standalone mockmvc for fast controller-level testing
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .setValidator(validator)
                .build();

        // used to serialize request bodies to json
        om = new ObjectMapper();
    }

    @Test
    void me_returnsCurrentUserProfile() throws Exception {
        // creates a fake user returned by the service
        User user = new User();
        user.setEmail("student@example.com");
        user.setFirstName("Kate");

        // simulates finding the authenticated user by email
        when(userService.findByEmail("student@example.com")).thenReturn(Optional.of(user));

        // verifies the endpoint returns the expected profile fields
        mvc.perform(get("/api/users/me")
                        .principal(new TestingAuthenticationToken("student@example.com", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("student@example.com"))
                .andExpect(jsonPath("$.firstName").value("Kate"));
    }

    @Test
    void updateName_trimsAndPersistsName() throws Exception {
        // creates an existing user whose first name will be updated
        User user = new User();
        user.setEmail("student@example.com");
        user.setFirstName("Old");

        // simulates loading and saving the user
        when(userService.findByEmail("student@example.com")).thenReturn(Optional.of(user));
        when(userService.save(user)).thenReturn(user);

        // verifies the controller trims whitespace and returns the updated name
        mvc.perform(patch("/api/users/me/name")
                        .principal(new TestingAuthenticationToken("student@example.com", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(new UpdateNameRequest("  Kate  "))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Kate"));

        // verifies the updated user was persisted
        verify(userService).save(user);
    }

    @Test
    void updateName_rejectsBlankName() throws Exception {
        // verifies validation rejects blank first names
        mvc.perform(patch("/api/users/me/name")
                        .principal(new TestingAuthenticationToken("student@example.com", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"firstName":"   "}
                            """))
                .andExpect(status().isBadRequest());
    }
}
