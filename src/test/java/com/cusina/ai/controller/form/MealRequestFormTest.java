package com.cusina.ai.controller.form;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MealRequestFormTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void shouldRejectDietaryPreferencesLongerThan500Chars() {
        MealRequestForm form = new MealRequestForm();
        form.setDietaryPreferences("x".repeat(501));

        assertThat(validator.validate(form)).isNotEmpty();
    }

    @Test
    void shouldAllowEmptyDietaryPreferences() {
        MealRequestForm form = new MealRequestForm();
        form.setDietaryPreferences("");

        assertThat(validator.validate(form)).isEmpty();
    }

    @Test
    void shouldRejectDishTypeOutsideClosedList() {
        MealRequestForm form = new MealRequestForm();
        form.setDishType("kolacja");

        assertThat(validator.validate(form)).extracting("propertyPath").anyMatch(path -> path.toString().equals("dishType"));
    }

    @Test
    void shouldRejectDietTypeOutsideClosedList() {
        MealRequestForm form = new MealRequestForm();
        form.setDietType("keto");

        assertThat(validator.validate(form)).extracting("propertyPath").anyMatch(path -> path.toString().equals("dietType"));
    }
}

