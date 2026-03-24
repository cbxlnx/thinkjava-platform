package com.thinkjava.platform.learn.tutor;

import com.thinkjava.platform.learn.section.LessonBlock;
import com.thinkjava.platform.learn.section.LessonBlockRepository;
import com.thinkjava.platform.learn.section.LessonBlockSearchResult;
import com.thinkjava.platform.learn.section.LessonSemanticSearchService;
import com.thinkjava.platform.openai.OpenAiChatService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TutorService {

    private final LessonSemanticSearchService semanticSearchService;
    private final LessonBlockRepository blockRepository;
    private final OpenAiChatService openAiChatService;

    public TutorService(
            LessonSemanticSearchService semanticSearchService,
            LessonBlockRepository blockRepository,
            OpenAiChatService openAiChatService
    ) {
        this.semanticSearchService = semanticSearchService;
        this.blockRepository = blockRepository;
        this.openAiChatService = openAiChatService;
    }

    public TutorAskResponse searchRelevantSections(UUID lessonId, String question) {

        List<LessonBlockSearchResult> results =
                semanticSearchService.findRelevantSections(lessonId, question);

        List<LessonBlock> blocks = results.stream()
                .map(r -> blockRepository.findById(r.getId()).orElse(null))
                .filter(b -> b != null)
                .toList();

        String context = blocks.stream()
                .map(b -> {
                    StringBuilder sb = new StringBuilder();

                    if (b.getMarkdown() != null && !b.getMarkdown().isBlank()) {
                        sb.append(b.getMarkdown()).append("\n\n");
                    }

                    if (b.getEmbeddingText() != null && !b.getEmbeddingText().isBlank()) {
                        sb.append(b.getEmbeddingText());
                    }

                    return sb.toString().trim();
                })
                .filter(s -> !s.isBlank())
                .collect(Collectors.joining("\n\n---\n\n"));

        String answer = openAiChatService.askTutor(question, context);

        List<TutorSearchResultDto> matches = results.stream()
                .map(r -> new TutorSearchResultDto(
                        r.getId(),
                        r.getLessonId(),
                        r.getSectionOrder(),
                        r.getType(),
                        r.getSimilarity()
                ))
                .toList();

        return new TutorAskResponse(question, answer, matches);
    }
}