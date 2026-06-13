package com.cusina.ai.session;

import com.cusina.ai.model.Ingredient;
import jakarta.servlet.http.HttpSession;
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
    private final transient IngredientPersistenceService persistenceService;
    private final transient String sessionKey;
    private final Map<String, Ingredient> ingredientsByKey = new LinkedHashMap<>();
    private boolean initialized;
    private boolean stateLoaded;

    public IngredientSession(IngredientPool ingredientPool,
                             IngredientPersistenceService persistenceService,
                             HttpSession httpSession) {
        this(ingredientPool, persistenceService, "session:" + httpSession.getId());
    }

    IngredientSession(IngredientPool ingredientPool,
                      IngredientPersistenceService persistenceService,
                      String sessionKey) {
        this.ingredientPool = ingredientPool;
        this.persistenceService = persistenceService;
        this.sessionKey = sessionKey;
    }

    public void initializeIfNeeded() {
        ensureStateLoaded();
        if (initialized) {
            return;
        }

        List<String> preloaded = ingredientPool.drawUnique(PRELOAD_COUNT);
        for (String name : preloaded) {
            Ingredient ingredient = Ingredient.preloaded(name);
            ingredientsByKey.putIfAbsent(ingredient.normalizedKey(), ingredient);
        }
        initialized = true;
        persist();
    }

    public AddResult addIngredient(String rawName) {
        ensureStateLoaded();
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
        persist();
        return AddResult.ADDED;
    }

    public void removeIngredient(String normalizedKey) {
        removeIngredientAndReport(normalizedKey);
    }

    public boolean removeIngredientAndReport(String normalizedKey) {
        ensureStateLoaded();
        if (normalizedKey == null) {
            return false;
        }
        boolean removed = ingredientsByKey.remove(normalizedKey.trim().toLowerCase()) != null;
        if (removed) {
            persist();
        }
        return removed;
    }

    public List<Ingredient> getIngredients() {
        ensureStateLoaded();
        return List.copyOf(new ArrayList<>(ingredientsByKey.values()));
    }

    public List<String> getIngredientNames() {
        return getIngredients().stream().map(Ingredient::displayName).toList();
    }

    public boolean isEmpty() {
        ensureStateLoaded();
        return ingredientsByKey.isEmpty();
    }

    public boolean isFull() {
        ensureStateLoaded();
        return ingredientsByKey.size() >= MAX_INGREDIENTS;
    }

    public int size() {
        ensureStateLoaded();
        return ingredientsByKey.size();
    }

    public void clear() {
        ensureStateLoaded();
        ingredientsByKey.clear();
        initialized = false;
        persist();
    }

    private void ensureStateLoaded() {
        if (stateLoaded) {
            return;
        }

        persistenceService.load(sessionKey).ifPresent(state -> {
            ingredientsByKey.clear();
            for (String name : state.ingredientNames()) {
                Ingredient ingredient = Ingredient.userAdded(name);
                ingredientsByKey.putIfAbsent(ingredient.normalizedKey(), ingredient);
            }
            initialized = state.initialized();
        });
        stateLoaded = true;
    }

    private void persist() {
        persistenceService.save(sessionKey, new PersistedIngredientSession(initialized, getIngredientNames()));
    }
}

