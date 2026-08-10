package com.aamsinghr.ai_qa_assistant.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestReviewResult {

    private int totalTestsFound;
    private String coverageScore;
    private String overallFeedback;
    private List<String> missingScenarios;
    private List<String> suggestions;
    private List<String> goodPracticesFound;
}
