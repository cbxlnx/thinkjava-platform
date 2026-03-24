package com.thinkjava.platform.learn.tutor;

import java.util.UUID;

public record TutorAskRequest(
        UUID lessonId,
        String question
) {}