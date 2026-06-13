package com.cusina.ai.controller.form;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class IngredientForm {

    @NotBlank(message = "{ingredient.name.notBlank}")
    @Size(max = 100, message = "{ingredient.name.size}")
    private String name;

    @DecimalMin(value = "0.01", message = "{ingredient.quantity.min}")
    @Digits(integer = 6, fraction = 2, message = "{ingredient.quantity.format}")
    private BigDecimal quantity;

    @Pattern(regexp = "^(|szt|g|kg|ml|l)$", message = "{ingredient.unit.invalid}")
    private String unit;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }
}

