package com.cusina.ai.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.MessageCreateParams;
import com.cusina.ai.config.AnthropicProperties;

public class AnthropicMealAiClient implements MealAiClient {

    private final AnthropicClient anthropicClient;
    private final AnthropicProperties properties;

    public AnthropicMealAiClient(AnthropicClient anthropicClient, AnthropicProperties properties) {
        this.anthropicClient = anthropicClient;
        this.properties = properties;
    }

    @Override
    public String requestMealSuggestionsJson(String systemPrompt, String userPrompt) {
        MessageCreateParams params = MessageCreateParams.builder()
                .model(properties.getModel())
                .maxTokens(properties.getMaxTokens())
                .system(systemPrompt)
                .addUserMessage(userPrompt)
                .build();

        return anthropicClient.messages().create(params).content().stream()
                .filter(ContentBlock::isText)
                .map(block -> block.asText().text())
                .reduce("", String::concat)
                .trim();
    }
}
