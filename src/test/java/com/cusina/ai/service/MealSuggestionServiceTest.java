package com.cusina.ai.service;

import com.cusina.ai.config.AnthropicProperties;
import com.cusina.ai.model.MealResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MealSuggestionServiceTest {

    private MealAiClient mealAiClient;
    private MealSuggestionService service;

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
                .thenReturn("{\"meals\":[{\"name\":\"A\",\"description\":\"d\",\"steps\":[\"s\"]},{\"name\":\"B\",\"description\":\"d\",\"steps\":[\"s\"]}]}");

        MealResponse response = service.suggest(List.of("egg"), "none").get();

        assertThat(response.hasError()).isTrue();
        assertThat(response.getError()).contains("validate");
    }

    @Test
    void shouldDropMalformedSuggestionsAndSetWarning() throws Exception {
        when(mealAiClient.requestMealSuggestionsJson(anyString(), anyString()))
                .thenReturn("{\"meals\":[{" +
                        "\"name\":\"Valid One\",\"description\":\"Good\",\"steps\":[\"Step 1\"]},{" +
                        "\"name\":\"\",\"description\":\"Broken\",\"steps\":[\"Step\"]},{" +
                        "\"name\":\"Valid Two\",\"description\":\"Also good\",\"steps\":[\"Step 1\",\"Step 2\"]}]}" );

        MealResponse response = service.suggest(List.of("egg"), "none").get();

        assertThat(response.hasError()).isFalse();
        assertThat(response.getMeals()).hasSize(2);
        assertThat(response.getOmittedMalformedCount()).isEqualTo(1);
        assertThat(response.hasWarning()).isTrue();
    }

    @Test
    void shouldMapJsonParsingFailureToFriendlyError() throws Exception {
        when(mealAiClient.requestMealSuggestionsJson(anyString(), anyString())).thenReturn("not-json");

        MealResponse response = service.suggest(List.of("egg"), "none").get();

        assertThat(response.hasError()).isTrue();
        assertThat(response.getError()).contains("process");
    }

    @Test
    void shouldParseSuggestionsWhenJsonIsInsideMarkdownCodeFence() throws Exception {
        when(mealAiClient.requestMealSuggestionsJson(anyString(), anyString())).thenReturn("""
                Here are your suggestions:
                ```json
                {"meals":[
                  {"name":"Meal A","description":"desc","steps":["Step 1"]},
                  {"name":"Meal B","description":"desc","steps":["Step 1"]},
                  {"name":"","description":"broken","steps":["Step 1"]}
                ]}
                ```
                """);

        MealResponse response = service.suggest(List.of("egg"), "none").get();

        assertThat(response.hasError()).isFalse();
        assertThat(response.getMeals()).hasSize(2);
        assertThat(response.getOmittedMalformedCount()).isEqualTo(1);
        assertThat(response.hasWarning()).isTrue();
    }

    @Test
    void shouldParseSuggestionsWhenJsonHasLeadingAndTrailingText() throws Exception {
        when(mealAiClient.requestMealSuggestionsJson(anyString(), anyString())).thenReturn(
                "AI response: {\"meals\":[{\"name\":\"A\",\"description\":\"d\",\"steps\":[\"s\"]},{\"name\":\"B\",\"description\":\"d\",\"steps\":[\"s\"]},{\"name\":\"C\",\"description\":\"d\",\"steps\":[\"s\"]}]} end.");

        MealResponse response = service.suggest(List.of("egg"), "none").get();

        assertThat(response.hasError()).isFalse();
        assertThat(response.getMeals()).hasSize(3);
    }
}

