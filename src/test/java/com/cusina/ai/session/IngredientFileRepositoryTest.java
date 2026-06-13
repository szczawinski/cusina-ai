package com.cusina.ai.session;

import com.cusina.ai.model.Ingredient;
import com.cusina.ai.model.IngredientUnit;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IngredientFileRepositoryTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldReturnEmptyListWhenFileIsMissing() {
        IngredientFileRepository repository = new IngredientFileRepository(new ObjectMapper(), tempDir.resolve("missing.json"));

        assertThat(repository.load("session:a")).isEmpty();
    }

    @Test
    void shouldSaveAndLoadIngredientNamesPerSession() {
        Path file = tempDir.resolve("ingredients.json");
        IngredientFileRepository repository = new IngredientFileRepository(new ObjectMapper(), file);

        repository.save("session:a", new PersistedIngredientSession(true, List.of(
                Ingredient.userAdded("Chicken", BigDecimal.valueOf(500), IngredientUnit.G),
                Ingredient.userAdded(" Garlic ", BigDecimal.ONE, IngredientUnit.SZT)
        )));
        repository.save("session:b", new PersistedIngredientSession(true, List.of(
                Ingredient.userAdded("Eggs", BigDecimal.valueOf(6), IngredientUnit.SZT)
        )));

        assertThat(repository.load("session:a")).isPresent();
        assertThat(repository.load("session:a").orElseThrow().resolvedIngredients()).extracting(Ingredient::displayName)
                .containsExactly("Chicken", "Garlic");
        assertThat(repository.load("session:b").orElseThrow().resolvedIngredients()).extracting(Ingredient::displayName)
                .containsExactly("Eggs");
    }

    @Test
    void shouldLoadLegacyNameOnlyEntriesWithDefaults() throws Exception {
        Path file = tempDir.resolve("legacy.json");
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.writeValue(file.toFile(), java.util.Map.of(
                "session:a", java.util.Map.of(
                        "initialized", true,
                        "ingredientNames", List.of("Mleko", "Jajka")
                )
        ));

        IngredientFileRepository repository = new IngredientFileRepository(objectMapper, file);

        PersistedIngredientSession loaded = repository.load("session:a").orElseThrow();
        assertThat(loaded.resolvedIngredients()).extracting(Ingredient::displayAmount)
                .containsExactly("1 l", "6 szt");
    }

    @Test
    void shouldKeepPersistenceCompatibleAndNormalizeMetricMultiplesOnLoad() {
        Path file = tempDir.resolve("normalized.json");
        IngredientFileRepository repository = new IngredientFileRepository(new ObjectMapper(), file);

        repository.save("session:a", new PersistedIngredientSession(true, List.of(
                Ingredient.userAdded("Woda", BigDecimal.valueOf(2000), IngredientUnit.ML)
        )));

        PersistedIngredientSession loaded = repository.load("session:a").orElseThrow();
        assertThat(loaded.resolvedIngredients()).extracting(Ingredient::displayAmount)
                .containsExactly("2 l");
    }
}

