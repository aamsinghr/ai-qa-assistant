package com.aamsinghr.ai_qa_assistant.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiTestCase {

    private String testName;
    private String description;
    private Map<String, Object> input;
    private Map<String, Object> expectedResult;
}
