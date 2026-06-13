package com.cusina.ai.session;

import java.util.List;

public record PersistedIngredientSession(boolean initialized, List<String> ingredientNames) {
}

