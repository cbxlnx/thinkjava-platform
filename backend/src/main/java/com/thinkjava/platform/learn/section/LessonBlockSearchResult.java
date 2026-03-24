package com.thinkjava.platform.learn.section;

import java.util.UUID;

public interface LessonBlockSearchResult {
    UUID getId();
    UUID getLessonId();
    Integer getSectionOrder();
    String getType();
    Double getSimilarity();
}