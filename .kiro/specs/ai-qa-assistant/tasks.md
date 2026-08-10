# Implementation Plan: AI QA Assistant

## Overview

Implement the AI QA Assistant using Amazon Bedrock (Claude model) with the AWS SDK for Java v2, add input validation, retry logic, structured error handling, and comprehensive property-based testing with jqwik. The implementation follows the existing Spring Boot 3.3.x project structure and builds incrementally from the AI layer up through services, controller, and tests.

## Tasks

- [ ] 1. Update project dependencies and configuration
  - [x] 1.1 Add AWS SDK Bedrock Runtime and jqwik dependencies to pom.xml
    - Add `software.amazon.awssdk:bedrockruntime` compile-scope dependency with pinned version
    - Add `net.jqwik:jqwik` test-scope dependency with pinned version
    - Add `aws.bedrock.region`, `aws.bedrock.model-id`, `aws.bedrock.timeout`, `aws.bedrock.max-tokens` properties to application.properties
    - Ensure `test.output.path` and `server.port` defaults are present
    - _Requirements: 8.1, 8.2, 8.3, 8.5, 10.4, 10.5, 10.6_

- [x] 2. Implement BedrockAiService (AI Layer)
  - [x] 2.1 Create BedrockAiService replacing existing AiService
    - Create `BedrockAiService.java` in `com.aamsinghr.ai_qa_assistant.ai` package
    - Inject `BedrockRuntimeClient` configured with region from properties
    - Implement `callAi(String prompt)` method using Anthropic Messages API format
    - Add input validation: reject null/blank prompts, reject prompts > 50,000 chars
    - Add timeout handling using configured `aws.bedrock.timeout` property (default 30s)
    - Configure `max_tokens` from `aws.bedrock.max-tokens` property (default 4096)
    - Extract text from first content block of model response
    - Wrap Bedrock errors in RuntimeException with error type, model ID, and failure reason
    - Use `@Service` annotation and constructor injection
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9_

  - [ ]* 2.2 Write property test for prompt input validation (Property 1)
    - **Property 1: Prompt input validation rejects invalid prompts**
    - Create `BedrockAiServicePropertyTest.java` in test `ai` package
    - Use jqwik `@Property` to generate null, blank, and whitespace-only strings → assert RuntimeException thrown without Bedrock call
    - Generate strings > 50,000 chars → assert RuntimeException with length message
    - **Validates: Requirements 1.8, 1.9**

  - [ ]* 2.3 Write property test for Bedrock error context (Property 2)
    - **Property 2: Bedrock error exceptions contain required context**
    - Mock BedrockRuntimeClient to throw various SDK exceptions
    - Assert thrown RuntimeException message contains error type, model ID, and failure reason
    - **Validates: Requirements 1.5**

  - [ ]* 2.4 Write unit tests for BedrockAiService
    - Test successful invocation with mocked Bedrock client
    - Test response content block parsing
    - Test timeout handling
    - _Requirements: 1.3, 1.5, 1.6_

- [x] 3. Checkpoint - Ensure AI layer compiles and tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 4. Implement service layer with validation and retry logic
  - [x] 4.1 Update TestCaseService to use BedrockAiService
    - Replace `AiService` dependency with `BedrockAiService`
    - Retain existing validation logic (endpoint/method blank checks)
    - Retain retry logic (3 total attempts) on parse failure
    - Update JSON extraction to use `[` and `]` delimiter search
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8_

  - [ ]* 4.2 Write property test for test case request validation (Property 3)
    - **Property 3: Test case request validation rejects blank fields**
    - Use jqwik to generate ApiTestRequest with null/blank endpoint → assert IllegalArgumentException "Endpoint cannot be empty"
    - Generate ApiTestRequest with valid endpoint but null/blank method → assert IllegalArgumentException "Method cannot be empty"
    - **Validates: Requirements 2.2, 2.3**

  - [ ]* 4.3 Write property test for JSON array extraction (Property 4)
    - **Property 4: JSON array extraction round-trip**
    - Generate valid JSON arrays wrapped with random prefix/suffix text → assert extraction produces parseable JSON array
    - Generate strings with no `[` or no `]` → assert parse failure signaled
    - **Validates: Requirements 2.7, 3.3**

  - [x] 4.4 Update TestDataService to use BedrockAiService with validation
    - Replace `AiService` dependency with `BedrockAiService`
    - Add schema validation: reject null or empty schema map with descriptive error
    - Retain JSON array extraction logic
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

  - [ ]* 4.5 Write property test for schema validation (Property 6)
    - **Property 6: Schema validation rejects null and empty maps**
    - Use jqwik to generate TestDataRequest with null schema → assert error
    - Generate TestDataRequest with empty map schema → assert error indicating non-empty schema required
    - **Validates: Requirements 3.5**

  - [x] 4.6 Update TestReviewerService to use BedrockAiService
    - Replace `AiService` dependency with `BedrockAiService`
    - Retain testCode blank validation
    - Retain null framework/endpoint substitution with "unknown"
    - Retain JSON object extraction using `{` and `}` delimiters
    - Add validation that `}` appears after `{` in response
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6_

  - [ ]* 4.7 Write property test for JSON object extraction (Property 5)
    - **Property 5: JSON object extraction round-trip**
    - Generate valid JSON objects wrapped with random prefix/suffix → assert extraction produces parseable JSON object
    - Generate strings where `}` does not appear after `{` → assert RuntimeException thrown
    - **Validates: Requirements 5.4, 5.5**

  - [ ]* 4.8 Write property test for test review input validation (Property 10)
    - **Property 10: Test review input validation rejects blank test code**
    - Generate TestReviewRequest with null/blank testCode → assert IllegalArgumentException "Test code cannot be empty"
    - **Validates: Requirements 5.2**

- [x] 5. Checkpoint - Ensure service layer compiles and tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 6. Implement CodeGeneratorService enhancements
  - [x] 6.1 Update CodeGeneratorService to use BedrockAiService reference and add directory creation
    - Ensure output directory creation with parent directories if not existing
    - Verify file write error produces RuntimeException "Failed to write test file"
    - Retain existing `formatClassName` and `formatMethodName` logic
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8_

  - [ ]* 6.2 Write property test for class name derivation (Property 7)
    - **Property 7: Class name derivation produces valid Java identifiers**
    - Use jqwik to generate random endpoint path strings → assert result has no curly braces, no slashes, starts uppercase, ends with "ApiTests", only alphanumeric between start and suffix
    - **Validates: Requirements 4.3**

  - [ ]* 6.3 Write property test for method name sanitization (Property 8)
    - **Property 8: Method name sanitization produces valid Java method names**
    - Generate random test name strings → assert result is alphanumeric only and does not start with digit
    - **Validates: Requirements 4.4**

  - [ ]* 6.4 Write property test for code generation structure (Property 9)
    - **Property 9: Code generation produces structurally complete test classes**
    - Generate valid endpoint, method, and non-empty list of ApiTestCase → assert output contains RestAssured imports, @BeforeAll setup, one @Test per test case, class name ending with "ApiTests", correct return message
    - **Validates: Requirements 4.1, 4.5, 4.8**

- [x] 7. Update controller and exception handling
  - [x] 7.1 Update TestAssistantController to wire updated services
    - Ensure all four endpoints remain exposed: `/generate/api-tests`, `/generate/test-data`, `/generate/restassured-tests`, `/review/tests`
    - Update constructor injection to use `BedrockAiService`-backed services
    - Verify response wrapping with `ApiResponse.success()` / `ApiResponse.failure()`
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.6_

  - [x] 7.2 Update GlobalExceptionHandler to sanitize error messages
    - Ensure IllegalArgumentException → 400, RuntimeException → 500, Exception → 500
    - Sanitize messages: strip stack trace patterns (`at com.`), internal paths (`src/`, `C:\`), and fully-qualified class names
    - _Requirements: 6.4, 6.5, 6.7_

  - [ ]* 7.3 Write property test for error response sanitization (Property 11)
    - **Property 11: Error responses do not expose internal system details**
    - Generate RuntimeExceptions with messages containing stack traces, file paths, and class names → assert response body message does not contain those patterns
    - **Validates: Requirements 6.5**

- [x] 8. Update PromptTemplates for Bedrock/Claude compatibility
  - [x] 8.1 Update PromptTemplates class for Claude-optimized prompts
    - Make class `final` with private constructor
    - Ensure each prompt contains explicit "Return ONLY" + "JSON" + "no markdown" instruction
    - Verify sample JSON structures demonstrate exact field names, nesting, and types
    - Ensure test case prompt includes positive, negative, boundary, security categories
    - Ensure test data prompt includes 40% valid, 20% boundary, 20% invalid, 20% edge case distribution guidance
    - Verify all placeholders are parameterized (`%s`, `%d`) for dynamic content
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6_

  - [ ]* 8.2 Write property test for prompt template completeness (Property 12)
    - **Property 12: Prompt templates are complete and well-formed**
    - Assert each template contains "ONLY" and "JSON" and "no markdown" text
    - Format each template with valid placeholder values → assert no unresolved `%s` or `%d` remain
    - **Validates: Requirements 7.2, 7.6**

- [x] 9. Checkpoint - Ensure full application compiles and all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 10. Integration tests and wiring
  - [x] 10.1 Write controller integration tests with MockMvc
    - Test all four POST endpoints with valid request bodies → assert 200 + success response structure
    - Test validation failures → assert 400 + failure response
    - Test service exceptions → assert 500 + sanitized failure response
    - Mock `BedrockAiService` to avoid external API calls in tests
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_

  - [ ]* 10.2 Write unit tests for TestCaseService retry logic
    - Mock AI to return invalid JSON first 2 times, valid on 3rd → assert success
    - Mock AI to return invalid JSON all 3 times → assert RuntimeException with retry message
    - _Requirements: 2.5, 2.6_

  - [ ]* 10.3 Write unit tests for CodeGeneratorService file operations
    - Test file write to temp directory → assert file exists with correct content
    - Test I/O failure handling → assert RuntimeException "Failed to write test file"
    - _Requirements: 4.2, 4.6, 4.7_

- [x] 11. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties defined in the design document (12 total)
- Unit tests validate specific examples and edge cases
- The existing `AiService.java` will be replaced by `BedrockAiService.java`
- AWS credentials must be configured via environment variables, IAM role, or AWS profile — never in source code
- All property tests use jqwik with minimum 100 iterations per `@Property` method

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1"] },
    { "id": 1, "tasks": ["2.1"] },
    { "id": 2, "tasks": ["2.2", "2.3", "2.4"] },
    { "id": 3, "tasks": ["4.1", "4.4", "4.6"] },
    { "id": 4, "tasks": ["4.2", "4.3", "4.5", "4.7", "4.8", "6.1"] },
    { "id": 5, "tasks": ["6.2", "6.3", "6.4", "7.1", "7.2", "8.1"] },
    { "id": 6, "tasks": ["7.3", "8.2", "10.1"] },
    { "id": 7, "tasks": ["10.2", "10.3"] }
  ]
}
```
