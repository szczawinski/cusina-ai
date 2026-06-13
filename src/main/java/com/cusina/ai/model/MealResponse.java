package com.cusina.ai.model;

import java.util.ArrayList;
import java.util.List;

public class MealResponse {

    private int rawCount;
    private List<MealSuggestion> meals = new ArrayList<>();
    private int omittedMalformedCount;
    private String warningPl;
    private String errorPl;

    public static MealResponse error(String message) {
        MealResponse response = new MealResponse();
        response.setErrorPl(message);
        return response;
    }

    public boolean hasError() {
        return errorPl != null && !errorPl.isBlank();
    }

    public boolean hasWarning() {
        return warningPl != null && !warningPl.isBlank();
    }
    public int getRawCount() {
        return rawCount;
    }

    public void setRawCount(int rawCount) {
        this.rawCount = rawCount;
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

    public List<MealSuggestion> getValidSuggestions() {
        return meals;
    }

    public int getOmittedMalformedCount() {
        return omittedMalformedCount;
    }

    public void setOmittedMalformedCount(int omittedMalformedCount) {
        this.omittedMalformedCount = omittedMalformedCount;
    }

    public String getWarningPl() {
        return warningPl;
    }

    public void setWarningPl(String warningPl) {
        this.warningPl = warningPl;
    }

    public String getErrorPl() {
        return errorPl;
    }

    public void setErrorPl(String errorPl) {
        this.errorPl = errorPl;
    }
}

