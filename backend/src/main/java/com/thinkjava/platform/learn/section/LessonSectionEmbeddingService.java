package com.thinkjava.platform.learn.section;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thinkjava.platform.learn.model.LessonBlockType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class LessonSectionEmbeddingService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LessonBlockRepository blockRepository;
    private final OpenAiEmbeddingService openAiEmbeddingService;

    public LessonSectionEmbeddingService(
            LessonBlockRepository blockRepository,
            OpenAiEmbeddingService openAiEmbeddingService
    ) {
        this.blockRepository = blockRepository;
        this.openAiEmbeddingService = openAiEmbeddingService;
    }
    // build a text representation of the lesson block for embedding
    public String buildEmbeddingText(LessonBlock block) {
        StringBuilder sb = new StringBuilder();
        // start with the section type
        sb.append("Section Type: ").append(block.getType()).append("\n\n");

        // include title if available
        if (block.getType() == LessonBlockType.MARKDOWN && block.getMarkdown() != null) {
            sb.append(block.getMarkdown()).append("\n");
        }
        // for video blocks, include the video title and description if available
        if (block.getType() == LessonBlockType.VIDEO) {
            if (block.getVideoTitle() != null) {
                sb.append("Video Title: ").append(block.getVideoTitle()).append("\n");
            }
            if (block.getMarkdown() != null) {
                sb.append(block.getMarkdown()).append("\n");
            }
        }
        // for quiz blocks, include the question and options
        if (block.getPayload() != null && !block.getPayload().isBlank()) {
            sb.append("\nPayload Text:\n");
            sb.append(extractTextFromJson(block.getPayload()));
        }

        return sb.toString().trim();
    }
    // method to embed all sections of a lesson by generating embedding text and vectors for each section and updating the database
    public void embedAllSectionsForLesson(UUID lessonId) {
        List<LessonBlock> blocks = blockRepository.findByLessonIdOrderByOrderIndexAsc(lessonId);
        // iterate through each block and generate embedding if it doesn't already exist
        for (LessonBlock block : blocks) {
            if (block.getEmbedding() != null && !block.getEmbedding().isBlank()) {
                continue;
            }

            String embeddingText = buildEmbeddingText(block);
            String embeddingVector = openAiEmbeddingService.createEmbedding(embeddingText);

            blockRepository.updateEmbedding(block.getId(), embeddingText, embeddingVector);

            System.out.println("Embedded section " + block.getOrderIndex() + " for lesson " + lessonId);
        }
    }
    // helper method to extract text content from a JSON string, used for quiz payloads
    private String extractTextFromJson(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            StringBuilder sb = new StringBuilder();
            collectText(root, sb);
            return sb.toString().trim();
        } catch (Exception e) {
            return json;
        }
    }
    // recursive method to traverse a JSON node and collect all textual content into a StringBuilder
    private void collectText(JsonNode node, StringBuilder sb) {
        if (node == null) return;

        if (node.isTextual()) {
            sb.append(node.asText()).append("\n");
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                collectText(child, sb);
            }
        } else if (node.isObject()) {
            node.fields().forEachRemaining(entry -> collectText(entry.getValue(), sb));
        }
    }
}