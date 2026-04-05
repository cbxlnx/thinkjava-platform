package com.thinkjava.platform;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiSmokeTest {

  private MockMvc mvc;

  @BeforeEach
  void setUp() {
    mvc = MockMvcBuilders.standaloneSetup(new ThinkjavaApplication()).build();
  }

  @Test
  void ping_returnsExpectedResponse() throws Exception {
    mvc.perform(get("/api/ping"))
        .andExpect(status().isOk())
        .andExpect(content().string("Hello, ThinkJava!"));
  }
}
