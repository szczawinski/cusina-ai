package com.cusina.ai.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MealRequestValidationTest {

    @Test
    void shouldDefaultLocaleToPolish() {
        MealRequest request = new MealRequest();

        assertThat(request.getLocale()).isEqualTo("pl-PL");
    }

    @Test
    void shouldAllowSettingIngredientsAndPreferences() {
        MealRequest request = new MealRequest();
        request.setIngredients(java.util.List.of("Jajka", "Ser"));
        request.setDietaryPreferences("Bez glutenu");
        request.setDishType(DishType.LUNCHE);
        request.setDietType(DietType.WEGETARIAŃSKIE);

        assertThat(request.getIngredients()).containsExactly("Jajka", "Ser");
        assertThat(request.getDietaryPreferences()).isEqualTo("Bez glutenu");
        assertThat(request.getDishType()).isEqualTo(DishType.LUNCHE);
        assertThat(request.getDietType()).isEqualTo(DietType.WEGETARIAŃSKIE);
    }
}

