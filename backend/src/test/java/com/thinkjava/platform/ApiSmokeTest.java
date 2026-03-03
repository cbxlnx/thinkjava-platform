package com.thinkjava.platform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ApiSmokeTest {

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper om;

  @Test
  void full_smoke_flow() throws Exception {
    // 1) register
    String email = "smoke_" + System.currentTimeMillis() + "@thinkjava.dev";
    String password = "Password123!";

    mvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
              {"email":"%s","password":"%s"}
            """.formatted(email, password)))
        .andExpect(status().is2xxSuccessful());

    // 2) login -> token
    String loginRes = mvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
              {"email":"%s","password":"%s"}
            """.formatted(email, password)))
        .andExpect(status().is2xxSuccessful())
        .andReturn().getResponse().getContentAsString();

    JsonNode loginJson = om.readTree(loginRes);
    
    String token = loginJson.path("token").asText();
    if (token.isBlank()) token = loginJson.path("accessToken").asText();

    if (token.isBlank()) {
      throw new IllegalStateException("JWT token not found in login response: " + loginRes);
    }

    String auth = "Bearer " + token;

    // 3) diagnostic complete
    mvc.perform(post("/api/diagnostic/complete")
            .header("Authorization", auth)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
              {
                "startModule": "fundamentals",
                "fundamentals": "Medium",
                "loops": "Weak",
                "arrays": "Unknown",
                "methods": "Strong",
                "oop": "Medium"
              }
            """))
        .andExpect(status().is2xxSuccessful());

    // 4) learn lessons
    String lessonsRes = mvc.perform(get("/api/learn/lessons")
            .header("Authorization", auth))
        .andExpect(status().is2xxSuccessful())
        .andExpect(jsonPath("$.lessons").isArray())
        .andReturn().getResponse().getContentAsString();

    JsonNode lessonsJson = om.readTree(lessonsRes);
    String firstLessonId = lessonsJson.path("lessons").get(0).path("id").asText();

    // 5) get lesson details
    String lessonRes = mvc.perform(get("/api/learn/lesson/{id}", firstLessonId)
            .header("Authorization", auth))
        .andExpect(status().is2xxSuccessful())
        .andExpect(jsonPath("$.lesson.id").value(firstLessonId))
        .andReturn().getResponse().getContentAsString();

    JsonNode lessonJson = om.readTree(lessonRes);

    // If quiz exists, submit answers (this assumes options contain correct answer string)
    var questions = lessonJson.path("quiz").path("questions");
    if (questions.isArray() && questions.size() > 0) {
      StringBuilder answersJson = new StringBuilder();
      answersJson.append("{\"answers\":{");

      for (int i = 0; i < questions.size(); i++) {
        String qId = questions.get(i).path("id").asText();
        // just pick first option; adapt if you store correctness differently
        String opt0 = questions.get(i).path("options").get(0).asText();
        answersJson.append("\"").append(qId).append("\":");
        answersJson.append(om.writeValueAsString(opt0));
        if (i < questions.size() - 1) answersJson.append(",");
      }
      answersJson.append("}}");

      mvc.perform(post("/api/learn/lesson/{id}/quiz/submit", firstLessonId)
              .header("Authorization", auth)
              .contentType(MediaType.APPLICATION_JSON)
              .content(answersJson.toString()))
          .andExpect(status().is2xxSuccessful())
          .andExpect(jsonPath("$.score").exists())
          .andExpect(jsonPath("$.passed").exists());
    }
  }
}