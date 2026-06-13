package com.cusina.ai.session;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IngredientSessionTest {

    private IngredientSession createSessionWithPreload(List<String> preload) {
        IngredientPool pool = mock(IngredientPool.class);
        IngredientPersistenceService persistenceService = mock(IngredientPersistenceService.class);
        when(persistenceService.load(any())).thenReturn(java.util.Optional.empty());
        when(pool.drawUnique(10)).thenReturn(preload);
        return new IngredientSession(pool, persistenceService, "session:test");
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
    void shouldApplyPreloadedQuantityDefaultsForKnownProducts() {
        IngredientSession session = createSessionWithPreload(List.of(
                "Jajka", "Mleko", "Pierś z kurczaka", "Pomidor", "Ogórek", "Papryka", "Cebula", "Czosnek", "Ryż", "Makaron"
        ));

        session.initializeIfNeeded();

        assertThat(session.getIngredients().stream().filter(i -> i.displayName().equals("Jajka")).findFirst().orElseThrow().displayAmount())
                .isEqualTo("6 szt");
        assertThat(session.getIngredients().stream().filter(i -> i.displayName().equals("Mleko")).findFirst().orElseThrow().displayAmount())
                .isEqualTo("1 l");
        assertThat(session.getIngredients().stream().filter(i -> i.displayName().equals("Pierś z kurczaka")).findFirst().orElseThrow().displayAmount())
                .isEqualTo("500 g");
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
    void shouldApplyInferredDefaultsWhenManualQuantityOrUnitMissing() {
        IngredientSession session = createSessionWithPreload(List.of(
                "Jajka", "Pomidor", "Ogórek", "Papryka", "Cebula", "Czosnek", "Ryż", "Makaron", "Kasza", "Marchew"
        ));
        session.initializeIfNeeded();

        assertThat(session.addIngredient("Mleko", null, null)).isEqualTo(IngredientSession.AddResult.ADDED);
        assertThat(session.addIngredient("Kasza gryczana", BigDecimal.valueOf(250), "g")).isEqualTo(IngredientSession.AddResult.ADDED);

        assertThat(session.getIngredients().stream().filter(i -> i.displayName().equals("Mleko")).findFirst().orElseThrow().displayAmount())
                .isEqualTo("1 l");
        assertThat(session.getIngredients().stream().filter(i -> i.displayName().equals("Kasza gryczana")).findFirst().orElseThrow().displayAmount())
                .isEqualTo("250 g");
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

    @Test
    void shouldLoadStateFromPersistenceBeforeApplyingBusinessRules() {
        IngredientPool pool = mock(IngredientPool.class);
        IngredientPersistenceService persistenceService = mock(IngredientPersistenceService.class);
        when(persistenceService.load("session:test")).thenReturn(java.util.Optional.of(
                new PersistedIngredientSession(true, List.of(), List.of("Pomidor", "Cebula"))
        ));

        IngredientSession session = new IngredientSession(pool, persistenceService, "session:test");

        assertThat(session.getIngredientNames()).containsExactly("Pomidor", "Cebula");
        verify(persistenceService).load("session:test");
    }

    @Test
    void shouldPreferRequestedSessionIdForPersistenceKey() {
        String key = IngredientSession.resolveSessionKey("old-session-id", "new-session-id");

        assertThat(key).isEqualTo("session:old-session-id");
    }

    @Test
    void shouldFallbackToCurrentSessionIdWhenRequestedIsBlank() {
        String key = IngredientSession.resolveSessionKey("  ", "new-session-id");

        assertThat(key).isEqualTo("session:new-session-id");
    }
}
