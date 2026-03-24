package com.thinkjava.platform.openai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class OpenAiChatService {

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.chat.model:gpt-4o-mini}")
    private String chatModel;

    private final RestTemplate restTemplate = new RestTemplate();

    public String askTutor(String question, String context) {
        String systemPrompt = """
                You are a helpful Java tutor.

                Use the provided lesson context as your primary source of truth.
                Stay focused on the current lesson topic.
                Give clear, beginner-friendly explanations.

                You may create simple original examples if they are consistent with the lesson topic and help the student understand.
                Do not introduce unrelated concepts or switch to a different lesson topic.

                If the student asks something outside the lesson topic or the available context, say that briefly and answer only at a high level.
                """;

        String inputText = systemPrompt
                + "\n\nLesson Context:\n" + context
                + "\n\nStudent Question:\n" + question;

        Map<String, Object> requestBody = Map.of(
                "model", chatModel,
                "input", inputText
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey.trim());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                "https://api.openai.com/v1/responses",
                HttpMethod.POST,
                entity,
                Map.class
        );

        Map<?, ?> body = response.getBody();
        if (body == null) {
            return "Sorry, I could not generate an answer.";
        }

        Object output = body.get("output");
        if (output instanceof List<?> outputList && !outputList.isEmpty()) {
            Object first = outputList.get(0);
            if (first instanceof Map<?, ?> firstMap) {
                Object content = firstMap.get("content");
                if (content instanceof List<?> contentList && !contentList.isEmpty()) {
                    Object firstContent = contentList.get(0);
                    if (firstContent instanceof Map<?, ?> contentMap) {
                        Object text = contentMap.get("text");
                        if (text != null) {
                            return text.toString();
                        }
                    }
                }
            }
        }

        Object outputText = body.get("output_text");
        if (outputText != null) {
            return outputText.toString();
        }

        return "Sorry, I could not generate an answer.";
    }
}