package com.thinkjava.platform.learn.section;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class LessonSemanticSearchService {

    private final LessonBlockRepository blockRepository;
    private final OpenAiEmbeddingService openAiEmbeddingService;

    public LessonSemanticSearchService(
            LessonBlockRepository blockRepository,
            OpenAiEmbeddingService openAiEmbeddingService
    ) {
        this.blockRepository = blockRepository;
        this.openAiEmbeddingService = openAiEmbeddingService;
    }

    // find the most relevant sections for a user question, prioritizing sections
    // from the current lesson
    public List<LessonBlockSearchResult> findRelevantSections(UUID currentLessonId, String userQuestion) {
        String queryEmbedding = openAiEmbeddingService.createEmbedding(userQuestion);
        return blockRepository.findTopSimilarInLesson(currentLessonId, queryEmbedding, 3);
    }
}