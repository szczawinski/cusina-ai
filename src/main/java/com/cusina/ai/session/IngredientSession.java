package com.cusina.ai.session;

import com.cusina.ai.model.Ingredient;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@SessionScope
public class IngredientSession implements Serializable {

    public enum AddResult { ADDED, DUPLICATE, FULL, INVALID }

    private static final int MAX_INGREDIENTS = 50;

    private final transient IngredientFileRepository ingredientFileRepository;
    private final Map<String, Ingredient> ingredientsByKey = new LinkedHashMap<>();

    public IngredientSession(IngredientFileRepository ingredientFileRepository) {
        this.ingredientFileRepository = ingredientFileRepository;
        loadFromFile();
    }

    private void loadFromFile() {
        List<String> storedNames = ingredientFileRepository.loadIngredientNames();
        if (storedNames == null) {
            return;
        }

        for (String storedName : storedNames) {
            if (ingredientsByKey.size() >= MAX_INGREDIENTS) {
                break;
            }
            try {
                Ingredient ingredient = new Ingredient(storedName);
                ingredientsByKey.putIfAbsent(ingredient.normalizedKey(), ingredient);
            } catch (IllegalArgumentException ignored) {
                // Skip malformed persisted entries and continue loading the rest.
            }
        }
    }

    private void persistToFile() {
        ingredientFileRepository.saveIngredientNames(getIngredientNames());
    }

    public AddResult addIngredient(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return AddResult.INVALID;
        }
        if (ingredientsByKey.size() >= MAX_INGREDIENTS) {
            return AddResult.FULL;
        }
        Ingredient ingredient;
        try {
            ingredient = new Ingredient(rawName);
        } catch (IllegalArgumentException ex) {
            return AddResult.INVALID;
        }
        String key = ingredient.normalizedKey();
        if (ingredientsByKey.containsKey(key)) {
            return AddResult.DUPLICATE;
        }
        ingredientsByKey.put(key, ingredient);
        persistToFile();
        return AddResult.ADDED;
    }

    public void removeIngredient(String normalizedKey) {
        if (normalizedKey == null) {
            return;
        }
        Ingredient removed = ingredientsByKey.remove(normalizedKey.trim().toLowerCase());
        if (removed != null) {
            persistToFile();
        }
    }

    public List<Ingredient> getIngredients() {
        return List.copyOf(new ArrayList<>(ingredientsByKey.values()));
    }

    public List<String> getIngredientNames() {
        return getIngredients().stream().map(Ingredient::displayName).toList();
    }

    public boolean isEmpty() {
        return ingredientsByKey.isEmpty();
    }

    public boolean isFull() {
        return ingredientsByKey.size() >= MAX_INGREDIENTS;
    }

    public int size() {
        return ingredientsByKey.size();
    }

    public void clear() {
        if (ingredientsByKey.isEmpty()) {
            return;
        }
        ingredientsByKey.clear();
        persistToFile();
    }

    @PreDestroy
    public void onDestroy() {
        persistToFile();
    }
}

