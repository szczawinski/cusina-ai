package com.cusina.ai.session;

import com.cusina.ai.config.FirestorePersistenceProperties;
import com.cusina.ai.model.Ingredient;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IngredientPersistenceServiceTest {

    @Test
    void shouldUseFileRepositoryWhenFirestoreIsDisabled() {
        IngredientFileRepository fileRepository = mock(IngredientFileRepository.class);
        IngredientFirestoreRepository firestoreRepository = mock(IngredientFirestoreRepository.class);
        FirestorePersistenceProperties properties = new FirestorePersistenceProperties();
        properties.setEnabled(false);

        PersistedIngredientSession state = new PersistedIngredientSession(true, List.of(Ingredient.userAdded("A")));
        when(fileRepository.load("session:a")).thenReturn(Optional.of(state));

        IngredientPersistenceService service = new IngredientPersistenceService(fileRepository, firestoreRepository, properties);

        assertThat(service.load("session:a")).contains(state);
        verify(fileRepository).load("session:a");
    }

    @Test
    void shouldFallbackToFileWhenFirestoreFails() {
        IngredientFileRepository fileRepository = mock(IngredientFileRepository.class);
        IngredientFirestoreRepository firestoreRepository = mock(IngredientFirestoreRepository.class);
        FirestorePersistenceProperties properties = new FirestorePersistenceProperties();
        properties.setEnabled(true);

        PersistedIngredientSession state = new PersistedIngredientSession(true, List.of(Ingredient.userAdded("A")));
        when(fileRepository.load("session:a")).thenReturn(Optional.of(state));
        when(firestoreRepository.load(any())).thenThrow(new IllegalStateException("boom"));

        IngredientPersistenceService service = new IngredientPersistenceService(fileRepository, firestoreRepository, properties);

        assertThat(service.load("session:a")).contains(state);
        verify(fileRepository).load("session:a");

        doThrow(new IllegalStateException("boom")).when(firestoreRepository).save(any(), any());
        service.save("session:a", state);
        verify(fileRepository).save("session:a", state);
    }
}


