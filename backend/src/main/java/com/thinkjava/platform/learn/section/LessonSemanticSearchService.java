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
    // find the most relevant sections for a user question, prioritizing sections from the current lesson
    public List<LessonBlockSearchResult> findRelevantSections(UUID currentLessonId, String userQuestion) {
        String queryEmbedding = openAiEmbeddingService.createEmbedding(userQuestion);

        List<LessonBlockSearchResult> inLessonResults =
                blockRepository.findTopSimilarInLesson(currentLessonId, queryEmbedding, 3);

        if (!inLessonResults.isEmpty()) {
            Double topSimilarity = inLessonResults.get(0).getSimilarity();

            if (topSimilarity != null && topSimilarity >= 0.55) {
                return inLessonResults;
            }
        }

        return blockRepository.findTopSimilarAcrossAllLessons(queryEmbedding, 3);
    }
}