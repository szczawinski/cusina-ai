package com.cusina.ai.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class AppStartupValidator implements ApplicationListener<ApplicationReadyEvent> {

    private final AnthropicProperties properties;
    private final AiProviderProperties aiProviderProperties;

    public AppStartupValidator(AnthropicProperties properties, AiProviderProperties aiProviderProperties) {
        this.properties = properties;
        this.aiProviderProperties = aiProviderProperties;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if ("anthropic".equalsIgnoreCase(aiProviderProperties.getProvider())
                && (properties.getApiKey() == null || properties.getApiKey().isBlank())) {
            throw new IllegalStateException("ANTHROPIC_API_KEY environment variable is not set. Application cannot start.");
        }
    }
}

