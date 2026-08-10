package com.aamsinghr.ai_qa_assistant.controller;

import com.aamsinghr.ai_qa_assistant.entity.ApiResponse;
import com.aamsinghr.ai_qa_assistant.entity.ApiTestCase;
import com.aamsinghr.ai_qa_assistant.entity.ApiTestRequest;
import com.aamsinghr.ai_qa_assistant.entity.TestDataRequest;
import com.aamsinghr.ai_qa_assistant.entity.TestReviewRequest;
import com.aamsinghr.ai_qa_assistant.entity.TestReviewResult;
import com.aamsinghr.ai_qa_assistant.service.CodeGeneratorService;
import com.aamsinghr.ai_qa_assistant.service.TestCaseService;
import com.aamsinghr.ai_qa_assistant.service.TestDataService;
import com.aamsinghr.ai_qa_assistant.service.TestReviewerService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class TestAssistantController {

    private final TestCaseService testCaseService;
    private final CodeGeneratorService codeGeneratorService;
    private final TestDataService testDataService;
    private final TestReviewerService testReviewerService;

    public TestAssistantController(TestCaseService testCaseService,
                                   CodeGeneratorService codeGeneratorService,
                                   TestDataService testDataService,
                                   TestReviewerService testReviewerService) {
        this.testCaseService = testCaseService;
        this.codeGeneratorService = codeGeneratorService;
        this.testDataService = testDataService;
        this.testReviewerService = testReviewerService;
    }

    @PostMapping("/generate/api-tests")
    public ApiResponse<List<ApiTestCase>> generateApiTests(
            @RequestBody ApiTestRequest request) {

        List<ApiTestCase> testCases = testCaseService.generateApiTests(request);
        return ApiResponse.success("Test cases generated successfully", testCases);
    }

    @PostMapping("/generate/test-data")
    public ApiResponse<List<Map<String, Object>>> generateTestData(
            @RequestBody TestDataRequest request) {

        List<Map<String, Object>> data = testDataService.generateTestData(request);
        return ApiResponse.success("Test data generated successfully", data);
    }

    @PostMapping("/generate/restassured-tests")
    public ApiResponse<String> generateRestAssuredTests(
            @RequestBody ApiTestRequest request) {

        List<ApiTestCase> testCases = testCaseService.generateApiTests(request);
        String result = codeGeneratorService.generateRestAssuredTests(
                request.getEndpoint(),
                request.getMethod(),
                testCases
        );
        return ApiResponse.success("RestAssured test file generated successfully", result);
    }

    @PostMapping("/review/tests")
    public ApiResponse<TestReviewResult> reviewTests(
            @RequestBody TestReviewRequest request) {

        TestReviewResult result = testReviewerService.reviewTests(request);
        return ApiResponse.success("Test review completed successfully", result);
    }
}
