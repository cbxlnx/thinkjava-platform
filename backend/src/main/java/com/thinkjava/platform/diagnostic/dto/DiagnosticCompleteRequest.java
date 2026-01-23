package com.thinkjava.platform.diagnostic.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class DiagnosticCompleteRequest {
  private String fundamentals; // Strong/Medium/Weak/Unknown
  private String loops;
  private String arrays;
  private String methods;
  private String oop;
  private String startModule;  // fundamentals|loops|arrays|methods|oop|advanced
}