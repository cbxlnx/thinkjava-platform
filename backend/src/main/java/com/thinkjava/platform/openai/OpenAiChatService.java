package com.thinkjava.platform.openai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class OpenAiChatService {

    // api key used for requests to openai
    @Value("${openai.api.key}")
    private String apiKey;

    // chat model used for tutor responses
    @Value("${openai.chat.model:gpt-4o-mini}")
    private String chatModel;

    private final RestTemplate restTemplate = new RestTemplate();

    public String askTutor(String question, String context, String lessonTitle) {
        // keep the tutor grounded in the current lesson
        String systemPrompt = """
                You are a helpful Java tutor.

                The current lesson title is: %s

                Use the provided lesson context as your primary source of truth.
                Answer only in relation to this lesson.
                Do not switch to another Java topic or another lesson.
                If the student asks something outside this lesson, say that briefly and keep the answer high-level.
                Give clear, beginner-friendly explanations.
                """.formatted(lessonTitle);

        // combine lesson context and the student question into one input
        String inputText = systemPrompt
                + "\n\nLesson Context:\n" + context
                + "\n\nStudent Question:\n" + question;

        // send a simple responses api request
        Map<String, Object> requestBody = Map.of(
                "model", chatModel,
                "input", inputText);

        // attach auth and json headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey.trim());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        // make the API call to OpenAI and parse the response to extract the generated answer text,
        // preferring the structured output array when available and falling back to output_text if not
        ResponseEntity<Map> response = restTemplate.exchange(
                "https://api.openai.com/v1/responses",
                HttpMethod.POST,
                entity,
                Map.class);

        Map<?, ?> body = response.getBody();
        if (body == null) {
            return "Sorry, I could not generate an answer.";
        }

        // prefer the structured output array when present
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

        // fall back to output_text for simpler responses
        Object outputText = body.get("output_text");
        if (outputText != null) {
            return outputText.toString();
        }

        // return a safe fallback if no text was found
        return "Sorry, I could not generate an answer.";
    }
}
