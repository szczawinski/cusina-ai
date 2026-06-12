package com.cusina.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "anthropic")
public class AnthropicProperties {

    private String apiKey;
    private String model = "claude-haiku-4-5";
    private int maxTokens = 2048;
    private int timeoutSeconds = 30;
    private int mealCount = 3;
    private int dietaryPreferenceMaxLength = 500;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public int getMealCount() {
        return mealCount;
    }

    public void setMealCount(int mealCount) {
        this.mealCount = mealCount;
    }

    public int getDietaryPreferenceMaxLength() {
        return dietaryPreferenceMaxLength;
    }

    public void setDietaryPreferenceMaxLength(int dietaryPreferenceMaxLength) {
        this.dietaryPreferenceMaxLength = dietaryPreferenceMaxLength;
    }
}

