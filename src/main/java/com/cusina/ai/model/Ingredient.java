package com.cusina.ai.model;

import java.util.Locale;
import java.util.Objects;
import java.math.BigDecimal;

public record Ingredient(String displayName, BigDecimal quantity, IngredientUnit unit, Source source) {

    public enum Source {
        PRELOADED,
        USER_ADDED
    }

    public Ingredient(String displayName) {
        this(displayName, BigDecimal.ONE, IngredientUnit.SZT, Source.USER_ADDED);
    }

    public Ingredient(String displayName, Source source) {
        this(displayName, BigDecimal.ONE, IngredientUnit.SZT, source);
    }

    public Ingredient {
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(quantity, "quantity");
        Objects.requireNonNull(unit, "unit");
        Objects.requireNonNull(source, "source");
        displayName = displayName.trim();
        if (displayName.isBlank()) {
            throw new IllegalArgumentException("Ingredient name cannot be blank");
        }
        if (displayName.length() > 100) {
            throw new IllegalArgumentException("Ingredient name must be 100 characters or fewer");
        }
        if (quantity.signum() <= 0) {
            throw new IllegalArgumentException("Ingredient quantity must be greater than zero");
        }
        quantity = quantity.stripTrailingZeros();
    }

    public String normalizedKey() {
        return displayName.trim().toLowerCase(Locale.ROOT);
    }

    public String displayAmount() {
        return quantity.stripTrailingZeros().toPlainString() + " " + unit.value();
    }

    public static Ingredient preloaded(String displayName) {
        return new Ingredient(displayName, BigDecimal.ONE, IngredientUnit.SZT, Source.PRELOADED);
    }

    public static Ingredient preloaded(String displayName, BigDecimal quantity, IngredientUnit unit) {
        return new Ingredient(displayName, quantity, unit, Source.PRELOADED);
    }

    public static Ingredient userAdded(String displayName) {
        return new Ingredient(displayName, BigDecimal.ONE, IngredientUnit.SZT, Source.USER_ADDED);
    }

    public static Ingredient userAdded(String displayName, BigDecimal quantity, IngredientUnit unit) {
        return new Ingredient(displayName, quantity, unit, Source.USER_ADDED);
    }
}

