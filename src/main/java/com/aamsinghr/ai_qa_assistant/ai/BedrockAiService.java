package com.aamsinghr.ai_qa_assistant.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

@Service
public class BedrockAiService {

    private static final int MAX_PROMPT_LENGTH = 50_000;

    private final BedrockRuntimeClient bedrockClient;
    private final ObjectMapper objectMapper;
    private final String modelId;
    private final int maxTokens;

    public BedrockAiService(
            BedrockRuntimeClient bedrockClient,
            @Value("${aws.bedrock.model-id}") String modelId,
            @Value("${aws.bedrock.max-tokens:4096}") int maxTokens) {
        this.bedrockClient = bedrockClient;
        this.objectMapper = new ObjectMapper();
        this.modelId = modelId;
        this.maxTokens = maxTokens;
    }

    /**
     * Sends a prompt to Claude on Amazon Bedrock and returns the text from the first content block.
     *
     * @param prompt Non-null, non-blank string, max 50,000 characters
     * @return AI response text
     * @throws RuntimeException if prompt is empty/blank, exceeds max length,
     *         Bedrock returns an error, or request times out
     */
    public String callAi(String prompt) {
        validatePrompt(prompt);

        try {
            String requestPayload = buildRequestPayload(prompt);

            InvokeModelRequest invokeRequest = InvokeModelRequest.builder()
                    .modelId(modelId)
                    .contentType("application/json")
                    .accept("application/json")
                    .body(SdkBytes.fromUtf8String(requestPayload))
                    .build();

            InvokeModelResponse response = bedrockClient.invokeModel(invokeRequest);

            return extractResponseText(response);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && (e.getMessage().contains("Prompt must not be empty")
                    || e.getMessage().contains("Prompt exceeds maximum length"))) {
                throw e;
            }
            throw new RuntimeException(String.format(
                    "Bedrock API error [type=%s, modelId=%s]: %s",
                    e.getClass().getSimpleName(),
                    modelId,
                    e.getMessage() != null ? e.getMessage() : "Unknown failure"), e);
        }
    }

    private void validatePrompt(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            throw new RuntimeException("Prompt must not be empty");
        }
        if (prompt.length() > MAX_PROMPT_LENGTH) {
            throw new RuntimeException(
                    "Prompt exceeds maximum length of " + MAX_PROMPT_LENGTH + " characters");
        }
    }

    private String buildRequestPayload(String prompt) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("anthropic_version", "bedrock-2023-05-31");
            root.put("max_tokens", maxTokens);

            ArrayNode messages = objectMapper.createArrayNode();
            ObjectNode message = objectMapper.createObjectNode();
            message.put("role", "user");

            ArrayNode content = objectMapper.createArrayNode();
            ObjectNode textBlock = objectMapper.createObjectNode();
            textBlock.put("type", "text");
            textBlock.put("text", prompt);
            content.add(textBlock);

            message.set("content", content);
            messages.add(message);
            root.set("messages", messages);

            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build Bedrock request payload: " + e.getMessage(), e);
        }
    }

    private String extractResponseText(InvokeModelResponse response) {
        try {
            String responseBody = response.body().asUtf8String();
            JsonNode responseJson = objectMapper.readTree(responseBody);
            JsonNode contentArray = responseJson.get("content");

            if (contentArray == null || !contentArray.isArray() || contentArray.isEmpty()) {
                throw new RuntimeException("Bedrock response contains no content blocks");
            }

            JsonNode firstBlock = contentArray.get(0);
            JsonNode textNode = firstBlock.get("text");

            if (textNode == null) {
                throw new RuntimeException("Bedrock response first content block has no text field");
            }

            return textNode.asText();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Bedrock response: " + e.getMessage(), e);
        }
    }
}
