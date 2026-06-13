package com.cusina.ai.session;

import com.cusina.ai.model.IngredientUnit;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;

final class IngredientDefaults {

    private static final Amount DEFAULT_AMOUNT = new Amount(BigDecimal.ONE, IngredientUnit.SZT);

    private static final Map<String, Amount> PRELOADED_DEFAULTS = Map.ofEntries(
            Map.entry("jajka", new Amount(BigDecimal.valueOf(6), IngredientUnit.SZT)),
            Map.entry("mleko", new Amount(BigDecimal.ONE, IngredientUnit.L)),
            Map.entry("pierś z kurczaka", new Amount(BigDecimal.valueOf(500), IngredientUnit.G)),
            Map.entry("ryż", new Amount(BigDecimal.valueOf(500), IngredientUnit.G)),
            Map.entry("makaron", new Amount(BigDecimal.valueOf(500), IngredientUnit.G)),
            Map.entry("oliwa", new Amount(BigDecimal.valueOf(250), IngredientUnit.ML)),
            Map.entry("jogurt naturalny", new Amount(BigDecimal.valueOf(400), IngredientUnit.G)),
            Map.entry("masło", new Amount(BigDecimal.valueOf(200), IngredientUnit.G)),
            Map.entry("ser", new Amount(BigDecimal.valueOf(200), IngredientUnit.G)),
            Map.entry("twaróg", new Amount(BigDecimal.valueOf(250), IngredientUnit.G))
    );

    private IngredientDefaults() {
    }

    static Amount resolvePreloaded(String displayName) {
        return PRELOADED_DEFAULTS.getOrDefault(normalize(displayName), inferForName(displayName));
    }

    static Amount inferForName(String displayName) {
        String normalized = normalize(displayName);
        if (PRELOADED_DEFAULTS.containsKey(normalized)) {
            return PRELOADED_DEFAULTS.get(normalized);
        }
        if (containsAny(normalized, "mleko", "woda", "sok", "bulion")) {
            return new Amount(BigDecimal.ONE, IngredientUnit.L);
        }
        if (containsAny(normalized, "oliwa", "olej", "ocet", "sos")) {
            return new Amount(BigDecimal.valueOf(100), IngredientUnit.ML);
        }
        if (containsAny(normalized, "kurczak", "indyk", "wołowina", "wieprzowina", "ryż", "makaron", "kasza")) {
            return new Amount(BigDecimal.valueOf(500), IngredientUnit.G);
        }
        if (containsAny(normalized, "jaj", "ząbek")) {
            return new Amount(BigDecimal.ONE, IngredientUnit.SZT);
        }
        return DEFAULT_AMOUNT;
    }

    static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean containsAny(String text, String... tokens) {
        for (String token : tokens) {
            if (text.contains(token)) {
                return true;
            }
        }
        return false;
    }

    record Amount(BigDecimal quantity, IngredientUnit unit) {
    }
}

