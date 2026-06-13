package com.cusina.ai.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
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

        repository.save("session:a", new PersistedIngredientSession(true, List.of("Chicken", " Garlic ", "")));
        repository.save("session:b", new PersistedIngredientSession(true, List.of("Eggs")));

        assertThat(repository.load("session:a")).isPresent();
        assertThat(repository.load("session:a").orElseThrow().ingredientNames()).containsExactly("Chicken", "Garlic");
        assertThat(repository.load("session:b").orElseThrow().ingredientNames()).containsExactly("Eggs");
    }
}

