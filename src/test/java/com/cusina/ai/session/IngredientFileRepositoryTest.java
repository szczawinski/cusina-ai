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

        assertThat(repository.loadIngredientNames()).isEmpty();
    }

    @Test
    void shouldSaveAndLoadIngredientNames() {
        Path file = tempDir.resolve("ingredients.json");
        IngredientFileRepository repository = new IngredientFileRepository(new ObjectMapper(), file);

        repository.saveIngredientNames(List.of("Chicken", " Garlic ", ""));

        assertThat(repository.loadIngredientNames()).containsExactly("Chicken", "Garlic");
    }
}

