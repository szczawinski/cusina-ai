package com.cusina.ai.session;

import com.cusina.ai.model.MealSuggestion;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Component
@SessionScope
public class MealSelectionSession implements Serializable {

    private final List<MealSuggestion> latestSuggestions = new ArrayList<>();
    private final List<MealSuggestion> selectedMealsHistory = new ArrayList<>();
    private MealSuggestion selectedMeal;

    public void updateLatestSuggestions(List<MealSuggestion> suggestions) {
        latestSuggestions.clear();
        if (suggestions != null) {
            suggestions.stream().map(this::copyMeal).forEach(latestSuggestions::add);
        }
    }

    public List<MealSuggestion> getLatestSuggestions() {
        return Collections.unmodifiableList(latestSuggestions);
    }

    public boolean hasLatestSuggestions() {
        return !latestSuggestions.isEmpty();
    }

    public Optional<MealSuggestion> selectFromLatest(int index) {
        if (index < 0 || index >= latestSuggestions.size()) {
            return Optional.empty();
        }

        MealSuggestion selected = copyMeal(latestSuggestions.get(index));
        selectedMeal = selected;
        selectedMealsHistory.add(0, selected);
        return Optional.of(selected);
    }

    public Optional<MealSuggestion> getSelectedMeal() {
        return Optional.ofNullable(selectedMeal);
    }

    public List<MealSuggestion> getSelectedMealsHistory() {
        return Collections.unmodifiableList(selectedMealsHistory);
    }

    public int getSelectionCount() {
        return selectedMealsHistory.size();
    }

    public int getUniqueMealCount() {
        Set<String> names = new HashSet<>();
        for (MealSuggestion meal : selectedMealsHistory) {
            if (meal.getName() != null && !meal.getName().isBlank()) {
                names.add(meal.getName().trim().toLowerCase(Locale.ROOT));
            }
        }
        return names.size();
    }

    private MealSuggestion copyMeal(MealSuggestion source) {
        MealSuggestion copy = new MealSuggestion();
        copy.setName(source.getName());
        copy.setDescription(source.getDescription());
        copy.setSteps(source.getSteps() == null ? List.of() : new ArrayList<>(source.getSteps()));
        copy.setUsedIngredients(source.getUsedIngredients() == null ? List.of() : new ArrayList<>(source.getUsedIngredients()));
        return copy;
    }
}

