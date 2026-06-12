package com.cusina.ai.service;

import com.cusina.ai.config.OllamaProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OllamaMealAiClientTest {

    private OllamaMealAiClient client;

    @BeforeEach
    void setUp() {
        OllamaProperties properties = new OllamaProperties();
        properties.setModel("llama3.1");
        client = new OllamaMealAiClient(properties, new ObjectMapper());
    }

    @Test
    void shouldBuildExpectedOllamaChatRequestBody() {
        Map<String, Object> body = client.buildRequestBody("system", "user");

        assertThat(body.get("model")).isEqualTo("llama3.1");
        assertThat(body.get("stream")).isEqualTo(false);
        assertThat(body.get("messages")).isInstanceOf(List.class);

        @SuppressWarnings("unchecked")
        List<Map<String, String>> messages = (List<Map<String, String>>) body.get("messages");
        assertThat(messages).hasSize(2);
        assertThat(messages.get(0)).containsEntry("role", "system").containsEntry("content", "system");
        assertThat(messages.get(1)).containsEntry("role", "user").containsEntry("content", "user");
    }

    @Test
    void shouldParseChatApiMessageContent() throws Exception {
        String content = client.extractAssistantContent("""
                {
                  "model":"llama3.1",
                  "message":{"role":"assistant","content":"meal-json"}
                }
                """);

        assertThat(content).isEqualTo("meal-json");
    }

    @Test
    void shouldFallbackToGenerateApiResponseField() throws Exception {
        String content = client.extractAssistantContent("""
                {
                  "model":"llama3.1",
                  "response":"meal-json"
                }
                """);

        assertThat(content).isEqualTo("meal-json");
    }
}


