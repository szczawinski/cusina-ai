package com.cusina.ai.service;

import com.cusina.ai.config.AnthropicProperties;
import com.cusina.ai.model.MealRequest;
import com.cusina.ai.model.MealResponse;
import com.cusina.ai.model.MealSuggestion;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MealSuggestionService {
    private static final Logger logger = LoggerFactory.getLogger(MealSuggestionService.class);

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
        long startTime = System.currentTimeMillis();
        try {
            String systemPrompt = buildSystemPrompt();
            String userPrompt = buildUserPrompt(request);
            
            long beforeAiCall = System.currentTimeMillis();
            String rawPayload = mealAiClient.requestMealSuggestionsJson(systemPrompt, userPrompt);
            long afterAiCall = System.currentTimeMillis();
            logger.info("Anthropic API response time: {} ms", (afterAiCall - beforeAiCall));
            
            MealResponse rawResponse = parseResponse(rawPayload);
            long endTime = System.currentTimeMillis();
            logger.info("Total meal suggestion processing time: {} ms", (endTime - startTime));
            
            return CompletableFuture.completedFuture(validateAndFilter(rawResponse, request.getIngredients()));
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            long endTime = System.currentTimeMillis();
            logger.error("JSON processing error after {} ms", (endTime - startTime), ex);
            return CompletableFuture.completedFuture(MealResponse.error(PARSE_ERROR));
        } catch (Exception ex) {
            long endTime = System.currentTimeMillis();
            logger.error("AI request error after {} ms", (endTime - startTime), ex);
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

        for (int index = 0; index < rawResponse.getMeals().size(); index++) {
            MealSuggestion meal = rawResponse.getMeals().get(index);
            if (meal != null && meal.isValid() && meal.usesValidIngredientSubset(availableIngredients)) {
                if (!isLikelyPolish(meal)) {
                    return MealResponse.error(LANGUAGE_ERROR);
                }
                validMeals.add(meal);
            } else {
                logger.warn("Pominięto sugestię AI #{}: {}", index + 1, describeRejectionReason(meal, availableIngredients));
                omitted++;
            }
        }

        MealResponse response = new MealResponse();
        response.setRawCount(rawResponse.getRawCount());
        response.setMeals(validMeals);
        response.setOmittedMalformedCount(omitted);
        if (omitted > 0) {
            response.setWarningPl("Część sugestii pominięto, ponieważ miały niepełne dane lub wskazywały składniki spoza Twojej listy.");
            logger.info("Walidacja sugestii AI zakończona: {} poprawnych, {} pominiętych.", validMeals.size(), omitted);
        }
        return response;
    }

    private String describeRejectionReason(MealSuggestion meal, List<String> availableIngredients) {
        if (meal == null) {
            return "brak obiektu sugestii";
        }
        if (!meal.isValid()) {
            return "brak wymaganych danych (name/description/steps)";
        }
        if (!meal.usesValidIngredientSubset(availableIngredients)) {
            return "użyto składników spoza listy użytkownika lub nie udało się ich dopasować";
        }
        return "nieznany powód odrzucenia";
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
        return "Jesteś szefem kuchni. Odpowiedz WYŁĄCZNIE poprawnym JSON w formacie {\"meals\":[{\"name\":\"string\",\"description\":\"string\",\"steps\":[\"string\"],\"usedIngredients\":[\"string\"]}]}. Zwróć dokładnie 3 sugestie, używaj wyłącznie języka polskiego, respektuj dostępne ilości/jednostki i wskazuj tylko składniki przekazane przez użytkownika.";
    }

    String buildUserPrompt(MealRequest request) {
        String ingredientContext = (request.getIngredientDetails() == null || request.getIngredientDetails().isEmpty())
                ? String.join(", ", request.getIngredients())
                : String.join(", ", request.getIngredientDetails());
        String preferences = (request.getDietaryPreferences() == null || request.getDietaryPreferences().isBlank()) ? "brak" : request.getDietaryPreferences().trim();
        String dishType = request.getDishType() == null ? "brak" : request.getDishType().value();
        String dietType = request.getDietType() == null ? "brak" : request.getDietType().value();
        return "Dostępne składniki (ilość + jednostka): " + ingredientContext
                + ". Preferencje żywieniowe: " + preferences
                + ". Typ dania (constraint): " + dishType
                + ". Typ diety (constraint): " + dietType
                + ". Każda sugestia ma użyć niepustego podzbioru dostępnych składników i zawierać pole usedIngredients. Zaproponuj dokładnie 3 praktyczne dania.";
    }
}

