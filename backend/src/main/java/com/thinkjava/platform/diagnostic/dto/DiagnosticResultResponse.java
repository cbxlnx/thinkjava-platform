package com.thinkjava.platform.diagnostic.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class DiagnosticResultResponse {
  private String fundamentals;
  private String loops;
  private String arrays;
  private String methods;
  private String oop;
  private String startModule;
  private Instant completedAt;
}