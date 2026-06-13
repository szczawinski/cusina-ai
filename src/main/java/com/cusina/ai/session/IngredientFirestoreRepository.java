package com.cusina.ai.session;

import com.cusina.ai.config.FirestorePersistenceProperties;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
            List<String> names = sanitizeNames(snapshot.get("ingredientNames"));
            return Optional.of(new PersistedIngredientSession(Boolean.TRUE.equals(initialized), names));
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
                    "ingredientNames", sanitizeNames(state.ingredientNames()),
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

    private List<String> sanitizeNames(Object namesValue) {
        if (!(namesValue instanceof List<?> names)) {
            return List.of();
        }
        return names.stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .map(String::trim)
                .filter(name -> !name.isBlank())
                .toList();
    }
}
