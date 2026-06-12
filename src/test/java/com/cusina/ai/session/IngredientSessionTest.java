package com.cusina.ai.session;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IngredientSessionTest {

    private IngredientSession createSessionWithStoredNames(List<String> storedNames) {
        IngredientFileRepository repository = mock(IngredientFileRepository.class);
        when(repository.loadIngredientNames()).thenReturn(storedNames);
        return new IngredientSession(repository);
    }

    @Test
    void shouldApplyCaseInsensitiveFirstWriteWinsDeduplication() {
        IngredientSession session = createSessionWithStoredNames(List.of());

        assertThat(session.addIngredient("Chicken")).isEqualTo(IngredientSession.AddResult.ADDED);
        assertThat(session.addIngredient("chicken")).isEqualTo(IngredientSession.AddResult.DUPLICATE);

        assertThat(session.size()).isEqualTo(1);
        assertThat(session.getIngredients().get(0).displayName()).isEqualTo("Chicken");
    }

    @Test
    void shouldEnforceFiftyIngredientCap() {
        IngredientSession session = createSessionWithStoredNames(List.of());
        for (int i = 0; i < 50; i++) {
            assertThat(session.addIngredient("item" + i)).isEqualTo(IngredientSession.AddResult.ADDED);
        }

        assertThat(session.addIngredient("overflow")).isEqualTo(IngredientSession.AddResult.FULL);
        assertThat(session.size()).isEqualTo(50);
        assertThat(session.isFull()).isTrue();
    }

    @Test
    void shouldLoadStoredIngredientsWithDedupAndCap() {
        IngredientSession session = createSessionWithStoredNames(List.of(
                "Chicken", "chicken", "Garlic", " ", "Lemon"
        ));

        assertThat(session.getIngredientNames()).containsExactly("Chicken", "Garlic", "Lemon");
    }

    @Test
    void shouldPersistOnlyWhenStateChanges() {
        IngredientFileRepository repository = mock(IngredientFileRepository.class);
        when(repository.loadIngredientNames()).thenReturn(List.of());
        IngredientSession session = new IngredientSession(repository);

        assertThat(session.addIngredient("Tomato")).isEqualTo(IngredientSession.AddResult.ADDED);
        assertThat(session.addIngredient("tomato")).isEqualTo(IngredientSession.AddResult.DUPLICATE);
        session.removeIngredient("missing");
        session.removeIngredient("tomato");

        verify(repository).saveIngredientNames(List.of("Tomato"));
        verify(repository).saveIngredientNames(List.of());
        verify(repository, times(2)).saveIngredientNames(anyList());
    }
}

