package com.cusina.ai.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MealSuggestion {

    private String name;
    private String description;
    private List<String> steps;
    private List<String> usedIngredients;

    public boolean isValid() {
        if (name == null || name.isBlank()) {
            return false;
        }
        if (description == null || description.isBlank()) {
            return false;
        }
        if (steps == null || steps.isEmpty()) {
            return false;
        }
        return steps.stream().noneMatch(step -> step == null || step.isBlank());
    }

    public boolean usesValidIngredientSubset(List<String> availableIngredients) {
        if (usedIngredients == null || usedIngredients.isEmpty() || availableIngredients == null || availableIngredients.isEmpty()) {
            return false;
        }
        Set<String> allowed = new HashSet<>(availableIngredients.stream()
                .map(MealSuggestion::normalizeIngredientToken)
                .filter(value -> !value.isBlank())
                .toList());

        if (allowed.isEmpty()) {
            return false;
        }

        List<String> normalizedUsed = usedIngredients.stream()
                .map(MealSuggestion::normalizeIngredientToken)
                .filter(value -> !value.isBlank())
                .toList();

        return !normalizedUsed.isEmpty() && normalizedUsed.stream().allMatch(used -> matchesAnyAllowed(used, allowed));
    }

    private static boolean matchesAnyAllowed(String used, Set<String> allowed) {
        if (allowed.contains(used)) {
            return true;
        }
        // Toleruje drobne różnice formatu, np. "jajko (6 szt)" vs "jajko".
        return allowed.stream().anyMatch(candidate ->
                (used.length() >= 4 && candidate.contains(used))
                        || (candidate.length() >= 4 && used.contains(candidate))
        );
    }

    private static String normalizeIngredientToken(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        normalized = normalized.replaceAll("\\([^)]*\\)", " ");
        normalized = normalized.replaceAll("[^\\p{L}\\p{Nd}\\s-]", " ");
        normalized = normalized.replaceAll("\\s+", " ").trim();

        // Ujednolicenie znaków (bez zmiany polskich liter na ASCII).
        return Normalizer.normalize(normalized, Normalizer.Form.NFC);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getSteps() {
        return steps;
    }

    public void setSteps(List<String> steps) {
        this.steps = steps;
    }

    public List<String> getUsedIngredients() {
        return usedIngredients;
    }

    public void setUsedIngredients(List<String> usedIngredients) {
        this.usedIngredients = usedIngredients;
    }
}

