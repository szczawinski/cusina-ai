package com.cusina.ai.controller;

import com.cusina.ai.config.AnthropicProperties;
import com.cusina.ai.model.Ingredient;
import com.cusina.ai.model.MealResponse;
import com.cusina.ai.model.MealSuggestion;
import com.cusina.ai.service.MealSuggestionService;
import com.cusina.ai.session.IngredientSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(MealController.class)
@Import(MealControllerTest.TestConfig.class)
class MealControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IngredientSession ingredientSession;

    @MockBean
    private MealSuggestionService mealSuggestionService;

    @TestConfiguration
    static class TestConfig {
        @Bean
        AnthropicProperties anthropicProperties() {
            AnthropicProperties p = new AnthropicProperties();
            p.setTimeoutSeconds(1);
            p.setDietaryPreferenceMaxLength(500);
            return p;
        }
    }

    @Test
    void shouldRedirectMealRequestWhenIngredientListIsEmpty() throws Exception {
        when(ingredientSession.isEmpty()).thenReturn(true);

        mockMvc.perform(get("/meal-request"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ingredients"));
    }

    @Test
    void shouldRenderMealRequestWhenIngredientsPresent() throws Exception {
        when(ingredientSession.isEmpty()).thenReturn(false);
        when(ingredientSession.getIngredients()).thenReturn(List.of(new Ingredient("Eggs")));

        mockMvc.perform(get("/meal-request"))
                .andExpect(status().isOk())
                .andExpect(view().name("meal-request"))
                .andExpect(model().attributeExists("mealRequest", "ingredients", "charLimit"));
    }

    @Test
    void shouldReturnValidationErrorWhenPreferencesTooLong() throws Exception {
        when(ingredientSession.isEmpty()).thenReturn(false);
        when(ingredientSession.getIngredients()).thenReturn(List.of(new Ingredient("Eggs")));

        mockMvc.perform(post("/meal-request/suggest").param("dietaryPreferences", "x".repeat(501)))
                .andExpect(status().isOk())
                .andExpect(view().name("meal-request"));
    }

    @Test
    void shouldRedirectToResultsOnSuccessfulSubmit() throws Exception {
        MealSuggestion meal = new MealSuggestion();
        meal.setName("Omelette");
        meal.setDescription("desc");
        meal.setSteps(List.of("step"));
        MealResponse response = new MealResponse();
        response.setMeals(List.of(meal));

        when(ingredientSession.isEmpty()).thenReturn(false);
        when(ingredientSession.getIngredientNames()).thenReturn(List.of("Eggs"));
        when(mealSuggestionService.suggest(any(), anyString())).thenReturn(CompletableFuture.completedFuture(response));

        mockMvc.perform(post("/meal-request/suggest").param("dietaryPreferences", "vegetarian"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/results"));
    }

    @Test
    void shouldGuardResultsWhenNoFlashResponseProvided() throws Exception {
        mockMvc.perform(get("/results"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/meal-request"));
    }
}

