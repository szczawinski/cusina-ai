package com.cusina.ai.config;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.cusina.ai.service.AnthropicMealAiClient;
import com.cusina.ai.service.MealAiClient;
import com.cusina.ai.service.OllamaMealAiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({AnthropicProperties.class, AiProviderProperties.class, OllamaProperties.class})
public class AnthropicConfig {

    @Bean
    @ConditionalOnProperty(prefix = "ai", name = "provider", havingValue = "anthropic", matchIfMissing = true)
    public AnthropicClient anthropicClient(AnthropicProperties properties) {
        return AnthropicOkHttpClient.builder()
                .apiKey(properties.getApiKey())
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "ai", name = "provider", havingValue = "anthropic", matchIfMissing = true)
    public MealAiClient anthropicMealAiClient(AnthropicClient anthropicClient, AnthropicProperties properties) {
        return new AnthropicMealAiClient(anthropicClient, properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "ai", name = "provider", havingValue = "ollama")
    public MealAiClient ollamaMealAiClient(OllamaProperties properties, ObjectMapper objectMapper) {
        return new OllamaMealAiClient(properties, objectMapper);
    }
}
