package com.cusina.ai.model;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MealRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void shouldRejectDietaryPreferencesLongerThan500Chars() {
        MealRequest request = new MealRequest();
        request.setDietaryPreferences("x".repeat(501));

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void shouldAcceptBlankDietaryPreferences() {
        MealRequest request = new MealRequest();
        request.setDietaryPreferences("");

        assertThat(validator.validate(request)).isEmpty();
    }
}

