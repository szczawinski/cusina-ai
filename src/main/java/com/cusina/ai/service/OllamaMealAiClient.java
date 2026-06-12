package com.cusina.ai.service;

import com.cusina.ai.config.OllamaProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

public class OllamaMealAiClient implements MealAiClient {

    private final OllamaProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public OllamaMealAiClient(OllamaProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .build();
    }

    @Override
    public String requestMealSuggestionsJson(String systemPrompt, String userPrompt) throws Exception {
        String rawResponse = restClient.post()
                .uri("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildRequestBody(systemPrompt, userPrompt))
                .retrieve()
                .body(String.class);

        return extractAssistantContent(rawResponse);
    }

    Map<String, Object> buildRequestBody(String systemPrompt, String userPrompt) {
        return Map.of(
                "model", properties.getModel(),
                "stream", false,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                )
        );
    }

    String extractAssistantContent(String rawResponse) throws com.fasterxml.jackson.core.JsonProcessingException {
        JsonNode root = objectMapper.readTree(rawResponse == null ? "" : rawResponse);

        String chatContent = root.path("message").path("content").asText("");
        if (!chatContent.isBlank()) {
            return chatContent.trim();
        }

        return root.path("response").asText("").trim();
    }
}

