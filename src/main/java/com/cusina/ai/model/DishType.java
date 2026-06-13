package com.cusina.ai.model;

import java.util.Arrays;
import java.util.Optional;

public enum DishType {
    SNIADANIA("śniadania"),
    LUNCHE("lunche"),
    ZUPY("zupy"),
    SALATKI("sałatki"),
    MAKARONY("makarony"),
    DANIA_GLOWNE("dania główne"),
    PODWIECZORKI("podwieczorki"),
    NAPOJE("napoje"),
    KOLACJE("kolacje");

    private final String value;

    DishType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static Optional<DishType> fromValue(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Arrays.stream(values()).filter(v -> v.value.equals(value.trim())).findFirst();
    }
}

