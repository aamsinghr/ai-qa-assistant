package com.aamsinghr.ai_qa_assistant.ai;

public final class PromptTemplates {

    private PromptTemplates() {
        // Utility class — not instantiable
    }

    public static final String API_TEST_PROMPT = """
            You are a senior QA automation engineer with expertise in REST API testing.

            Generate comprehensive API test cases for the following endpoint.

            Endpoint: %s
            Method: %s
            Request Body Schema: %s

            Generate at least 8 test cases covering ALL of the following categories:

            1. Positive scenarios — valid input, expected success response
            2. Negative scenarios — invalid input, missing fields, wrong data types
            3. Boundary scenarios — empty strings, null values, max/min lengths, special characters
            4. Security scenarios — injection attempts, authentication bypass, privilege escalation

            STRICT RULES:
            1. Return ONLY a single JSON array — no extra text, no markdown, no explanation, no code blocks
            2. Do NOT split into multiple arrays
            3. Do NOT add explanations outside the JSON
            4. Each test case must cover a UNIQUE validation condition
            5. statusCode must be a realistic HTTP code (200, 201, 400, 401, 403, 404, 409, 422, 500)
            6. responseBody must contain realistic expected fields, not just {}
            7. input must contain realistic values matching the schema types

            Return in this EXACT format:
            [
              {
                "testName": "Valid User Registration",
                "description": "Positive: register with all valid fields",
                "input": {
                  "email": "john@example.com",
                  "password": "SecurePass@123"
                },
                "expectedResult": {
                  "statusCode": 201,
                  "responseBody": {
                    "message": "User registered successfully",
                    "userId": 1
                  }
                }
              }
            ]

            Return ONLY JSON. No markdown, no explanation, no code blocks.
            """;

    public static final String TEST_DATA_PROMPT = """
            You are a QA engineer specializing in test data generation.

            Given the following schema, generate %d test data objects.

            Schema:
            %s

            Generate a MIX of the following data types:
            - Valid realistic data (at least 40%%)
            - Boundary values — empty strings, zero, max length values (at least 20%%)
            - Invalid data — wrong types, negative numbers, malformed emails (at least 20%%)
            - Edge cases — special characters, very long strings, null values (at least 20%%)

            STRICT RULES:
            1. Return ONLY a JSON array — no markdown, no explanation, no code blocks
            2. Maintain correct data types as defined in the schema
            3. Use realistic values — real-looking names, proper email formats for valid cases
            4. Each object must be meaningfully different from others
            5. Invalid data objects should have values that would realistically fail validation

            Return in this EXACT format:
            [
              {
                "fieldName": "value"
              }
            ]

            Return ONLY JSON. No markdown, no explanation, no code blocks.
            """;

    public static final String TEST_REVIEW_PROMPT = """
            You are a senior QA automation engineer performing a detailed code review.

            Review the following test file and provide a comprehensive analysis.

            Framework: %s
            Endpoint being tested: %s

            Test Code:
            %s

            Analyze the test file and identify:
            1. How many test methods exist
            2. What percentage of important scenarios are covered
            3. What critical scenarios are missing
            4. What specific improvements should be made
            5. What good practices are already present

            STRICT RULES:
            1. Return ONLY a single JSON object — no markdown, no explanation, no code blocks
            2. coverageScore must be a percentage string like "65%%"
            3. All list fields must be proper JSON arrays of strings
            4. totalTestsFound must be an integer
            5. Be specific in missingScenarios — mention field names and exact scenario
            6. Be encouraging in goodPracticesFound — always find at least one positive

            Return in this EXACT format:
            {
              "totalTestsFound": 5,
              "coverageScore": "60%%",
              "overallFeedback": "The test suite covers basic positive and negative cases but lacks security and boundary coverage.",
              "missingScenarios": [
                "No SQL injection test for email field",
                "No test for missing Authorization header",
                "No boundary test for password minimum length"
              ],
              "suggestions": [
                "Add test with email = 'admin@test.com OR 1=1' to check SQL injection",
                "Add test with no Authorization header expecting 401",
                "Add test with password of exactly 7 characters expecting 400"
              ],
              "goodPracticesFound": [
                "Good coverage of empty field scenarios",
                "Correct use of HTTP status code assertions"
              ]
            }

            Return ONLY JSON. No markdown, no explanation, no code blocks.
            """;
}
