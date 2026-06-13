package com.cusina.ai.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppStartupValidatorTest {

    @Test
    void shouldRequireAnthropicApiKeyWhenAnthropicProviderSelected() {
        AnthropicProperties anthropicProperties = new AnthropicProperties();
        anthropicProperties.setApiKey("");

        AiProviderProperties aiProviderProperties = new AiProviderProperties();
        aiProviderProperties.setProvider("anthropic");

        AppStartupValidator validator = new AppStartupValidator(anthropicProperties, aiProviderProperties);

        assertThatThrownBy(() -> validator.onApplicationEvent(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ANTHROPIC_API_KEY");
    }

    @Test
    void shouldSkipAnthropicApiKeyValidationForOllamaProvider() {
        AnthropicProperties anthropicProperties = new AnthropicProperties();
        anthropicProperties.setApiKey("");

        AiProviderProperties aiProviderProperties = new AiProviderProperties();
        aiProviderProperties.setProvider("ollama");

        AppStartupValidator validator = new AppStartupValidator(anthropicProperties, aiProviderProperties);

        assertThatCode(() -> validator.onApplicationEvent(null)).doesNotThrowAnyException();
    }

    @Test
    void shouldFailWhenMealCountIsDifferentThanThree() {
        AnthropicProperties anthropicProperties = new AnthropicProperties();
        anthropicProperties.setApiKey("key");
        anthropicProperties.setMealCount(2);

        AiProviderProperties aiProviderProperties = new AiProviderProperties();
        aiProviderProperties.setProvider("anthropic");

        AppStartupValidator validator = new AppStartupValidator(anthropicProperties, aiProviderProperties);

        assertThatThrownBy(() -> validator.onApplicationEvent(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("meal-count");
    }
}

