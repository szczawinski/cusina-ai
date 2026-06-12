package com.cusina.ai.controller.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class IngredientForm {

    @NotBlank(message = "Ingredient name cannot be empty.")
    @Size(max = 100, message = "Ingredient name must be 100 characters or fewer.")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

