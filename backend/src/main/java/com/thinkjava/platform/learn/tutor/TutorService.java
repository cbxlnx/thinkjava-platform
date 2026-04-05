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
    // Main method to handle a tutor question by searching for relevant lesson sections and generating an answer using OpenAI
    public TutorAskResponse searchRelevantSections(UUID lessonId, String question) {

        List<LessonBlockSearchResult> results =
                semanticSearchService.findRelevantSections(lessonId, question);
        // retrieve the full LessonBlock entities for the search results and filter out any that are not found
        List<LessonBlock> blocks = results.stream()
                .map(r -> blockRepository.findById(r.getId()).orElse(null))
                .filter(b -> b != null)
                .toList();
        // construct the context string for the OpenAI prompt 
        // by concatenating the markdown and embedding text of the relevant lesson blocks, separated by a delimiter
        String context = blocks.stream()
                .map(b -> {
                    StringBuilder sb = new StringBuilder();
                        // include the markdown content of the block if it exists
                    if (b.getMarkdown() != null && !b.getMarkdown().isBlank()) {
                        sb.append(b.getMarkdown()).append("\n\n");
                    }
                         // include the embedding text of the block if it exists        
                    if (b.getEmbeddingText() != null && !b.getEmbeddingText().isBlank()) {
                        sb.append(b.getEmbeddingText());
                    }

                    return sb.toString().trim();
                })
                .filter(s -> !s.isBlank())
                .collect(Collectors.joining("\n\n---\n\n"));
        String lessonTitle = blocks.isEmpty()
                ? "this lesson"
                : blocks.get(0).getLesson().getTitle();

        String answer = openAiChatService.askTutor(question, context, lessonTitle);
                // construct the list of search results to include in the response 
                // mapping each LessonBlockSearchResult to a TutorSearchResultDto
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