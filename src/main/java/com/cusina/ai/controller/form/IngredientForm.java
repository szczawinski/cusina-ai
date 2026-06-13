package com.cusina.ai.controller.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class IngredientForm {

    @NotBlank(message = "{ingredient.name.notBlank}")
    @Size(max = 100, message = "{ingredient.name.size}")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

