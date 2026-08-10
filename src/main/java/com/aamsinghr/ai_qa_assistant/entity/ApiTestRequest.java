package com.aamsinghr.ai_qa_assistant.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiTestRequest {

    private String endpoint;
    private String method;
    private Map<String, Object> schema;
}
