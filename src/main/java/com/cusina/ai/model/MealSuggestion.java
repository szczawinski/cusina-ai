package com.cusina.ai.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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
                .map(value -> value == null ? "" : value.trim().toLowerCase(Locale.ROOT))
                .filter(value -> !value.isBlank())
                .toList());

        if (allowed.isEmpty()) {
            return false;
        }

        List<String> normalizedUsed = usedIngredients.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .toList();

        return !normalizedUsed.isEmpty() && normalizedUsed.stream().allMatch(allowed::contains);
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

