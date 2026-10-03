package com.resume.screening.dto;

import com.resume.screening.enums.ApplicationDecision;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateRecommendationDto {

    private Long applicationId;
    private Long jobId;
    private String jobTitle;
    private String companyName;

    private Long studentProfileId;
    private String studentName;
    private String studentEmail;
    private String studentPhone;
    private String studentCollege;
    private String studentDegree;
    private String studentBranch;
    private String studentSkills;
    private String githubUrl;
    private String linkedinUrl;

    private LocalDateTime appliedAt;
    private String formattedAppliedAt;
    private String applicationStatus;
    private ApplicationDecision finalDecision;

    private Long resumeId;
    private String resumeFileName;

    @Builder.Default
    private boolean hasAtsAnalysis = false;
    private Double atsScore;
    private String formattedAtsScore;
    private Double skillMatchPercentage;
    private String formattedSkillMatch;
    private String qualificationMatch;
    private String experienceMatch;
    private List<String> matchedSkills;
    private List<String> missingSkills;

    private String recommendationLabel;
    private String recommendationBadgeClass;

    @Builder.Default
    private boolean isShortlisted = false;
    private Long shortlistedCandidateId;
}
