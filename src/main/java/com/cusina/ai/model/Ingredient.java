package com.cusina.ai.model;

import java.util.Locale;
import java.util.Objects;

public record Ingredient(String displayName, Source source) {

    public enum Source {
        PRELOADED,
        USER_ADDED
    }

    public Ingredient(String displayName) {
        this(displayName, Source.USER_ADDED);
    }

    public Ingredient {
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(source, "source");
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

    public static Ingredient preloaded(String displayName) {
        return new Ingredient(displayName, Source.PRELOADED);
    }

    public static Ingredient userAdded(String displayName) {
        return new Ingredient(displayName, Source.USER_ADDED);
    }
}

