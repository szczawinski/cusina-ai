package com.cusina.ai.model;

import java.util.ArrayList;
import java.util.List;

public class MealResponse {

    private List<MealSuggestion> meals = new ArrayList<>();
    private int omittedMalformedCount;
    private String warning;
    private String error;

    public static MealResponse error(String message) {
        MealResponse response = new MealResponse();
        response.setError(message);
        return response;
    }

    public boolean hasError() {
        return error != null && !error.isBlank();
    }

    public boolean hasWarning() {
        return warning != null && !warning.isBlank();
    }

    public boolean hasMeals() {
        return meals != null && !meals.isEmpty();
    }

    public List<MealSuggestion> getMeals() {
        return meals;
    }

    public void setMeals(List<MealSuggestion> meals) {
        this.meals = meals;
    }

    public int getOmittedMalformedCount() {
        return omittedMalformedCount;
    }

    public void setOmittedMalformedCount(int omittedMalformedCount) {
        this.omittedMalformedCount = omittedMalformedCount;
    }

    public String getWarning() {
        return warning;
    }

    public void setWarning(String warning) {
        this.warning = warning;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}

