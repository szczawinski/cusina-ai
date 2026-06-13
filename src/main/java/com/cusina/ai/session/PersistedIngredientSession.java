package com.cusina.ai.session;

import com.cusina.ai.model.Ingredient;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PersistedIngredientSession(boolean initialized,
										 List<Ingredient> ingredients,
										 List<String> ingredientNames) {

	public PersistedIngredientSession(boolean initialized, List<Ingredient> ingredients) {
		this(initialized, ingredients, List.of());
	}

	public List<Ingredient> resolvedIngredients() {
		if (ingredients != null && !ingredients.isEmpty()) {
			return ingredients;
		}
		if (ingredientNames == null || ingredientNames.isEmpty()) {
			return List.of();
		}
		return ingredientNames.stream()
				.filter(name -> name != null && !name.isBlank())
				.map(name -> {
					IngredientDefaults.Amount defaults = IngredientDefaults.inferForName(name);
					return Ingredient.userAdded(name, defaults.quantity(), defaults.unit());
				})
				.toList();
	}
}


