package com.resume.screening.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobApplicationDto {

    private Long id;
    
    // Job details
    private Long jobId;
    private String jobTitle;
    private String companyName;
    private String location;
    private String employmentType;
    private String jobStatus;

    // Student / Applicant details
    private Long studentProfileId;
    private String studentName;
    private String studentEmail;
    private String studentPhone;
    private String studentCollege;
    private String studentDegree;
    private String studentBranch;
    private Double studentCgpa;
    private String studentSkills;
    private String githubUrl;
    private String linkedinUrl;
    private String education;
    private String experience;
    private String summary;

    // Resume details
    private Long resumeId;
    private String resumeFileName;
    private String resumeFileType;

    // Application metadata
    private LocalDateTime appliedAt;
    private String formattedAppliedAt;
    private String status; // APPLIED, UNDER_REVIEW, SHORTLISTED, REJECTED, SELECTED
}
