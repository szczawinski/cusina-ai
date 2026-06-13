package com.cusina.ai.config;

import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class AppStartupValidator implements ApplicationListener<ApplicationStartedEvent> {

    private final AnthropicProperties properties;
    private final AiProviderProperties aiProviderProperties;

    public AppStartupValidator(AnthropicProperties properties, AiProviderProperties aiProviderProperties) {
        this.properties = properties;
        this.aiProviderProperties = aiProviderProperties;
    }

    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        if ("anthropic".equalsIgnoreCase(aiProviderProperties.getProvider())
                && (properties.getApiKey() == null || properties.getApiKey().isBlank())) {
            throw new IllegalStateException("Brak ANTHROPIC_API_KEY. Aplikacja nie może wystartować.");
        }

        if (properties.getTimeoutSeconds() <= 0) {
            throw new IllegalStateException("anthropic.timeout-seconds musi być większe od 0.");
        }

        if (properties.getMealCount() != 3) {
            throw new IllegalStateException("anthropic.meal-count musi mieć wartość 3.");
        }
    }
}

