package com.cusina.ai.controller;

import com.cusina.ai.config.AnthropicProperties;
import com.cusina.ai.model.Ingredient;
import com.cusina.ai.model.MealResponse;
import com.cusina.ai.model.MealSuggestion;
import com.cusina.ai.service.MealSuggestionService;
import com.cusina.ai.session.IngredientSession;
import com.cusina.ai.session.MealSelectionSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
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

    @MockBean
    private MealSelectionSession mealSelectionSession;

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
                .andExpect(redirectedUrl("/ingredients"))
                .andExpect(flash().attribute("errorMessage", "Najpierw dodaj co najmniej jeden składnik."));
    }

    @Test
    void shouldRenderMealRequestWhenIngredientsPresent() throws Exception {
        when(ingredientSession.isEmpty()).thenReturn(false);
        when(ingredientSession.getIngredients()).thenReturn(List.of(new Ingredient("Eggs")));

        mockMvc.perform(get("/meal-request"))
                .andExpect(status().isOk())
                .andExpect(view().name("meal-request"))
                .andExpect(model().attributeExists("mealRequest", "ingredients", "charLimit", "dishTypes", "dietTypes"));
    }

    @Test
    void shouldReturnValidationErrorWhenPreferencesTooLong() throws Exception {
        when(ingredientSession.isEmpty()).thenReturn(false);
        when(ingredientSession.getIngredients()).thenReturn(List.of(new Ingredient("Eggs")));

        mockMvc.perform(post("/meal-request/suggest").param("dietaryPreferences", "x".repeat(501)))
                .andExpect(status().isOk())
                .andExpect(view().name("meal-request"))
                .andExpect(model().attributeHasFieldErrors("mealRequest", "dietaryPreferences"));
    }

     @Test
     void shouldRedirectToResultsOnSuccessfulSubmit() throws Exception {
         MealSuggestion meal = new MealSuggestion();
         meal.setName("Omelette");
         meal.setDescription("desc");
         meal.setSteps(List.of("step"));
         meal.setUsedIngredients(List.of("Eggs"));
         MealResponse response = new MealResponse();
         response.setRawCount(3);
         response.setMeals(List.of(meal));

         when(ingredientSession.isEmpty()).thenReturn(false);
         when(ingredientSession.getIngredientNames()).thenReturn(List.of("Eggs"));
         when(ingredientSession.getIngredientPromptDetails()).thenReturn(List.of("Eggs (2 szt)"));
         when(mealSuggestionService.suggest(any())).thenReturn(CompletableFuture.completedFuture(response));

         mockMvc.perform(post("/meal-request/suggest")
                         .param("dietaryPreferences", "wegetariańskie")
                         .param("dishType", "zupy")
                         .param("dietType", "wegetariańskie"))
                 .andExpect(status().is3xxRedirection())
                 .andExpect(redirectedUrl("/results"))
                 .andExpect(flash().attributeExists("mealResponse"));

         verify(mealSelectionSession).updateLatestSuggestions(any());
     }

     @Test
     void shouldRejectTamperedComboboxValueWithoutAiCall() throws Exception {
         when(ingredientSession.isEmpty()).thenReturn(false);
         when(ingredientSession.getIngredients()).thenReturn(List.of(new Ingredient("Eggs")));

         mockMvc.perform(post("/meal-request/suggest")
                         .param("dietaryPreferences", "")
                         .param("dishType", "invalid-value")
                         .param("dietType", "lekka"))
                 .andExpect(status().isOk())
                 .andExpect(view().name("meal-request"))
                 .andExpect(model().attributeHasFieldErrors("mealRequest", "dishType"));

         verify(mealSuggestionService, never()).suggest(any());
     }

    @Test
    void shouldRenderResultsWhenFlashMealResponseIsProvided() throws Exception {
        MealResponse response = new MealResponse();
        response.setRawCount(3);
        response.setMeals(List.of());

        mockMvc.perform(get("/results").flashAttr("mealResponse", response))
                .andExpect(status().isOk())
                .andExpect(view().name("results"));
    }

    @Test
    void shouldRenderWarningBreakdownOnResultsPage() throws Exception {
        MealResponse response = new MealResponse();
        response.setRawCount(3);
        response.setMeals(List.of());
        response.setWarningPl("Część sugestii pominięto, ponieważ miały niepełne dane lub wskazywały składniki spoza Twojej listy.");
        response.setOmittedMissingDataCount(1);
        response.setOmittedOutOfScopeCount(2);
        response.setOmittedMalformedCount(3);

        mockMvc.perform(get("/results").flashAttr("mealResponse", response))
                .andExpect(status().isOk())
                .andExpect(view().name("results"))
                .andExpect(content().string(containsString("Niepełne dane przepisu")))
                .andExpect(content().string(containsString("Składniki spoza Twojej listy")));
    }

    @Test
    void shouldGuardResultsWhenNoFlashResponseProvided() throws Exception {
        when(mealSelectionSession.hasLatestSuggestions()).thenReturn(false);

        mockMvc.perform(get("/results"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/meal-request"));
    }

    @Test
    void shouldSelectMealAndRedirectToDetailedRecipe() throws Exception {
        MealSuggestion selected = new MealSuggestion();
        selected.setName("Zupa pomidorowa");
        when(mealSelectionSession.selectFromLatest(0)).thenReturn(Optional.of(selected));

        mockMvc.perform(post("/results/select").param("index", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/meal-selected"))
                .andExpect(flash().attribute("successMessage", "Przepis został dodany do historii."));
    }

    @Test
    void shouldRenderSelectedMealViewWhenSelectionExists() throws Exception {
        MealSuggestion selected = new MealSuggestion();
        selected.setName("Placuszki");
        selected.setDescription("Opis");
        selected.setSteps(List.of("Krok 1"));
        when(mealSelectionSession.getSelectedMeal()).thenReturn(Optional.of(selected));

        mockMvc.perform(get("/meal-selected"))
                .andExpect(status().isOk())
                .andExpect(view().name("meal-selected"))
                .andExpect(model().attributeExists("selectedMeal"));
    }

    @Test
    void shouldGuardSelectedMealWhenNothingChosenYet() throws Exception {
        when(mealSelectionSession.getSelectedMeal()).thenReturn(Optional.empty());

        mockMvc.perform(get("/meal-selected"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/results"));
    }

    @Test
    void shouldRenderMealHistoryWithSummary() throws Exception {
        when(mealSelectionSession.getSelectedMealsHistory()).thenReturn(List.of(new MealSuggestion()));
        when(mealSelectionSession.getSelectionCount()).thenReturn(1);
        when(mealSelectionSession.getUniqueMealCount()).thenReturn(1);

        mockMvc.perform(get("/meal-history"))
                .andExpect(status().isOk())
                .andExpect(view().name("meal-history"))
                .andExpect(model().attribute("selectionCount", 1))
                .andExpect(model().attribute("uniqueMealCount", 1));
    }
}

