# Requirements Document

## Introduction

AI QA Assistant is a Spring Boot REST API that leverages Amazon Bedrock (Claude model) to automate QA engineering tasks. The system generates API test cases, test data, RestAssured code files, and reviews existing test coverage using AWS-native AI services.

## Glossary

- **System**: The AI QA Assistant Spring Boot application
- **Bedrock_Client**: The service component responsible for invoking Amazon Bedrock foundation models via the AWS SDK
- **Test_Case_Generator**: The service component that produces structured API test cases from endpoint metadata
- **Test_Data_Generator**: The service component that produces realistic test data objects from a schema definition
- **Code_Generator**: The service component that produces runnable RestAssured Java test files and writes them to disk
- **Test_Reviewer**: The service component that analyzes existing test code and returns coverage assessments
- **Controller**: The REST controller that exposes HTTP endpoints and delegates to service components
- **Prompt_Engine**: The component that constructs structured prompts for the AI model to enforce JSON-only responses
- **API_Response_Wrapper**: The generic response envelope containing success status, message, and data fields
- **Schema**: A JSON object describing the structure of an API request body with field names and types
- **Test_Case**: A structured object containing test name, description, input payload, and expected result
- **Review_Result**: A structured object containing coverage score, missing scenarios, suggestions, and good practices

## Requirements

### Requirement 1: Amazon Bedrock AI Integration

**User Story:** As a QA engineer, I want the system to use Amazon Bedrock (Claude model) for AI capabilities, so that I can leverage AWS-native AI services without depending on third-party API providers.

#### Acceptance Criteria

1. THE Bedrock_Client SHALL invoke the Amazon Bedrock InvokeModel API using the AWS SDK for Java v2
2. THE Bedrock_Client SHALL authenticate using AWS credentials resolved via the default credential provider chain (environment variables, IAM role, or AWS profile)
3. WHEN a prompt is submitted, THE Bedrock_Client SHALL send the prompt to the configured Claude model on Amazon Bedrock and return the text value from the first content block of the model response
4. WHEN a prompt is submitted, THE Bedrock_Client SHALL construct the request payload using the Anthropic Messages API format with the prompt as a user message
5. IF the Bedrock API returns an error or the request exceeds the configured timeout (default 30 seconds), THEN THE Bedrock_Client SHALL throw a runtime exception that includes the error type, the model identifier used, and a message indicating the failure reason
6. THE Bedrock_Client SHALL set a configurable maximum token limit for AI responses (default 4096 tokens)
7. THE System SHALL externalize the Bedrock model identifier, AWS region, and request timeout duration as configurable application properties
8. IF the submitted prompt is empty or blank, THEN THE Bedrock_Client SHALL throw a runtime exception indicating that the prompt must not be empty, without invoking the Bedrock API
9. THE Bedrock_Client SHALL enforce a maximum prompt length of 50,000 characters and throw a runtime exception indicating the limit exceeded if the submitted prompt exceeds this length

### Requirement 2: API Test Case Generation

**User Story:** As a QA engineer, I want to generate comprehensive API test cases for any endpoint, so that I can quickly identify positive, negative, boundary, and security scenarios without manual effort.

#### Acceptance Criteria

1. WHEN a valid ApiTestRequest is submitted to POST /generate/api-tests, THE Test_Case_Generator SHALL return a list of at least 8 test cases with a minimum of 1 test case in each of the following categories: positive, negative, boundary, and security
2. WHEN the endpoint field is null or blank, THE Test_Case_Generator SHALL reject the request with an IllegalArgumentException containing the message "Endpoint cannot be empty" and respond with HTTP 400 and a JSON body where "success" is false
3. WHEN the method field is null or blank, THE Test_Case_Generator SHALL reject the request with an IllegalArgumentException containing the message "Method cannot be empty" and respond with HTTP 400 and a JSON body where "success" is false
4. WHEN a valid ApiTestRequest is submitted, THE Test_Case_Generator SHALL serialize the request schema field to JSON and include it in the AI prompt; IF the schema field is null, THEN THE Test_Case_Generator SHALL serialize it as a JSON null value
5. IF the AI response cannot be parsed as a valid JSON array of test cases, THEN THE Test_Case_Generator SHALL retry the AI call up to 2 additional times (3 total attempts) before failing
6. IF all retry attempts fail, THEN THE Test_Case_Generator SHALL throw a RuntimeException with the message "AI failed to generate valid test cases after retries" and respond with HTTP 500 and a JSON body where "success" is false
7. WHEN the AI response is received, THE Test_Case_Generator SHALL extract the JSON array by locating the first '[' and last ']' characters; IF neither '[' nor ']' is found in the AI response, THEN THE Test_Case_Generator SHALL treat this as a parse failure and proceed with the retry logic defined in criterion 5
8. WHEN test cases are successfully generated, THE Test_Case_Generator SHALL return each test case containing the fields: testName, description, input, and expectedResult, wrapped in a JSON response envelope with "success" set to true, a "message" field, and a "data" field containing the test case list

### Requirement 3: Test Data Generation

**User Story:** As a QA engineer, I want to generate realistic test data from a schema definition, so that I can quickly produce valid, invalid, boundary, and edge case data objects for testing.

#### Acceptance Criteria

1. WHEN a TestDataRequest containing a non-null, non-empty schema map is submitted to POST /generate/test-data, THE Test_Data_Generator SHALL return exactly 5 test data objects matching the provided schema structure, wrapped in a success ApiResponse
2. THE Test_Data_Generator SHALL request a mix of valid (40%), boundary (20%), invalid (20%), and edge case (20%) data objects from the AI model via the prompt sent to the AI service
3. THE Test_Data_Generator SHALL extract the JSON array from the AI response by locating the first '[' and last ']' characters and parsing the substring between them
4. IF the AI response cannot be parsed as a valid JSON array, THEN THE Test_Data_Generator SHALL throw a RuntimeException with the message "Failed to generate test data", resulting in an HTTP 500 response with success set to false
5. IF the TestDataRequest schema field is null or an empty map, THEN THE Test_Data_Generator SHALL return an error response indicating that a non-empty schema is required

### Requirement 4: RestAssured Code Generation

**User Story:** As a QA engineer, I want to generate runnable RestAssured test files from endpoint metadata, so that I can immediately execute generated tests against the target API.

#### Acceptance Criteria

1. WHEN a valid ApiTestRequest containing a non-empty endpoint, a non-empty method, and at least one generated test case is submitted to POST /generate/restassured-tests, THE Code_Generator SHALL produce a complete Java test class using RestAssured and JUnit 5
2. THE Code_Generator SHALL write the generated Java file to the directory specified by the test.output.path application property
3. THE Code_Generator SHALL derive the class name from the endpoint path by removing curly braces and slashes, capitalizing the first character of each path segment, concatenating the segments, and appending "ApiTests" (e.g., "/users/{id}/orders" becomes "UsersIdOrdersApiTests")
4. THE Code_Generator SHALL generate one @Test method per test case with a method name derived from the test name by removing all non-alphanumeric characters, and if the resulting name begins with a digit, prefixing it with "test"
5. THE Code_Generator SHALL include RestAssured static imports, ContentType setup, and a @BeforeAll method configuring the base URI to "http://localhost:8080"
6. THE Code_Generator SHALL create the output directory including any necessary parent directories if it does not already exist
7. IF the file cannot be written due to I/O failure, THEN THE Code_Generator SHALL throw a RuntimeException with the message "Failed to write test file"
8. WHEN the generated file is saved successfully, THE Code_Generator SHALL return the string "RestAssured test file generated: " followed by the class name and ".java" extension

### Requirement 5: AI Test Review

**User Story:** As a QA engineer, I want to submit existing test code for AI-powered review, so that I can identify coverage gaps, missing scenarios, and receive improvement suggestions.

#### Acceptance Criteria

1. WHEN a TestReviewRequest is submitted to POST /review/tests with a non-null and non-blank testCode field, THE Test_Reviewer SHALL return a TestReviewResult containing totalTestsFound (integer), coverageScore (percentage string), overallFeedback (string), missingScenarios (list of strings), suggestions (list of strings), and goodPracticesFound (list of strings)
2. WHEN the testCode field is null or blank, THE Test_Reviewer SHALL reject the request with an IllegalArgumentException containing the message "Test code cannot be empty"
3. IF the framework or endpoint field in the TestReviewRequest is null, THEN THE Test_Reviewer SHALL substitute the value "unknown" for that field when including it in the AI prompt for context-aware review
4. THE Test_Reviewer SHALL extract the JSON object from the AI response by locating the first '{' and last '}' characters
5. IF the AI response does not contain valid JSON delimiters (no '{' or '}' found, or the last '}' position is not after the first '{' position), THEN THE Test_Reviewer SHALL throw a RuntimeException indicating the AI returned an invalid JSON structure
6. IF the extracted JSON cannot be parsed as a valid TestReviewResult, THEN THE Test_Reviewer SHALL throw a RuntimeException with the message "Failed to review test file" followed by the error detail

### Requirement 6: REST API and Response Contract

**User Story:** As an API consumer, I want all endpoints to return a consistent response format, so that I can reliably parse success and error responses with a single client-side model.

#### Acceptance Criteria

1. THE Controller SHALL expose four POST endpoints: /generate/api-tests, /generate/test-data, /generate/restassured-tests, and /review/tests
2. THE API_Response_Wrapper SHALL contain three fields: success (boolean), message (String with a maximum length of 500 characters), and data (generic typed payload that is null when success is false)
3. WHEN a request is processed successfully, THE Controller SHALL return HTTP status 200 with an ApiResponse where success is set to true, message is a non-empty string describing the completed operation, and the data field contains the result payload
4. WHEN an IllegalArgumentException occurs, THE System SHALL return HTTP status 400 with an ApiResponse where success is set to false, the message field contains the exception message, and the data field is null
5. WHEN an unexpected RuntimeException occurs, THE System SHALL return HTTP status 500 with an ApiResponse where success is set to false, the message field contains a generic error description that does not include stack traces, internal class names, or server file paths, and the data field is null
6. THE System SHALL handle all exceptions through a centralized GlobalExceptionHandler annotated with @RestControllerAdvice
7. WHEN a checked Exception occurs that is not an IllegalArgumentException or RuntimeException, THE System SHALL return HTTP status 500 with an ApiResponse where success is set to false, a generic error message that does not expose internal system details, and the data field is null

### Requirement 7: Prompt Engineering

**User Story:** As a system maintainer, I want AI prompts to be centralized and structured to enforce JSON-only responses, so that prompt modifications do not require changes across multiple service classes.

#### Acceptance Criteria

1. THE Prompt_Engine SHALL store all prompt templates as public static final String constants in a single dedicated class, with no prompt text defined in any service class
2. THE Prompt_Engine SHALL instruct the AI model in each prompt template to return only JSON with no markdown, no explanations, and no code blocks, by including an explicit instruction stating "Return ONLY JSON" and "no markdown, no explanation, no code blocks"
3. THE Prompt_Engine SHALL include a sample JSON object in each prompt template that demonstrates the exact field names, nesting structure, and data types expected in the AI response
4. THE Prompt_Engine SHALL include category distribution guidance listing at minimum the categories positive, negative, boundary, and security in the test case generation prompt
5. THE Prompt_Engine SHALL include data distribution guidance specifying valid data at 40%, boundary values at 20%, invalid data at 20%, and edge cases at 20% in the test data generation prompt
6. THE Prompt_Engine SHALL define each prompt template with parameterized placeholders for dynamic content (endpoint, method, schema, count) so that service classes supply only variable values without modifying prompt text

### Requirement 8: Application Configuration

**User Story:** As a developer, I want all environment-specific settings externalized to application properties, so that I can deploy the application across environments without code changes.

#### Acceptance Criteria

1. THE System SHALL externalize the AWS region as a configurable property (aws.bedrock.region) that accepts a valid AWS region identifier
2. THE System SHALL externalize the Bedrock model identifier as a configurable property (aws.bedrock.model-id) that accepts a non-empty string value
3. THE System SHALL externalize the generated test output path as a configurable property (test.output.path) with a default value of "src/test/java/generated"
4. THE System SHALL resolve AWS credentials through the AWS SDK default credential provider chain without storing credentials in application properties or source-controlled configuration files
5. IF the server.port property is not explicitly set, THEN THE System SHALL listen on port 8080
6. IF a required configurable property (aws.bedrock.region or aws.bedrock.model-id) is not provided at startup, THEN THE System SHALL fail to start and log an error message indicating which property is missing

### Requirement 9: Frontend User Interface

**User Story:** As a QA engineer, I want a web-based interface for the assistant, so that I can interact with all features through a browser without using curl or Postman.

#### Acceptance Criteria

1. THE System SHALL serve a single-page HTML interface at the root path (/) that returns an HTTP 200 response with content-type text/html
2. THE frontend SHALL provide form inputs for each of the four API capabilities (test case generation, test data generation, RestAssured code generation, test review), with each form containing input fields that correspond to the required request parameters for that capability's API endpoint
3. WHEN the system receives AI-generated results, THE frontend SHALL display the response data in a structured layout using preformatted text or labeled sections so that output is distinguishable from the input form
4. WHILE the frontend is waiting for an API response, THE frontend SHALL disable the submit button and change the button label to indicate that processing is in progress
5. IF an API call returns a response where the success field is false, THEN THE frontend SHALL display the message field from the ApiResponse to the user in the results area
6. IF a network error occurs and no API response is received, THEN THE frontend SHALL display the error description in the results area so the user can distinguish network failures from API-level errors
7. WHEN the API response is received or an error occurs, THE frontend SHALL re-enable the submit button and restore its original label

### Requirement 10: Project Structure and Build

**User Story:** As a developer, I want the project to follow standard Spring Boot Maven conventions, so that it integrates with existing build pipelines and IDE tooling without additional configuration.

#### Acceptance Criteria

1. THE System SHALL use Java 17 as the minimum language version specified via the `java.version` property in pom.xml
2. THE System SHALL use Spring Boot 3.3.x as the parent POM framework version
3. THE System SHALL use Maven as the build tool with source code located under src/main/java and test code located under src/test/java
4. THE System SHALL include the AWS SDK for Java v2 Bedrock Runtime dependency as a compile-scope dependency for AI model invocation
5. THE System SHALL include Lombok as an optional compile dependency, Jackson as a JSON processing dependency (either explicit or transitively via Spring Web), and spring-boot-starter-web as a compile-scope dependency
6. THE System SHALL include JUnit 5 and RestAssured as test-scoped dependencies that are not packaged in the production artifact
7. THE System SHALL organize source code under a base package into at minimum the following sub-packages: ai, controller, entity, and service, where additional sub-packages are permitted
8. WHEN a developer runs `mvn compile` on a clean checkout, THE System SHALL complete compilation without errors, confirming all declared dependencies resolve and annotation processors execute successfully
