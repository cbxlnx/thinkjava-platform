package com.thinkjava.platform.diagnostic.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DiagnosticStatusResponse {
  private boolean required;
  private String status; // NOT_STARTED | COMPLETED
}
