package com.cusina.ai.service;

public interface MealAiClient {
    String requestMealSuggestionsJson(String systemPrompt, String userPrompt) throws Exception;
}

