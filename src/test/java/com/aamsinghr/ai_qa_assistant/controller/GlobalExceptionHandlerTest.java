package com.aamsinghr.ai_qa_assistant.controller;

import com.aamsinghr.ai_qa_assistant.entity.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleIllegalArgument_returns400WithExceptionMessage() {
        IllegalArgumentException ex = new IllegalArgumentException("Endpoint cannot be empty");

        ResponseEntity<ApiResponse<Void>> response = handler.handleIllegalArgument(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Endpoint cannot be empty", response.getBody().getMessage());
        assertNull(response.getBody().getData());
    }

    @Test
    void handleRuntimeException_returns500WithSanitizedMessage() {
        RuntimeException ex = new RuntimeException("Failed to generate test data");

        ResponseEntity<ApiResponse<Void>> response = handler.handleRuntimeException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Failed to generate test data", response.getBody().getMessage());
        assertNull(response.getBody().getData());
    }

    @Test
    void handleRuntimeException_stripsStackTraceLines() {
        String messageWithStackTrace = "Error occurred\n  at com.aamsinghr.ai_qa_assistant.service.TestCaseService.generate(TestCaseService.java:45)\n  at com.aamsinghr.ai_qa_assistant.controller.TestAssistantController.generate(TestAssistantController.java:30)";
        RuntimeException ex = new RuntimeException(messageWithStackTrace);

        ResponseEntity<ApiResponse<Void>> response = handler.handleRuntimeException(ex);

        String resultMessage = response.getBody().getMessage();
        assertFalse(resultMessage.contains("at com."), "Should not contain stack trace lines");
        assertFalse(resultMessage.contains("TestCaseService.java"), "Should not contain file references");
    }

    @Test
    void handleRuntimeException_stripsInternalPaths() {
        RuntimeException ex = new RuntimeException("Error in src/main/java/com/aamsinghr/Service.java");

        ResponseEntity<ApiResponse<Void>> response = handler.handleRuntimeException(ex);

        String resultMessage = response.getBody().getMessage();
        assertFalse(resultMessage.contains("src/"), "Should not contain src/ paths");
    }

    @Test
    void handleRuntimeException_stripsDriveLetterPaths() {
        RuntimeException ex = new RuntimeException("Error at C:\\Users\\dev\\project\\src\\Main.java");

        ResponseEntity<ApiResponse<Void>> response = handler.handleRuntimeException(ex);

        String resultMessage = response.getBody().getMessage();
        assertFalse(resultMessage.contains("C:\\"), "Should not contain Windows drive paths");
    }

    @Test
    void handleRuntimeException_stripsFullyQualifiedClassNames() {
        RuntimeException ex = new RuntimeException("Failed in com.aamsinghr.ai_qa_assistant.service.TestCaseService");

        ResponseEntity<ApiResponse<Void>> response = handler.handleRuntimeException(ex);

        String resultMessage = response.getBody().getMessage();
        assertFalse(resultMessage.contains("com.aamsinghr"), "Should not contain fully-qualified class names");
    }

    @Test
    void handleRuntimeException_preservesUserFacingMessages() {
        RuntimeException ex = new RuntimeException("Failed to generate test data");

        ResponseEntity<ApiResponse<Void>> response = handler.handleRuntimeException(ex);

        assertEquals("Failed to generate test data", response.getBody().getMessage());
    }

    @Test
    void handleRuntimeException_returnsGenericMessageWhenAllContentSanitized() {
        RuntimeException ex = new RuntimeException("com.aamsinghr.ai_qa_assistant.SomeClass");

        ResponseEntity<ApiResponse<Void>> response = handler.handleRuntimeException(ex);

        String resultMessage = response.getBody().getMessage();
        assertFalse(resultMessage.contains("com.aamsinghr"));
        assertFalse(resultMessage.isBlank());
    }

    @Test
    void handleRuntimeException_nullMessageReturnsGenericError() {
        RuntimeException ex = new RuntimeException((String) null);

        ResponseEntity<ApiResponse<Void>> response = handler.handleRuntimeException(ex);

        assertEquals("An unexpected error occurred", response.getBody().getMessage());
    }

    @Test
    void handleGenericException_returns500WithGenericMessage() {
        Exception ex = new Exception("Some internal error details");

        ResponseEntity<ApiResponse<Void>> response = handler.handleGenericException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("An unexpected error occurred", response.getBody().getMessage());
        assertNull(response.getBody().getData());
    }
}
