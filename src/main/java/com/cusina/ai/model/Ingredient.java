package com.cusina.ai.model;

import java.util.Locale;
import java.util.Objects;

public record Ingredient(String displayName) {

    public Ingredient {
        Objects.requireNonNull(displayName, "displayName");
        displayName = displayName.trim();
        if (displayName.isBlank()) {
            throw new IllegalArgumentException("Ingredient name cannot be blank");
        }
        if (displayName.length() > 100) {
            throw new IllegalArgumentException("Ingredient name must be 100 characters or fewer");
        }
    }

    public String normalizedKey() {
        return displayName.trim().toLowerCase(Locale.ROOT);
    }
}

