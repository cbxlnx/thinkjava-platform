package com.thinkjava.platform.learn.section;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class OpenAiEmbeddingService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.embedding.model:text-embedding-3-small}")
    private String embeddingModel;

    @PostConstruct
public void debugKey() {
    System.out.println("Embedding key present: " + (apiKey != null && !apiKey.isBlank()));
    System.out.println("Embedding key length: " + (apiKey == null ? 0 : apiKey.length()));
    if (apiKey != null) {
        System.out.println("Embedding key prefix: " + apiKey.substring(0, Math.min(7, apiKey.length())));
    }
}

    public String createEmbedding(String text) {
        try {
            String url = "https://api.openai.com/v1/embeddings";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            String requestBody = objectMapper.writeValueAsString(
                    new EmbeddingRequest(embeddingModel, text));

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode embeddingNode = root.path("data").get(0).path("embedding");

            List<Double> values = objectMapper.convertValue(
                    embeddingNode,
                    objectMapper.getTypeFactory()
                            .constructCollectionType(List.class, Double.class));

            List<String> stringValues = values.stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());
            
            System.out.println("Embedding key prefix: " + apiKey.substring(0, 15));

            return "[" + String.join(",", stringValues) + "]";
        } catch (Exception e) {
            throw new RuntimeException("Failed to create embedding", e);
        }
    }

    private record EmbeddingRequest(String model, String input) {
    }
}