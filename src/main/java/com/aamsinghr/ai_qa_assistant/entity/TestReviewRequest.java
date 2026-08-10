package com.aamsinghr.ai_qa_assistant.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestReviewRequest {

    private String framework;
    private String endpoint;
    private String testCode;
}
