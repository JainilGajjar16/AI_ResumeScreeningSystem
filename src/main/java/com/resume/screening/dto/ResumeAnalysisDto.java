package com.resume.screening.dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeAnalysisDto {

    private Long id;
    private Long resumeId;
    private Integer resumeVersionNumber;
    private String resumeFileName;
    private Long jobId;
    private String jobTitle;
    private String companyName;
    private Long studentId;
    private String studentName;
    private String studentEmail;

    private Double atsScore;
    private Double skillMatchPercentage;
    private List<String> matchedSkills;
    private List<String> missingSkills;
    private String qualificationMatch;
    private String experienceMatch;

    // Component Scores Breakdown
    private Double skillsMatchScore;        // max 50
    private Double experienceScore;          // max 20
    private Double qualificationScore;       // max 15
    private Double resumeCompletenessScore; // max 10
    private Double keywordMatchScore;       // max 5

    private List<String> strengths;
    private List<String> suggestions;

    private Boolean hasGithub;
    private String githubUrl;
    private Boolean hasLinkedin;
    private String linkedinUrl;

    private String analyzedAtFormatted;
}
