package com.cusina.ai.model;

import java.util.Arrays;
import java.util.Optional;

public enum IngredientUnit {
    SZT("szt"),
    G("g"),
    KG("kg"),
    ML("ml"),
    L("l");

    private final String value;

    IngredientUnit(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static Optional<IngredientUnit> fromValue(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return Optional.empty();
        }
        String normalized = rawValue.trim().toLowerCase();
        return Arrays.stream(values())
                .filter(unit -> unit.value.equals(normalized))
                .findFirst();
    }
}

