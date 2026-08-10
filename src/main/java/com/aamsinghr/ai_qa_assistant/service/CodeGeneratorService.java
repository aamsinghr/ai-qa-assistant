package com.aamsinghr.ai_qa_assistant.service;

import com.aamsinghr.ai_qa_assistant.entity.ApiTestCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class CodeGeneratorService {

    @Value("${test.output.path}")
    private String testOutputPath;

    /**
     * Generates a RestAssured Java test file and writes it to disk.
     *
     * @param endpoint API endpoint path
     * @param method   HTTP method
     * @param testCases List of test cases to generate methods for
     * @return Success message with generated filename
     * @throws RuntimeException if file write fails
     */
    public String generateRestAssuredTests(String endpoint,
                                           String method,
                                           List<ApiTestCase> testCases) {

        String className = formatClassName(endpoint);

        StringBuilder code = new StringBuilder();

        code.append("package generated;\n\n");
        code.append("import static io.restassured.RestAssured.*;\n");
        code.append("import io.restassured.RestAssured;\n");
        code.append("import io.restassured.http.ContentType;\n");
        code.append("import org.junit.jupiter.api.BeforeAll;\n");
        code.append("import org.junit.jupiter.api.Test;\n");
        code.append("import java.util.Map;\n\n");

        code.append("public class ").append(className).append(" {\n\n");

        code.append("    @BeforeAll\n");
        code.append("    public static void setup() {\n");
        code.append("        RestAssured.baseURI = \"http://localhost:8080\";\n");
        code.append("    }\n\n");

        for (ApiTestCase test : testCases) {

            code.append("    @Test\n");
            code.append("    public void ")
                    .append(formatMethodName(test.getTestName()))
                    .append("() {\n\n");

            if (test.getInput() != null && !test.getInput().isEmpty()) {
                code.append(generateMapCode("requestBody", test.getInput(), 2));
            }

            int statusCode = 200;
            if (test.getExpectedResult() != null) {
                Object sc = test.getExpectedResult().get("statusCode");
                if (sc instanceof Integer) {
                    statusCode = (Integer) sc;
                } else if (sc instanceof Number) {
                    statusCode = ((Number) sc).intValue();
                }
            }

            code.append("\n");

            code.append("        given()\n")
                    .append("            .contentType(ContentType.JSON)\n");

            if (test.getInput() != null && !test.getInput().isEmpty()) {
                code.append("            .body(requestBody)\n");
            }

            code.append("        .when()\n")
                    .append("            .")
                    .append(method.toLowerCase())
                    .append("(\"")
                    .append(endpoint)
                    .append("\")\n")
                    .append("        .then()\n")
                    .append("            .statusCode(")
                    .append(statusCode)
                    .append(");\n\n");

            code.append("    }\n\n");
        }

        code.append("}");

        saveToFile(className, code.toString());

        return "RestAssured test file generated: " + className + ".java";
    }

    String formatClassName(String endpoint) {
        String cleaned = endpoint
                .replaceAll("[{}]", "")
                .replaceAll("/", " ")
                .trim();

        String[] parts = cleaned.split(" ");
        StringBuilder className = new StringBuilder();

        for (String part : parts) {
            if (!part.isEmpty()) {
                className.append(Character.toUpperCase(part.charAt(0)))
                        .append(part.substring(1));
            }
        }

        className.append("ApiTests");
        return className.toString();
    }

    String formatMethodName(String testName) {
        String sanitized = testName.replaceAll("[^a-zA-Z0-9]", "");
        if (!sanitized.isEmpty() && Character.isDigit(sanitized.charAt(0))) {
            sanitized = "test" + sanitized;
        }
        return sanitized;
    }

    private String generateMapCode(String varName,
                                   Map<String, Object> map,
                                   int indentLevel) {

        StringBuilder code = new StringBuilder();
        String indent = "    ".repeat(indentLevel);

        code.append(indent)
                .append("Map<String, Object> ")
                .append(varName)
                .append(" = Map.of(\n");

        int i = 0;
        for (Map.Entry<String, Object> entry : map.entrySet()) {

            String key = entry.getKey();
            Object value = entry.getValue();

            code.append(indent).append("    \"").append(key).append("\", ");

            if (value instanceof Map) {
                code.append(varName).append("_").append(key);
            } else if (value instanceof String) {
                code.append("\"").append(value).append("\"");
            } else {
                code.append(value);
            }

            if (i < map.size() - 1) {
                code.append(",");
            }

            code.append("\n");
            i++;
        }

        code.append(indent).append(");\n\n");

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getValue() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nested = (Map<String, Object>) entry.getValue();
                code.append(generateMapCode(
                        varName + "_" + entry.getKey(),
                        nested,
                        indentLevel
                ));
            }
        }

        return code.toString();
    }

    private void saveToFile(String className, String code) {
        try {
            File folder = new File(testOutputPath);

            if (!folder.exists()) {
                if (!folder.mkdirs()) {
                    throw new IOException("Failed to create output directory: " + testOutputPath);
                }
            }

            File file = new File(folder, className + ".java");
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(code);
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to write test file", e);
        }
    }
}
