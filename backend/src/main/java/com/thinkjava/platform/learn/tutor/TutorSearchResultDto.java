package com.thinkjava.platform.learn.tutor;

import java.util.UUID;

public record TutorSearchResultDto(
        UUID blockId,
        UUID lessonId,
        Integer sectionOrder,
        String type,
        Double similarity
) {}