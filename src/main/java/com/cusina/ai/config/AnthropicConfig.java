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

import java.time.Duration;

@Configuration
@EnableConfigurationProperties({AnthropicProperties.class, AiProviderProperties.class, OllamaProperties.class})
public class AnthropicConfig {

    @Bean
    @ConditionalOnProperty(prefix = "ai", name = "provider", havingValue = "anthropic", matchIfMissing = true)
    public AnthropicClient anthropicClient(AnthropicProperties properties) {
        // Ustaw timeout na 20 sekund dla połączenia HTTP
        // OkHttp timeout: connection, read, write
        return AnthropicOkHttpClient.builder()
                .apiKey(properties.getApiKey())
                .timeout(Duration.ofSeconds(20))
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
