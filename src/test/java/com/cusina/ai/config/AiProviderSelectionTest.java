package com.cusina.ai.config;

import com.anthropic.client.AnthropicClient;
import com.cusina.ai.service.AnthropicMealAiClient;
import com.cusina.ai.service.MealAiClient;
import com.cusina.ai.service.OllamaMealAiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class AiProviderSelectionTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(AnthropicConfig.class)
            .withBean(ObjectMapper.class, ObjectMapper::new);

    @Test
    void shouldUseAnthropicClientByDefault() {
        contextRunner
                .withPropertyValues("anthropic.api-key=test-key")
                .run(context -> {
            assertThat(context).hasSingleBean(MealAiClient.class);
            assertThat(context.getBean(MealAiClient.class)).isInstanceOf(AnthropicMealAiClient.class);
            assertThat(context).hasSingleBean(AnthropicClient.class);
        });
    }

    @Test
    void shouldUseOllamaClientWhenProviderIsExplicitlySet() {
        contextRunner
                .withPropertyValues(
                        "ai.provider=ollama"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(MealAiClient.class);
                    assertThat(context.getBean(MealAiClient.class)).isInstanceOf(OllamaMealAiClient.class);
                    assertThat(context).doesNotHaveBean(AnthropicClient.class);
                });
    }
}



