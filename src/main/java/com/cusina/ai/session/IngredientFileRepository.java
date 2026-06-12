package com.cusina.ai.session;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

@Component
public class IngredientFileRepository {

    private static final Logger log = LoggerFactory.getLogger(IngredientFileRepository.class);

    private final ObjectMapper objectMapper;
    private final Path filePath;

    @Autowired
    public IngredientFileRepository(ObjectMapper objectMapper,
                                    @Value("${ingredients.persistence.file:data/ingredients.json}") String filePath) {
        this(objectMapper, Path.of(filePath));
    }

    IngredientFileRepository(ObjectMapper objectMapper, Path filePath) {
        this.objectMapper = objectMapper;
        this.filePath = filePath;
    }

    public synchronized List<String> loadIngredientNames() {
        if (!Files.exists(filePath)) {
            return List.of();
        }

        try {
            List<String> names = objectMapper.readValue(filePath.toFile(), new TypeReference<>() {
            });
            if (names == null) {
                return List.of();
            }
            return names.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(name -> !name.isBlank())
                    .toList();
        } catch (IOException ex) {
            log.warn("Could not read ingredients from {}. Starting with an empty list.", filePath, ex);
            return List.of();
        }
    }

    public synchronized void saveIngredientNames(List<String> ingredientNames) {
        try {
            Path parent = filePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), ingredientNames);
        } catch (IOException ex) {
            log.warn("Could not save ingredients to {}.", filePath, ex);
        }
    }
}


