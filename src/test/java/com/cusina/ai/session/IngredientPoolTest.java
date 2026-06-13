package com.cusina.ai.session;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class IngredientPoolTest {

    @Test
    void shouldDrawExactlyTenUniqueIngredients() {
        IngredientPool pool = new IngredientPool();

        List<String> drawn = pool.drawUnique(10);

        assertThat(drawn).hasSize(10);
        assertThat(drawn.stream().map(v -> v.toLowerCase(Locale.ROOT)).distinct().count()).isEqualTo(10);
    }

    @Test
    void shouldProduceIndependentRandomDraws() {
        IngredientPool pool = new IngredientPool();

        List<String> first = pool.drawUnique(10);
        List<String> second = pool.drawUnique(10);

        assertThat(first).hasSize(10);
        assertThat(second).hasSize(10);
    }
}

