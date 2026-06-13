package com.cusina.ai.model;

import java.util.Arrays;
import java.util.Optional;

public enum DietType {
    FIT("fit"),
    WEGAŃSKIE("wegańskie"),
    WEGETARIAŃSKIE("wegetariańskie"),
    BEZGLUTENOWE("bezglutenowe"),
    NA_PATRZE("na patrze"),
    KUCHNIA_AZJATYCKA("kuchnia azjatycka"),
    KUCHNIA_SRODZIEMNOMORSKA("kuchnia śródziemnomorska");

    private final String value;

    DietType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static Optional<DietType> fromValue(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Arrays.stream(values()).filter(v -> v.value.equals(value.trim())).findFirst();
    }
}

