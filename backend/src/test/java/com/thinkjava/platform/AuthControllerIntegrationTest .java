package com.thinkjava.platform;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thinkjava.platform.dto.LoginRequest;
import com.thinkjava.platform.dto.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper om;

    @Test
    @DisplayName("Register should create a new user and return JWT")
    void register_success() throws Exception {
        String email = "register_" + System.currentTimeMillis() + "@thinkjava.dev";
        String password = "Password123!";

        RegisterRequest request = new RegisterRequest();
        request.setEmail(email);
        request.setPassword(password);

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(request)))
                .andExpect(status().isCreated()) // 201 Created
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.token", not("")));

    }

    @Test
    @DisplayName("Login should return JWT for valid credentials")
    void login_success() throws Exception {
        String email = "login_" + System.currentTimeMillis() + "@thinkjava.dev";
        String password = "Password123!";

        RegisterRequest register = new RegisterRequest();
        register.setEmail(email);
        register.setPassword(password);

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(register)))
                .andExpect(status().isCreated());

        LoginRequest login = new LoginRequest();
        login.setEmail(email);
        login.setPassword(password);

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.token", not("")));
    }

    @Test
    @DisplayName("Register should fail when email already exists")
    void register_duplicateEmail_returnsConflict() throws Exception {
        String email = "duplicate_" + System.currentTimeMillis() + "@thinkjava.dev";
        String password = "Password123!";

        RegisterRequest request = new RegisterRequest();
        request.setEmail(email);
        request.setPassword(password);

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("Login should fail for wrong password")
    void login_wrongPassword_returnsUnauthorized() throws Exception {
        String email = "wrongpass_" + System.currentTimeMillis() + "@thinkjava.dev";
        String password = "Password123!";
        String wrongPassword = "WrongPassword123!";

        RegisterRequest register = new RegisterRequest();
        register.setEmail(email);
        register.setPassword(password);

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(register)))
                .andExpect(status().isCreated());

        LoginRequest login = new LoginRequest();
        login.setEmail(email);
        login.setPassword(wrongPassword);

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(login)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(containsString("Invalid")));
    }

    @Test
    @DisplayName("Register should fail when email is invalid")
    void register_invalidEmail_returnsBadRequest() throws Exception {
        String body = """
            {
              "email": "not-an-email",
              "password": "Password123!"
            }
            """;

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Register should fail when password is blank")
    void register_blankPassword_returnsBadRequest() throws Exception {
        String body = """
            {
              "email": "test@thinkjava.dev",
              "password": ""
            }
            """;

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Login should fail when request fields are missing")
    void login_missingFields_returnsBadRequest() throws Exception {
        String body = """
            {
              "email": "",
              "password": ""
            }
            """;

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}