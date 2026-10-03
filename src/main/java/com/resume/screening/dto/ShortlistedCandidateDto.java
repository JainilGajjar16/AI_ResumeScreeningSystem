package com.resume.screening.dto;

import com.resume.screening.enums.ApplicationDecision;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShortlistedCandidateDto {

    private Long id;
    private Long jobId;
    private String jobTitle;
    private String companyName;
    private Long jobApplicationId;

    private Long studentProfileId;
    private String studentName;
    private String studentEmail;
    private String studentDegree;
    private String studentCollege;

    private Long resumeId;
    private Double atsScore;
    private String formattedAtsScore;
    private Double skillMatchPercentage;
    private String qualificationMatch;
    private String experienceMatch;

    private LocalDateTime shortlistedAt;
    private String formattedShortlistedAt;
    private String applicationStatus;
    private ApplicationDecision finalDecision;
}
