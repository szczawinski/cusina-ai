package com.cusina.ai.model;

import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

public class MealRequest {

    private List<String> ingredients = new ArrayList<>();

    @Size(max = 500, message = "Dietary preferences must be 500 characters or fewer.")
    private String dietaryPreferences;

    public List<String> getIngredients() {
        return ingredients;
    }

    public void setIngredients(List<String> ingredients) {
        this.ingredients = ingredients;
    }

    public String getDietaryPreferences() {
        return dietaryPreferences;
    }

    public void setDietaryPreferences(String dietaryPreferences) {
        this.dietaryPreferences = dietaryPreferences;
    }
}

