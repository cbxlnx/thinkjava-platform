package com.thinkjava.platform.learn.section;

import java.util.UUID;
// Interface to represent the search results for lesson blocks, 
// containing relevant information for each block that matches the search query
public interface LessonBlockSearchResult {
    UUID getId();
    UUID getLessonId();
    Integer getSectionOrder();
    String getType();
    Double getSimilarity();
}