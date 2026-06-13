package com.cusina.ai.controller.form;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class MealRequestForm {

    @Size(max = 500, message = "{meal.preferences.size}")
    private String dietaryPreferences;

    @Pattern(regexp = "^(|śniadania|lunche|zupy|sałatki|makarony|dania główne|podwieczorki|napoje|kolacje)$", message = "{meal.dishType.invalid}")
    private String dishType;

    @Pattern(regexp = "^(|fit|wegańskie|wegetariańskie|bezglutenowe|na patrze|kuchnia azjatycka|kuchnia śródziemnomorska)$", message = "{meal.dietType.invalid}")
    private String dietType;

    public String getDietaryPreferences() {
        return dietaryPreferences;
    }

    public void setDietaryPreferences(String dietaryPreferences) {
        this.dietaryPreferences = dietaryPreferences;
    }

    public String getDishType() {
        return dishType;
    }

    public void setDishType(String dishType) {
        this.dishType = dishType;
    }

    public String getDietType() {
        return dietType;
    }

    public void setDietType(String dietType) {
        this.dietType = dietType;
    }

}

