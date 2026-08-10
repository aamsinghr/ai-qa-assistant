package com.aamsinghr.ai_qa_assistant.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.aamsinghr.ai_qa_assistant.entity.*;
import com.aamsinghr.ai_qa_assistant.service.CodeGeneratorService;
import com.aamsinghr.ai_qa_assistant.service.TestCaseService;
import com.aamsinghr.ai_qa_assistant.service.TestDataService;
import com.aamsinghr.ai_qa_assistant.service.TestReviewerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TestAssistantController.class)
class TestAssistantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TestCaseService testCaseService;

    @MockBean
    private CodeGeneratorService codeGeneratorService;

    @MockBean
    private TestDataService testDataService;

    @MockBean
    private TestReviewerService testReviewerService;

    // --- POST /generate/api-tests ---

    @Test
    void generateApiTests_validRequest_returns200WithSuccess() throws Exception {
        List<ApiTestCase> testCases = List.of(
                new ApiTestCase("Test valid input", "Positive test", Map.of("name", "John"), Map.of("statusCode", 200))
        );
        when(testCaseService.generateApiTests(any(ApiTestRequest.class))).thenReturn(testCases);

        ApiTestRequest request = new ApiTestRequest("/users", "POST", Map.of("name", "string"));

        mockMvc.perform(post("/generate/api-tests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", notNullValue()))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].testName", is("Test valid input")));
    }

    @Test
    void generateApiTests_blankEndpoint_returns400WithFailure() throws Exception {
        when(testCaseService.generateApiTests(any(ApiTestRequest.class)))
                .thenThrow(new IllegalArgumentException("Endpoint cannot be empty"));

        ApiTestRequest request = new ApiTestRequest("", "POST", null);

        mockMvc.perform(post("/generate/api-tests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Endpoint cannot be empty")))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void generateApiTests_blankMethod_returns400WithFailure() throws Exception {
        when(testCaseService.generateApiTests(any(ApiTestRequest.class)))
                .thenThrow(new IllegalArgumentException("Method cannot be empty"));

        ApiTestRequest request = new ApiTestRequest("/users", "", null);

        mockMvc.perform(post("/generate/api-tests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Method cannot be empty")));
    }

    // --- POST /generate/test-data ---

    @Test
    void generateTestData_validRequest_returns200WithSuccess() throws Exception {
        List<Map<String, Object>> testData = List.of(
                Map.of("name", "Alice", "age", 30),
                Map.of("name", "Bob", "age", 25)
        );
        when(testDataService.generateTestData(any(TestDataRequest.class))).thenReturn(testData);

        TestDataRequest request = new TestDataRequest(Map.of("name", "string", "age", "integer"));

        mockMvc.perform(post("/generate/test-data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", notNullValue()))
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    void generateTestData_emptySchema_returns400WithFailure() throws Exception {
        when(testDataService.generateTestData(any(TestDataRequest.class)))
                .thenThrow(new IllegalArgumentException("Schema cannot be null or empty"));

        TestDataRequest request = new TestDataRequest(Map.of());

        mockMvc.perform(post("/generate/test-data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Schema cannot be null or empty")));
    }

    // --- POST /review/tests ---

    @Test
    void reviewTests_validRequest_returns200WithSuccess() throws Exception {
        TestReviewResult reviewResult = new TestReviewResult(
                5, "80%", "Good coverage overall",
                List.of("Edge case for empty input"),
                List.of("Add boundary tests"),
                List.of("Uses descriptive test names")
        );
        when(testReviewerService.reviewTests(any(TestReviewRequest.class))).thenReturn(reviewResult);

        TestReviewRequest request = new TestReviewRequest("JUnit5", "/users", "@Test void test() {}");

        mockMvc.perform(post("/review/tests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", notNullValue()))
                .andExpect(jsonPath("$.data.totalTestsFound", is(5)))
                .andExpect(jsonPath("$.data.coverageScore", is("80%")));
    }

    @Test
    void reviewTests_blankTestCode_returns400WithFailure() throws Exception {
        when(testReviewerService.reviewTests(any(TestReviewRequest.class)))
                .thenThrow(new IllegalArgumentException("Test code cannot be empty"));

        TestReviewRequest request = new TestReviewRequest("JUnit5", "/users", "");

        mockMvc.perform(post("/review/tests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Test code cannot be empty")));
    }

    // --- POST /generate/restassured-tests ---

    @Test
    void generateRestAssuredTests_validRequest_returns200WithSuccess() throws Exception {
        List<ApiTestCase> testCases = List.of(
                new ApiTestCase("Test valid", "Positive", Map.of("id", 1), Map.of("statusCode", 200))
        );
        when(testCaseService.generateApiTests(any(ApiTestRequest.class))).thenReturn(testCases);
        when(codeGeneratorService.generateRestAssuredTests(anyString(), anyString(), anyList()))
                .thenReturn("RestAssured test file generated: UsersApiTests.java");

        ApiTestRequest request = new ApiTestRequest("/users", "GET", Map.of("id", "integer"));

        mockMvc.perform(post("/generate/restassured-tests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", notNullValue()))
                .andExpect(jsonPath("$.data", containsString("RestAssured test file generated")));
    }

    // --- Service exception → 500 ---

    @Test
    void generateApiTests_serviceThrowsRuntimeException_returns500WithSanitizedMessage() throws Exception {
        when(testCaseService.generateApiTests(any(ApiTestRequest.class)))
                .thenThrow(new RuntimeException("AI failed to generate valid test cases after retries"));

        ApiTestRequest request = new ApiTestRequest("/users", "POST", Map.of("name", "string"));

        mockMvc.perform(post("/generate/api-tests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", notNullValue()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void generateTestData_serviceThrowsRuntimeException_returns500() throws Exception {
        when(testDataService.generateTestData(any(TestDataRequest.class)))
                .thenThrow(new RuntimeException("Failed to generate test data"));

        TestDataRequest request = new TestDataRequest(Map.of("name", "string"));

        mockMvc.perform(post("/generate/test-data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Failed to generate test data")));
    }

    @Test
    void serviceException_doesNotExposeInternalDetails() throws Exception {
        String internalMessage = "Error at com.aamsinghr.ai_qa_assistant.service.TestCaseService.generate";
        when(testCaseService.generateApiTests(any(ApiTestRequest.class)))
                .thenThrow(new RuntimeException(internalMessage));

        ApiTestRequest request = new ApiTestRequest("/users", "POST", Map.of("name", "string"));

        mockMvc.perform(post("/generate/api-tests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", not(containsString("com.aamsinghr"))))
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}
