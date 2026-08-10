package com.aamsinghr.ai_qa_assistant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.aamsinghr.ai_qa_assistant.ai.BedrockAiService;
import com.aamsinghr.ai_qa_assistant.ai.PromptTemplates;
import com.aamsinghr.ai_qa_assistant.entity.TestReviewRequest;
import com.aamsinghr.ai_qa_assistant.entity.TestReviewResult;
import org.springframework.stereotype.Service;

@Service
public class TestReviewerService {

    private final BedrockAiService bedrockAiService;
    private final ObjectMapper objectMapper;

    public TestReviewerService(BedrockAiService bedrockAiService) {
        this.bedrockAiService = bedrockAiService;
        this.objectMapper = new ObjectMapper();
    }

    public TestReviewResult reviewTests(TestReviewRequest request) {
        if (request.getTestCode() == null || request.getTestCode().isBlank()) {
            throw new IllegalArgumentException("Test code cannot be empty");
        }

        String framework = request.getFramework() != null ? request.getFramework() : "unknown";
        String endpoint = request.getEndpoint() != null ? request.getEndpoint() : "unknown";

        String prompt = String.format(
                PromptTemplates.TEST_REVIEW_PROMPT,
                framework,
                endpoint,
                request.getTestCode()
        );

        String aiResponse = bedrockAiService.callAi(prompt);

        String json = extractJsonObject(aiResponse);

        try {
            return objectMapper.readValue(json, TestReviewResult.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to review test file: " + e.getMessage(), e);
        }
    }

    private String extractJsonObject(String response) {
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');

        if (start < 0 || end < 0 || end <= start) {
            throw new RuntimeException("AI returned an invalid JSON structure");
        }

        return response.substring(start, end + 1);
    }
}
