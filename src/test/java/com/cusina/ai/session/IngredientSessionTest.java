package com.cusina.ai.session;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IngredientSessionTest {

    private IngredientSession createSessionWithPreload(List<String> preload) {
        IngredientPool pool = mock(IngredientPool.class);
        when(pool.drawUnique(10)).thenReturn(preload);
        return new IngredientSession(pool);
    }

    @Test
    void shouldInitializeSessionWithExactlyTenPreloadedIngredients() {
        IngredientSession session = createSessionWithPreload(List.of(
                "Jajka", "Pomidor", "Ogórek", "Papryka", "Cebula", "Czosnek", "Ryż", "Makaron", "Kasza", "Marchew"
        ));

        session.initializeIfNeeded();

        assertThat(session.size()).isEqualTo(10);
        assertThat(session.getIngredients()).allMatch(i -> i.source() == com.cusina.ai.model.Ingredient.Source.PRELOADED);
    }

    @Test
    void shouldApplyCaseInsensitiveFirstWriteWinsDeduplication() {
        IngredientSession session = createSessionWithPreload(List.of(
                "Jajka", "Pomidor", "Ogórek", "Papryka", "Cebula", "Czosnek", "Ryż", "Makaron", "Kasza", "Marchew"
        ));
        session.initializeIfNeeded();

        assertThat(session.addIngredient("Chicken")).isEqualTo(IngredientSession.AddResult.ADDED);
        assertThat(session.addIngredient("chicken")).isEqualTo(IngredientSession.AddResult.DUPLICATE);

        assertThat(session.size()).isEqualTo(11);
        assertThat(session.getIngredients().get(10).displayName()).isEqualTo("Chicken");
    }

    @Test
    void shouldEnforceFiftyIngredientCap() {
        IngredientSession session = createSessionWithPreload(List.of(
                "Jajka", "Pomidor", "Ogórek", "Papryka", "Cebula", "Czosnek", "Ryż", "Makaron", "Kasza", "Marchew"
        ));
        session.initializeIfNeeded();
        for (int i = 0; i < 40; i++) {
            assertThat(session.addIngredient("item" + i)).isEqualTo(IngredientSession.AddResult.ADDED);
        }

        assertThat(session.addIngredient("overflow")).isEqualTo(IngredientSession.AddResult.FULL);
        assertThat(session.size()).isEqualTo(50);
        assertThat(session.isFull()).isTrue();
    }

    @Test
    void shouldNotReinitializeAlreadyInitializedSession() {
        IngredientSession session = createSessionWithPreload(List.of(
                "A", "B", "C", "D", "E", "F", "G", "H", "I", "J"
        ));

        session.initializeIfNeeded();
        session.initializeIfNeeded();

        assertThat(session.size()).isEqualTo(10);
    }
}

