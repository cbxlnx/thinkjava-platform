package com.thinkjava.platform.dashboard;

// Simple DTO for recent activity items on the dashboard
public record ActivityItemResponse(
    String title,
    String time, 
    String pill 
) {}