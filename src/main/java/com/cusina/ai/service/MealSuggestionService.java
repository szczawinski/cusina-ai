package com.cusina.ai.service;

import com.cusina.ai.config.AnthropicProperties;
import com.cusina.ai.model.MealResponse;
import com.cusina.ai.model.MealSuggestion;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MealSuggestionService {

    private static final Pattern CODE_FENCE_PATTERN = Pattern.compile("(?s)```(?:json)?\\s*(.*?)\\s*```");
    private static final String PARSE_ERROR = "We couldn't process the AI response. Please retry.";
    private static final String INVALID_COUNT_ERROR = "We couldn't validate the AI response. Please retry.";
    private static final String AI_ERROR = "Unable to reach the AI service. Please try again.";

    private final MealAiClient mealAiClient;
    private final ObjectMapper objectMapper;
    private final AnthropicProperties anthropicProperties;

    public MealSuggestionService(MealAiClient mealAiClient,
                                 ObjectMapper objectMapper,
                                 AnthropicProperties anthropicProperties) {
        this.mealAiClient = mealAiClient;
        this.objectMapper = objectMapper;
        this.anthropicProperties = anthropicProperties;
    }

    @Async("aiTaskExecutor")
    public CompletableFuture<MealResponse> suggest(List<String> ingredients, String dietaryPreferences) {
        try {
            String systemPrompt = buildSystemPrompt();
            String userPrompt = buildUserPrompt(ingredients, dietaryPreferences);
            String rawPayload = mealAiClient.requestMealSuggestionsJson(systemPrompt, userPrompt);
            MealResponse rawResponse = parseResponse(rawPayload);
            return CompletableFuture.completedFuture(validateAndFilter(rawResponse));
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            return CompletableFuture.completedFuture(MealResponse.error(PARSE_ERROR));
        } catch (Exception ex) {
            return CompletableFuture.completedFuture(MealResponse.error(AI_ERROR));
        }
    }

    MealResponse parseResponse(String rawPayload) throws com.fasterxml.jackson.core.JsonProcessingException {
        try {
            return objectMapper.readValue(rawPayload, MealResponse.class);
        } catch (com.fasterxml.jackson.core.JsonProcessingException initial) {
            String normalized = normalizePayload(rawPayload);
            if (normalized.equals(rawPayload == null ? "" : rawPayload.trim())) {
                throw initial;
            }
            return objectMapper.readValue(normalized, MealResponse.class);
        }
    }

    String normalizePayload(String rawPayload) {
        String trimmed = rawPayload == null ? "" : rawPayload.trim();
        Matcher matcher = CODE_FENCE_PATTERN.matcher(trimmed);
        if (matcher.find()) {
            trimmed = matcher.group(1).trim();
        }

        String extracted = extractFirstJsonObject(trimmed);
        return extracted != null ? extracted : trimmed;
    }

    String extractFirstJsonObject(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        int start = text.indexOf('{');
        if (start < 0) {
            return null;
        }

        int depth = 0;
        boolean inString = false;
        boolean escaping = false;

        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (escaping) {
                escaping = false;
                continue;
            }
            if (inString && c == '\\') {
                escaping = true;
                continue;
            }
            if (c == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }

            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return text.substring(start, i + 1);
                }
            }
        }

        return null;
    }

    MealResponse validateAndFilter(MealResponse rawResponse) {
        if (rawResponse == null || rawResponse.getMeals() == null || rawResponse.getMeals().size() != anthropicProperties.getMealCount()) {
            return MealResponse.error(INVALID_COUNT_ERROR);
        }

        List<MealSuggestion> validMeals = new ArrayList<>();
        int omitted = 0;

        for (MealSuggestion meal : rawResponse.getMeals()) {
            if (meal != null && meal.isValid()) {
                validMeals.add(meal);
            } else {
                omitted++;
            }
        }

        MealResponse response = new MealResponse();
        response.setMeals(validMeals);
        response.setOmittedMalformedCount(omitted);
        if (omitted > 0) {
            response.setWarning("Some suggestions were omitted because they were malformed.");
        }
        return response;
    }

    String buildSystemPrompt() {
        return "You are a creative meal planning chef. Respond ONLY with valid JSON matching {\"meals\":[{\"name\":\"string\",\"description\":\"string\",\"steps\":[\"string\"]}]}. Always return exactly 3 suggestions.";
    }

    String buildUserPrompt(List<String> ingredients, String dietaryPreferences) {
        String preferences = (dietaryPreferences == null || dietaryPreferences.isBlank()) ? "none" : dietaryPreferences.trim();
        return "Available ingredients: " + String.join(", ", ingredients) + ". Dietary preferences: " + preferences + ". Suggest exactly 3 practical meals.";
    }
}

