package com.cusina.ai.session;

import com.cusina.ai.model.Ingredient;
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
    private static final int PRELOAD_COUNT = 10;

    private final transient IngredientPool ingredientPool;
    private final Map<String, Ingredient> ingredientsByKey = new LinkedHashMap<>();
    private boolean initialized;

    public IngredientSession(IngredientPool ingredientPool) {
        this.ingredientPool = ingredientPool;
    }

    public void initializeIfNeeded() {
        if (initialized) {
            return;
        }

        List<String> preloaded = ingredientPool.drawUnique(PRELOAD_COUNT);
        for (String name : preloaded) {
            Ingredient ingredient = Ingredient.preloaded(name);
            ingredientsByKey.putIfAbsent(ingredient.normalizedKey(), ingredient);
        }
        initialized = true;
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
            ingredient = Ingredient.userAdded(rawName);
        } catch (IllegalArgumentException ex) {
            return AddResult.INVALID;
        }
        String key = ingredient.normalizedKey();
        if (ingredientsByKey.containsKey(key)) {
            return AddResult.DUPLICATE;
        }
        ingredientsByKey.put(key, ingredient);
        return AddResult.ADDED;
    }

    public void removeIngredient(String normalizedKey) {
        removeIngredientAndReport(normalizedKey);
    }

    public boolean removeIngredientAndReport(String normalizedKey) {
        if (normalizedKey == null) {
            return false;
        }
        return ingredientsByKey.remove(normalizedKey.trim().toLowerCase()) != null;
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
        ingredientsByKey.clear();
        initialized = false;
    }
}

