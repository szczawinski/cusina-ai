package com.cusina.ai.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IngredientTest {

    @Test
    void shouldNormalizeMillilitersToLitersForMultiplesOfThousand() {
        Ingredient ingredient = Ingredient.userAdded("Woda", BigDecimal.valueOf(2000), IngredientUnit.ML);

        assertThat(ingredient.quantity()).isEqualByComparingTo("2");
        assertThat(ingredient.unit()).isEqualTo(IngredientUnit.L);
        assertThat(ingredient.displayAmount()).isEqualTo("2 l");
    }

    @Test
    void shouldNormalizeGramsToKilogramsForMultiplesOfThousand() {
        Ingredient ingredient = Ingredient.userAdded("Mąka", BigDecimal.valueOf(3000), IngredientUnit.G);

        assertThat(ingredient.quantity()).isEqualByComparingTo("3");
        assertThat(ingredient.unit()).isEqualTo(IngredientUnit.KG);
        assertThat(ingredient.displayAmount()).isEqualTo("3 kg");
    }

    @Test
    void shouldKeepOriginalUnitWhenNotMultipleOfThousand() {
        Ingredient ingredient = Ingredient.userAdded("Sok", BigDecimal.valueOf(1500), IngredientUnit.ML);

        assertThat(ingredient.quantity()).isEqualByComparingTo("1500");
        assertThat(ingredient.unit()).isEqualTo(IngredientUnit.ML);
        assertThat(ingredient.displayAmount()).isEqualTo("1500 ml");
    }

    @Test
    void shouldRejectFractionalQuantityForPiecesUnit() {
        assertThatThrownBy(() -> Ingredient.userAdded("Jajka", BigDecimal.valueOf(1.5), IngredientUnit.SZT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive integer");
    }
}

