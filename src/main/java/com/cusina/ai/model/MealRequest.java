package com.cusina.ai.model;

import java.util.ArrayList;
import java.util.List;

public class MealRequest {

    private List<String> ingredients = new ArrayList<>();
    private List<String> ingredientDetails = new ArrayList<>();
    private String dietaryPreferences;
    private DishType dishType;
    private DietType dietType;
    private String locale = "pl-PL";

    public List<String> getIngredients() {
        return ingredients;
    }

    public void setIngredients(List<String> ingredients) {
        this.ingredients = ingredients;
    }

    public List<String> getIngredientDetails() {
        return ingredientDetails;
    }

    public void setIngredientDetails(List<String> ingredientDetails) {
        this.ingredientDetails = ingredientDetails;
    }

    public String getDietaryPreferences() {
        return dietaryPreferences;
    }

    public void setDietaryPreferences(String dietaryPreferences) {
        this.dietaryPreferences = dietaryPreferences;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public DishType getDishType() {
        return dishType;
    }

    public void setDishType(DishType dishType) {
        this.dishType = dishType;
    }

    public DietType getDietType() {
        return dietType;
    }

    public void setDietType(DietType dietType) {
        this.dietType = dietType;
    }
}

