package com.cusina.ai.service;

import com.cusina.ai.config.AnthropicProperties;
import com.cusina.ai.model.DietType;
import com.cusina.ai.model.DishType;
import com.cusina.ai.model.MealRequest;
import com.cusina.ai.model.MealResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MealSuggestionServiceTest {

    private MealAiClient mealAiClient;
    private MealSuggestionService service;

    private MealRequest requestWithIngredients() {
        MealRequest request = new MealRequest();
        request.setIngredients(List.of("jajko", "pomidor", "ser", "cebula"));
        request.setIngredientDetails(List.of("jajko (6 szt)", "pomidor (2 szt)", "ser (200 g)", "cebula (1 szt)"));
        request.setDietaryPreferences("bez orzechów");
        return request;
    }

    @BeforeEach
    void setUp() {
        mealAiClient = mock(MealAiClient.class);
        AnthropicProperties properties = new AnthropicProperties();
        properties.setMealCount(3);
        service = new MealSuggestionService(mealAiClient, new ObjectMapper(), properties);
    }

    @Test
    void shouldReturnErrorWhenRawCountIsNotExactlyThree() throws Exception {
        when(mealAiClient.requestMealSuggestionsJson(anyString(), anyString()))
                .thenReturn("{\"meals\":[{\"name\":\"A\",\"description\":\"danie z pomidorem\",\"steps\":[\"s\"],\"usedIngredients\":[\"pomidor\"]},{\"name\":\"B\",\"description\":\"danie z serem\",\"steps\":[\"s\"],\"usedIngredients\":[\"ser\"]}]}");

        MealResponse response = service.suggest(requestWithIngredients()).get();

        assertThat(response.hasError()).isTrue();
        assertThat(response.getErrorPl()).contains("dokładnie 3");
    }

    @Test
    void shouldDropMalformedSuggestionsAndSetWarning() throws Exception {
        when(mealAiClient.requestMealSuggestionsJson(anyString(), anyString()))
                .thenReturn("{\"meals\":[{" +
                        "\"name\":\"Danie pierwsze\",\"description\":\"Smaczne danie z warzywami\",\"steps\":[\"Dodaj składniki\"],\"usedIngredients\":[\"pomidor\"]},{" +
                        "\"name\":\"\",\"description\":\"Broken\",\"steps\":[\"Step\"],\"usedIngredients\":[\"pomidor\"]},{" +
                        "\"name\":\"Danie drugie\",\"description\":\"Aromatyczne danie z makaronem\",\"steps\":[\"Podsmaż\",\"Podawaj\"],\"usedIngredients\":[\"jajko\",\"cebula\"]}]}" );

        MealResponse response = service.suggest(requestWithIngredients()).get();

        assertThat(response.hasError()).isFalse();
        assertThat(response.getMeals()).hasSize(2);
        assertThat(response.getRawCount()).isEqualTo(3);
        assertThat(response.getOmittedMalformedCount()).isEqualTo(1);
        assertThat(response.hasWarning()).isTrue();
        assertThat(response.getWarningPl()).contains("pominięto");
    }

    @Test
    void shouldMapJsonParsingFailureToFriendlyError() throws Exception {
        when(mealAiClient.requestMealSuggestionsJson(anyString(), anyString())).thenReturn("not-json");

        MealResponse response = service.suggest(requestWithIngredients()).get();

        assertThat(response.hasError()).isTrue();
        assertThat(response.getErrorPl()).contains("przetworzyć");
    }

    @Test
    void shouldParseSuggestionsWhenJsonIsInsideMarkdownCodeFence() throws Exception {
        when(mealAiClient.requestMealSuggestionsJson(anyString(), anyString())).thenReturn("""
                Here are your suggestions:
                ```json
                {"meals":[
                  {"name":"Danie A","description":"Smaczne danie z cebulą","steps":["Dodaj składniki"],"usedIngredients":["cebula"]},
                  {"name":"Danie B","description":"Aromatyczne danie z warzywami","steps":["Podsmaż i podawaj"],"usedIngredients":["pomidor"]},
                  {"name":"","description":"broken","steps":["Step 1"],"usedIngredients":["pomidor"]}
                ]}
                ```
                """);

        MealResponse response = service.suggest(requestWithIngredients()).get();

        assertThat(response.hasError()).isFalse();
        assertThat(response.getMeals()).hasSize(2);
        assertThat(response.getOmittedMalformedCount()).isEqualTo(1);
        assertThat(response.hasWarning()).isTrue();
    }

    @Test
    void shouldParseSuggestionsWhenJsonHasLeadingAndTrailingText() throws Exception {
        when(mealAiClient.requestMealSuggestionsJson(anyString(), anyString())).thenReturn(
                "AI response: {\"meals\":[{\"name\":\"A\",\"description\":\"danie z cebulą\",\"steps\":[\"Dodaj\"],\"usedIngredients\":[\"cebula\"]},{\"name\":\"B\",\"description\":\"danie z serem\",\"steps\":[\"Podgrzej\"],\"usedIngredients\":[\"ser\"]},{\"name\":\"C\",\"description\":\"danie z ryżem\",\"steps\":[\"Podawaj\"],\"usedIngredients\":[\"jajko\"]}]} end.");

        MealResponse response = service.suggest(requestWithIngredients()).get();

        assertThat(response.hasError()).isFalse();
        assertThat(response.getMeals()).hasSize(3);
    }

    @Test
    void shouldRejectClearlyNonPolishAiOutput() throws Exception {
        when(mealAiClient.requestMealSuggestionsJson(anyString(), anyString())).thenReturn("""
                {"meals":[
                  {"name":"Meal A","description":"Quick dinner","steps":["Mix"],"usedIngredients":["jajko"]},
                  {"name":"Meal B","description":"Simple lunch","steps":["Cook"],"usedIngredients":["pomidor"]},
                  {"name":"Meal C","description":"Healthy bowl","steps":["Serve"],"usedIngredients":["ser"]}
                ]}
                """);

        MealResponse response = service.suggest(requestWithIngredients()).get();

        assertThat(response.hasError()).isTrue();
        assertThat(response.getErrorPl()).contains("języku polskim");
    }

    @Test
    void shouldAcceptSuggestionUsingNonEmptySubsetOfIngredients() throws Exception {
        when(mealAiClient.requestMealSuggestionsJson(anyString(), anyString())).thenReturn("""
                {"meals":[
                  {"name":"A","description":"danie z pomidorem","steps":["Dodaj"],"usedIngredients":["pomidor"]},
                  {"name":"B","description":"danie z jajkiem","steps":["Smaż"],"usedIngredients":["jajko"]},
                  {"name":"C","description":"danie z serem","steps":["Podawaj"],"usedIngredients":["ser"]}
                ]}
                """);

        MealResponse response = service.suggest(requestWithIngredients()).get();

        assertThat(response.hasError()).isFalse();
        assertThat(response.getMeals()).hasSize(3);
    }

    @Test
    void shouldRejectSuggestionsUsingIngredientsOutsideRequestScope() throws Exception {
        when(mealAiClient.requestMealSuggestionsJson(anyString(), anyString())).thenReturn("""
                {"meals":[
                  {"name":"A","description":"danie z pomidorem","steps":["Dodaj"],"usedIngredients":["pomidor"]},
                  {"name":"B","description":"danie z mango","steps":["Smaż"],"usedIngredients":["mango"]},
                  {"name":"C","description":"danie z serem","steps":["Podawaj"],"usedIngredients":["ser"]}
                ]}
                """);

        MealResponse response = service.suggest(requestWithIngredients()).get();

        assertThat(response.hasError()).isFalse();
        assertThat(response.getMeals()).hasSize(2);
        assertThat(response.getOmittedMalformedCount()).isEqualTo(1);
    }

    @Test
    void shouldIncludeDishTypeAndDietTypeInPrompt() throws Exception {
        MealRequest request = requestWithIngredients();
        request.setDishType(DishType.LUNCHE);
        request.setDietType(DietType.WEGETARIAŃSKIE);

        when(mealAiClient.requestMealSuggestionsJson(anyString(), anyString())).thenReturn("""
                {"meals":[
                  {"name":"A","description":"danie z pomidorem","steps":["Dodaj"],"usedIngredients":["pomidor"]},
                  {"name":"B","description":"danie z jajkiem","steps":["Smaż"],"usedIngredients":["jajko"]},
                  {"name":"C","description":"danie z serem","steps":["Podawaj"],"usedIngredients":["ser"]}
                ]}
                """);

        service.suggest(request).get();

        verify(mealAiClient).requestMealSuggestionsJson(anyString(), argThat(prompt ->
                prompt.contains("Typ dania (constraint): lunche")
                        && prompt.contains("Typ diety (constraint): wegetariańskie")
                        && prompt.contains("jajko (6 szt)")));
    }
}

