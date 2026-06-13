package com.cusina.ai.session;

import com.cusina.ai.config.FirestorePersistenceProperties;
import com.cusina.ai.model.Ingredient;
import com.cusina.ai.model.IngredientUnit;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
public class IngredientFirestoreRepository implements IngredientSessionRepository {

    private static final Logger log = LoggerFactory.getLogger(IngredientFirestoreRepository.class);

    private final FirestorePersistenceProperties properties;

    private volatile Firestore firestore;
    private volatile boolean initializationAttempted;

    public IngredientFirestoreRepository(FirestorePersistenceProperties properties) {
        this.properties = properties;
    }

    @Override
    public Optional<PersistedIngredientSession> load(String sessionKey) {
        Firestore client = getFirestore();
        try {
            var snapshot = client.collection(resolveCollection())
                    .document(sessionKey)
                    .get()
                    .get(5, TimeUnit.SECONDS);

            if (!snapshot.exists()) {
                return Optional.empty();
            }

            Boolean initialized = snapshot.getBoolean("initialized");
            List<Ingredient> ingredients = sanitizeIngredients(snapshot.get("ingredients"));
            if (ingredients.isEmpty()) {
                ingredients = sanitizeIngredients(snapshot.get("ingredientNames"));
            }
            return Optional.of(new PersistedIngredientSession(Boolean.TRUE.equals(initialized), ingredients));
        } catch (Exception ex) {
            throw new IllegalStateException("Firestore read failed", ex);
        }
    }

    @Override
    public void save(String sessionKey, PersistedIngredientSession state) {
        Firestore client = getFirestore();
        try {
            Map<String, Object> payload = Map.of(
                    "initialized", state.initialized(),
                    "ingredients", toFirestoreIngredients(state.resolvedIngredients()),
                    "ingredientNames", state.resolvedIngredients().stream().map(Ingredient::displayName).toList(),
                    "updatedAt", FieldValue.serverTimestamp()
            );

            client.collection(resolveCollection())
                    .document(sessionKey)
                    .set(payload)
                    .get(5, TimeUnit.SECONDS);
        } catch (Exception ex) {
            throw new IllegalStateException("Firestore write failed", ex);
        }
    }

    private Firestore getFirestore() {
        if (firestore != null) {
            return firestore;
        }

        synchronized (this) {
            if (firestore != null) {
                return firestore;
            }
            if (initializationAttempted) {
                throw new IllegalStateException("Firestore is unavailable");
            }

            initializationAttempted = true;
            firestore = createFirestoreClient();
            log.info("Firestore ingredient persistence initialized with collection '{}'.", resolveCollection());
            return firestore;
        }
    }

    private Firestore createFirestoreClient() {
        try {
            FirestoreOptions.Builder builder = FirestoreOptions.newBuilder();

            if (hasText(properties.getProjectId())) {
                builder.setProjectId(properties.getProjectId().trim());
            }

            GoogleCredentials credentials = resolveCredentials();
            if (credentials != null) {
                builder.setCredentials(credentials);
            }

            return builder.build().getService();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to initialize Firestore client", ex);
        }
    }

    private GoogleCredentials resolveCredentials() throws IOException {
        if (hasText(properties.getCredentialsPath())) {
            Path path = Path.of(properties.getCredentialsPath().trim());
            if (Files.exists(path)) {
                try (FileInputStream in = new FileInputStream(path.toFile())) {
                    return GoogleCredentials.fromStream(in);
                }
            }
        }
        return GoogleCredentials.getApplicationDefault();
    }

    private String resolveCollection() {
        if (hasText(properties.getCollection())) {
            return properties.getCollection().trim();
        }
        return "ingredientSessions";
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private List<Ingredient> sanitizeIngredients(Object rawValue) {
        if (!(rawValue instanceof List<?> rawList)) {
            return List.of();
        }

        List<Ingredient> ingredients = new ArrayList<>();
        for (Object entry : rawList) {
            if (entry == null) {
                continue;
            }
            if (entry instanceof String name) {
                IngredientDefaults.Amount defaults = IngredientDefaults.inferForName(name);
                ingredients.add(Ingredient.userAdded(name, defaults.quantity(), defaults.unit()));
                continue;
            }
            if (entry instanceof Map<?, ?> mapEntry) {
                Ingredient ingredient = sanitizeIngredientMap(mapEntry);
                if (ingredient != null) {
                    ingredients.add(ingredient);
                }
            }
        }
        return ingredients;
    }

    private Ingredient sanitizeIngredientMap(Map<?, ?> mapEntry) {
        String name = Objects.toString(mapEntry.get("displayName"), "").trim();
        if (name.isBlank()) {
            return null;
        }
        IngredientDefaults.Amount defaults = IngredientDefaults.inferForName(name);
        BigDecimal quantity = parseQuantity(mapEntry.get("quantity"), defaults.quantity());
        IngredientUnit unit = IngredientUnit.fromValue(Objects.toString(mapEntry.get("unit"), null)).orElse(defaults.unit());

        Ingredient.Source source;
        try {
            source = Ingredient.Source.valueOf(Objects.toString(mapEntry.get("source"), "USER_ADDED"));
        } catch (IllegalArgumentException ex) {
            source = Ingredient.Source.USER_ADDED;
        }
        return new Ingredient(name, quantity, unit, source);
    }

    private BigDecimal parseQuantity(Object rawQuantity, BigDecimal fallback) {
        if (rawQuantity == null) {
            return fallback;
        }
        try {
            return new BigDecimal(rawQuantity.toString());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private List<Map<String, Object>> toFirestoreIngredients(List<Ingredient> ingredients) {
        return ingredients.stream().map(ingredient -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("displayName", ingredient.displayName());
            map.put("quantity", ingredient.quantity());
            map.put("unit", ingredient.unit().value());
            map.put("source", ingredient.source().name());
            return map;
        }).toList();
    }
}

