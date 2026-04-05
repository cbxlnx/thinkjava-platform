package com.thinkjava.platform;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thinkjava.platform.auth.AuthController;
import com.thinkjava.platform.auth.JwtService;
import com.thinkjava.platform.common.ApiExceptionHandler;
import com.thinkjava.platform.dto.LoginRequest;
import com.thinkjava.platform.dto.RegisterRequest;
import com.thinkjava.platform.user.EmailAlreadyUsedException;
import com.thinkjava.platform.user.User;
import com.thinkjava.platform.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {
    // Mocks for dependencies
    @Mock
    private UserService userService;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authManager;

    private MockMvc mvc;
    private ObjectMapper om;

    @BeforeEach
    void setUp() {
        AuthController controller = new AuthController(userService, jwtService, authManager);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .setValidator(validator)
                .build();

        om = new ObjectMapper();
    }

    @Test
    @DisplayName("Register should create a new user and return JWT")
    void register_success() throws Exception {
        User user = new User();
        user.setEmail("register@thinkjava.dev");

        RegisterRequest request = new RegisterRequest();
        request.setEmail("register@thinkjava.dev");
        request.setPassword("Password123!");

        when(userService.create("register@thinkjava.dev", "Password123!")).thenReturn(user);
        when(jwtService.generate("register@thinkjava.dev")).thenReturn("jwt-token");

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    @DisplayName("Register should fail when email already exists")
    void register_duplicateEmail_returnsConflict() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("duplicate@thinkjava.dev");
        request.setPassword("Password123!");

        when(userService.create("duplicate@thinkjava.dev", "Password123!"))
                .thenThrow(new EmailAlreadyUsedException());

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("User with this email already exists"));
    }

    @Test
    @DisplayName("Login should return JWT for valid credentials")
    void login_success() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("login@thinkjava.dev");
        request.setPassword("Password123!");

        when(jwtService.generate("login@thinkjava.dev")).thenReturn("jwt-token");

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));

        verify(authManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("Login should fail for wrong password")
    void login_wrongPassword_returnsUnauthorized() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("login@thinkjava.dev");
        request.setPassword("wrong-password");

        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid email or password"));

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(containsString("Invalid email or password")));
    }

    @Test
    @DisplayName("Register should fail when email is invalid")
    void register_invalidEmail_returnsBadRequest() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email": "not-an-email",
                              "password": "Password123!"
                            }
                            """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Register should fail when password is blank")
    void register_blankPassword_returnsBadRequest() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email": "test@thinkjava.dev",
                              "password": ""
                            }
                            """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Login should fail when request fields are missing")
    void login_missingFields_returnsBadRequest() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email": "",
                              "password": ""
                            }
                            """))
                .andExpect(status().isBadRequest());
    }
}
