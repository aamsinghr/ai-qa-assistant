package com.aamsinghr.ai_qa_assistant.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.aamsinghr.ai_qa_assistant.ai.BedrockAiService;
import com.aamsinghr.ai_qa_assistant.ai.PromptTemplates;
import com.aamsinghr.ai_qa_assistant.entity.TestDataRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class TestDataService {

    private final BedrockAiService aiService;
    private final ObjectMapper objectMapper;

    public TestDataService(BedrockAiService aiService) {
        this.aiService = aiService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Generates 5 test data objects from a schema definition.
     *
     * @param request Must have non-null, non-empty schema map
     * @return List of 5 test data maps
     * @throws IllegalArgumentException if schema is null or empty
     * @throws RuntimeException if AI response cannot be parsed
     */
    public List<Map<String, Object>> generateTestData(TestDataRequest request) {
        validateSchema(request);

        try {
            String schemaJson = objectMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(request.getSchema());

            String prompt = String.format(
                    PromptTemplates.TEST_DATA_PROMPT,
                    5,
                    schemaJson
            );

            String aiResponse = aiService.callAi(prompt);

            String json = extractJsonArray(aiResponse);

            return objectMapper.readValue(
                    json,
                    new TypeReference<List<Map<String, Object>>>() {}
            );
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate test data", e);
        }
    }

    private void validateSchema(TestDataRequest request) {
        if (request.getSchema() == null || request.getSchema().isEmpty()) {
            throw new IllegalArgumentException("Schema cannot be null or empty");
        }
    }

    private String extractJsonArray(String response) {
        int start = response.indexOf('[');
        int end = response.lastIndexOf(']');

        if (start == -1 || end == -1 || end <= start) {
            throw new RuntimeException("Failed to generate test data");
        }

        return response.substring(start, end + 1);
    }
}
