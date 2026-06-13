package com.cusina.ai.session;

import com.cusina.ai.config.FirestorePersistenceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class IngredientPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(IngredientPersistenceService.class);

    private final IngredientFileRepository fileRepository;
    @Nullable
    private final IngredientFirestoreRepository firestoreRepository;
    private final FirestorePersistenceProperties firestoreProperties;

    public IngredientPersistenceService(IngredientFileRepository fileRepository,
                                        @Nullable IngredientFirestoreRepository firestoreRepository,
                                        FirestorePersistenceProperties firestoreProperties) {
        this.fileRepository = fileRepository;
        this.firestoreRepository = firestoreRepository;
        this.firestoreProperties = firestoreProperties;
    }

    public Optional<PersistedIngredientSession> load(String sessionKey) {
        if (shouldUseFirestore()) {
            try {
                return firestoreRepository.load(sessionKey);
            } catch (Exception ex) {
                log.warn("Firestore load failed for key '{}'. Falling back to file persistence.", sessionKey, ex);
            }
        }
        return fileRepository.load(sessionKey);
    }

    public void save(String sessionKey, PersistedIngredientSession state) {
        if (shouldUseFirestore()) {
            try {
                firestoreRepository.save(sessionKey, state);
                return;
            } catch (Exception ex) {
                log.warn("Firestore save failed for key '{}'. Falling back to file persistence.", sessionKey, ex);
            }
        }
        fileRepository.save(sessionKey, state);
    }

    private boolean shouldUseFirestore() {
        return firestoreProperties.isEnabled() && firestoreRepository != null;
    }
}

