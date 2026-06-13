package com.cusina.ai.session;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class IngredientPool {

    private static final List<String> CURATED_INGREDIENTS = List.of(
            "Jajka", "Mleko", "Pierś z kurczaka", "Pomidor", "Ogórek", "Papryka", "Cebula", "Czosnek", "Ryż", "Makaron", "Kasza", "Marchew",
            "Brokuł", "Szpinak", "Pieczarki", "Ser", "Twaróg", "Jogurt naturalny", "Indyk", "Tuńczyk", "Fasola",
            "Ciecierzyca", "Soczewica", "Ziemniaki", "Bataty", "Cukinia", "Bakłażan", "Płatki owsiane", "Masło", "Oliwa"
    );

    private final SecureRandom random = new SecureRandom();

    public List<String> drawUnique(int count) {
        if (count <= 0) {
            return List.of();
        }
        if (CURATED_INGREDIENTS.size() < count) {
            throw new IllegalStateException("Ingredient pool must contain at least " + count + " items");
        }

        List<String> shuffled = new ArrayList<>(CURATED_INGREDIENTS);
        for (int i = shuffled.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            String tmp = shuffled.get(i);
            shuffled.set(i, shuffled.get(j));
            shuffled.set(j, tmp);
        }

        Set<String> normalized = new LinkedHashSet<>();
        List<String> unique = new ArrayList<>();
        for (String item : shuffled) {
            String key = item.trim().toLowerCase(Locale.ROOT);
            if (normalized.add(key)) {
                unique.add(item);
            }
            if (unique.size() == count) {
                return unique;
            }
        }

        throw new IllegalStateException("Not enough unique ingredients after normalization");
    }
}

