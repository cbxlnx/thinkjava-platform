package com.thinkjava.platform.learn.dto;

import java.util.Map;
import java.util.UUID;

public record LessonQuizSubmitRequest(Map<UUID, String> answers) {}