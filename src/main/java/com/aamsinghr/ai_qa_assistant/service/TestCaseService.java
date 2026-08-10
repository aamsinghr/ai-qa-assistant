package com.aamsinghr.ai_qa_assistant.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.aamsinghr.ai_qa_assistant.ai.BedrockAiService;
import com.aamsinghr.ai_qa_assistant.ai.PromptTemplates;
import com.aamsinghr.ai_qa_assistant.entity.ApiTestCase;
import com.aamsinghr.ai_qa_assistant.entity.ApiTestRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TestCaseService {

    private static final Logger log = LoggerFactory.getLogger(TestCaseService.class);
    private static final int MAX_ATTEMPTS = 3;

    private final BedrockAiService aiService;
    private final ObjectMapper objectMapper;

    public TestCaseService(BedrockAiService aiService) {
        this.aiService = aiService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Generates API test cases from endpoint metadata.
     * Retries up to 3 total attempts on parse failure.
     *
     * @param request Must have non-blank endpoint and method
     * @return List of test cases
     * @throws IllegalArgumentException if endpoint or method is blank
     * @throws RuntimeException after 3 failed parse attempts
     */
    public List<ApiTestCase> generateApiTests(ApiTestRequest request) {
        validateRequest(request);

        int attempt = 0;

        while (attempt < MAX_ATTEMPTS) {
            try {
                String schemaJson = objectMapper
                        .writerWithDefaultPrettyPrinter()
                        .writeValueAsString(request.getSchema());

                String prompt = String.format(
                        PromptTemplates.API_TEST_PROMPT,
                        request.getEndpoint(),
                        request.getMethod(),
                        schemaJson
                );

                String aiResponse = aiService.callAi(prompt);

                String jsonArray = extractJsonArray(aiResponse);

                return objectMapper.readValue(jsonArray, new TypeReference<List<ApiTestCase>>() {});

            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                attempt++;
                log.error("Attempt {}/{} failed: {}", attempt, MAX_ATTEMPTS, e.getMessage(), e);
                if (attempt >= MAX_ATTEMPTS) {
                    throw new RuntimeException("AI failed to generate valid test cases after retries: " + e.getMessage(), e);
                }
            }
        }

        throw new RuntimeException("AI failed to generate valid test cases after retries");
    }

    private void validateRequest(ApiTestRequest request) {
        if (request.getEndpoint() == null || request.getEndpoint().isBlank()) {
            throw new IllegalArgumentException("Endpoint cannot be empty");
        }
        if (request.getMethod() == null || request.getMethod().isBlank()) {
            throw new IllegalArgumentException("Method cannot be empty");
        }
    }

    private String extractJsonArray(String aiResponse) {
        int start = aiResponse.indexOf('[');
        int end = aiResponse.lastIndexOf(']');

        if (start < 0 || end < 0) {
            throw new RuntimeException("AI response does not contain a valid JSON array");
        }

        return aiResponse.substring(start, end + 1);
    }
}
