package com.cusina.ai.session;

import com.fasterxml.jackson.core.type.TypeReference;
import com.cusina.ai.model.Ingredient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
public class IngredientFileRepository implements IngredientSessionRepository {

    private static final Logger log = LoggerFactory.getLogger(IngredientFileRepository.class);

    private final ObjectMapper objectMapper;
    private final Path filePath;

    @Autowired
    public IngredientFileRepository(ObjectMapper objectMapper,
                                    @Value("${ingredients.persistence.file:data/ingredients.json}") String filePath) {
        this(objectMapper, Path.of(filePath));
    }

    IngredientFileRepository(ObjectMapper objectMapper, Path filePath) {
        this.objectMapper = objectMapper;
        this.filePath = filePath;
    }

    @Override
    public synchronized Optional<PersistedIngredientSession> load(String sessionKey) {
        if (sessionKey == null || sessionKey.isBlank()) {
            return Optional.empty();
        }

        Map<String, PersistedIngredientSession> states = readAllStates();
        PersistedIngredientSession state = states.get(sessionKey);
        if (state == null) {
            return Optional.empty();
        }

        List<Ingredient> ingredients = sanitizeIngredients(state.resolvedIngredients());
        return Optional.of(new PersistedIngredientSession(state.initialized(), ingredients));
    }

    @Override
    public synchronized void save(String sessionKey, PersistedIngredientSession state) {
        if (sessionKey == null || sessionKey.isBlank() || state == null) {
            return;
        }

        try {
            Path parent = filePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            Map<String, PersistedIngredientSession> states = readAllStates();
            states.put(sessionKey, new PersistedIngredientSession(state.initialized(), sanitizeIngredients(state.resolvedIngredients())));
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), states);
        } catch (IOException ex) {
            log.warn("Could not save ingredients to {}.", filePath, ex);
        }
    }

    private Map<String, PersistedIngredientSession> readAllStates() {
        if (!Files.exists(filePath)) {
            return new LinkedHashMap<>();
        }

        try {
            Map<String, PersistedIngredientSession> states = objectMapper.readValue(filePath.toFile(), new TypeReference<>() {
            });
            if (states == null) {
                return new LinkedHashMap<>();
            }
            return new LinkedHashMap<>(states);
        } catch (IOException ex) {
            log.warn("Could not read ingredients map from {}. Starting with an empty map.", filePath, ex);
            return new LinkedHashMap<>();
        }
    }

    private List<Ingredient> sanitizeIngredients(List<Ingredient> ingredients) {
        if (ingredients == null) {
            return List.of();
        }
        return ingredients.stream()
                .filter(Objects::nonNull)
                .map(this::sanitizeIngredient)
                .toList();
    }

    private Ingredient sanitizeIngredient(Ingredient ingredient) {
        IngredientDefaults.Amount defaults = IngredientDefaults.inferForName(ingredient.displayName());
        return new Ingredient(
                ingredient.displayName(),
                ingredient.quantity() == null ? defaults.quantity() : ingredient.quantity(),
                ingredient.unit() == null ? defaults.unit() : ingredient.unit(),
                ingredient.source() == null ? Ingredient.Source.USER_ADDED : ingredient.source()
        );
    }
}
