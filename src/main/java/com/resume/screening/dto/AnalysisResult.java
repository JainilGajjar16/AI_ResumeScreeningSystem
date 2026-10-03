package com.resume.screening.dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalysisResult {

    private Double atsScore;                  // Total 0 - 100
    private Double skillMatchPercentage;      // 0 - 100%
    private List<String> matchedSkills;
    private List<String> missingSkills;
    private String qualificationMatch;       // MATCH, PARTIAL_MATCH, NO_MATCH, NOT_AVAILABLE
    private String experienceMatch;          // MATCH, PARTIAL_MATCH, NO_MATCH, NOT_AVAILABLE
    
    // Component weighted score contributions (for transparent breakdown)
    private Double skillsMatchScore;         // max 50
    private Double experienceScore;           // max 20
    private Double qualificationScore;        // max 15
    private Double resumeCompletenessScore;  // max 10
    private Double keywordMatchScore;        // max 5

    private List<String> strengths;
    private List<String> suggestions;
}
