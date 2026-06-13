package com.cusina.ai.service;

import com.cusina.ai.config.AnthropicProperties;
import com.cusina.ai.model.MealRequest;
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
    private static final String PARSE_ERROR = "Nie udało się przetworzyć odpowiedzi AI. Spróbuj ponownie.";
    private static final String INVALID_COUNT_ERROR = "Odpowiedź AI nie spełnia wymogu dokładnie 3 sugestii. Spróbuj ponownie.";
    private static final String AI_ERROR = "Nie udało się połączyć z usługą AI. Spróbuj ponownie.";
    private static final String LANGUAGE_ERROR = "Odpowiedź AI nie była w języku polskim. Spróbuj ponownie.";

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
    public CompletableFuture<MealResponse> suggest(MealRequest request) {
        try {
            String systemPrompt = buildSystemPrompt();
            String userPrompt = buildUserPrompt(request);
            String rawPayload = mealAiClient.requestMealSuggestionsJson(systemPrompt, userPrompt);
            MealResponse rawResponse = parseResponse(rawPayload);
            return CompletableFuture.completedFuture(validateAndFilter(rawResponse, request.getIngredients()));
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

    MealResponse validateAndFilter(MealResponse rawResponse, List<String> availableIngredients) {
        if (rawResponse == null || rawResponse.getMeals() == null) {
            return MealResponse.error(INVALID_COUNT_ERROR);
        }

        rawResponse.setRawCount(rawResponse.getMeals().size());
        if (rawResponse.getRawCount() != anthropicProperties.getMealCount()) {
            return MealResponse.error(INVALID_COUNT_ERROR);
        }

        List<MealSuggestion> validMeals = new ArrayList<>();
        int omitted = 0;

        for (MealSuggestion meal : rawResponse.getMeals()) {
            if (meal != null && meal.isValid() && meal.usesValidIngredientSubset(availableIngredients)) {
                if (!isLikelyPolish(meal)) {
                    return MealResponse.error(LANGUAGE_ERROR);
                }
                validMeals.add(meal);
            } else {
                omitted++;
            }
        }

        MealResponse response = new MealResponse();
        response.setRawCount(rawResponse.getRawCount());
        response.setMeals(validMeals);
        response.setOmittedMalformedCount(omitted);
        if (omitted > 0) {
            response.setWarningPl("Część sugestii pominięto, ponieważ miały niepełne dane.");
        }
        return response;
    }

    boolean isLikelyPolish(MealSuggestion suggestion) {
        String text = ((suggestion.getName() == null ? "" : suggestion.getName()) + " "
                + (suggestion.getDescription() == null ? "" : suggestion.getDescription()) + " "
                + String.join(" ", suggestion.getSteps() == null ? List.of() : suggestion.getSteps())).toLowerCase();

        boolean hasPolishDiacritics = text.matches(".*[ąćęłńóśźż].*");
        boolean hasPolishTokens = text.contains(" i ") || text.contains(" oraz ") || text.contains(" z ")
                || text.contains("na ") || text.contains("przez ") || text.contains("dodaj");
        return hasPolishDiacritics || hasPolishTokens;
    }

    String buildSystemPrompt() {
        return "Jesteś szefem kuchni. Odpowiedz WYŁĄCZNIE poprawnym JSON w formacie {\"meals\":[{\"name\":\"string\",\"description\":\"string\",\"steps\":[\"string\"],\"usedIngredients\":[\"string\"]}]}. Zwróć dokładnie 3 sugestie, używaj wyłącznie języka polskiego i wskazuj tylko składniki przekazane przez użytkownika.";
    }

    String buildUserPrompt(MealRequest request) {
        String preferences = (request.getDietaryPreferences() == null || request.getDietaryPreferences().isBlank()) ? "brak" : request.getDietaryPreferences().trim();
        String dishType = request.getDishType() == null ? "brak" : request.getDishType().value();
        String dietType = request.getDietType() == null ? "brak" : request.getDietType().value();
        return "Dostępne składniki: " + String.join(", ", request.getIngredients())
                + ". Preferencje żywieniowe: " + preferences
                + ". Typ dania (constraint): " + dishType
                + ". Typ diety (constraint): " + dietType
                + ". Każda sugestia ma użyć niepustego podzbioru dostępnych składników i zawierać pole usedIngredients. Zaproponuj dokładnie 3 praktyczne dania.";
    }
}

