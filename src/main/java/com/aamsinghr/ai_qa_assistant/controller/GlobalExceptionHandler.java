package com.aamsinghr.ai_qa_assistant.controller;

import com.aamsinghr.ai_qa_assistant.entity.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.regex.Pattern;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Pattern STACK_TRACE_PATTERN = Pattern.compile("(?m)^\\s*at\\s+.*");
    private static final Pattern INTERNAL_PATH_PATTERN = Pattern.compile("(?i)(src/|[A-Za-z]:\\\\)\\S*");
    private static final Pattern QUALIFIED_CLASS_PATTERN = Pattern.compile(
            "\\b[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+\\.[A-Z][A-Za-z0-9_]*\\b");

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(ex.getMessage()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException ex) {
        String sanitized = sanitizeMessage(ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure(sanitized));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure("An unexpected error occurred"));
    }

    /**
     * Sanitizes a RuntimeException message by removing internal system details:
     * - Stack trace lines (lines starting with "at ")
     * - Internal file paths (containing "src/" or drive letters like "C:\")
     * - Fully-qualified Java class names (e.g., "com.aamsinghr.ai_qa_assistant.SomeClass")
     *
     * Returns the original message if none of these patterns are found.
     */
    String sanitizeMessage(String message) {
        if (message == null || message.isBlank()) {
            return "An unexpected error occurred";
        }

        String sanitized = message;

        // Strip stack trace lines (lines starting with "at ")
        sanitized = STACK_TRACE_PATTERN.matcher(sanitized).replaceAll("");

        // Remove internal file paths containing "src/" or drive letters
        sanitized = INTERNAL_PATH_PATTERN.matcher(sanitized).replaceAll("");

        // Remove fully-qualified Java class names (e.g., com.xxx.yyy.ClassName)
        sanitized = QUALIFIED_CLASS_PATTERN.matcher(sanitized).replaceAll("");

        // Clean up multiple whitespace/newlines left after removals
        sanitized = sanitized.replaceAll("\\s*\\n\\s*\\n\\s*", "\n").trim();
        sanitized = sanitized.replaceAll("\\s{2,}", " ").trim();

        // If sanitization removed everything meaningful, return generic message
        if (sanitized.isBlank()) {
            return "An unexpected error occurred";
        }

        return sanitized;
    }
}
