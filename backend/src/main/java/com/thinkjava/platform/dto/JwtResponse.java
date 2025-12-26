package com.thinkjava.platform.dto;

public class JwtResponse {
  private String token;

  public JwtResponse() {}               // for Jackson
  public JwtResponse(String token) {    // for new JwtResponse(token)
    this.token = token;
  }

  public String getToken() {            // Jackson uses getters
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }
}
