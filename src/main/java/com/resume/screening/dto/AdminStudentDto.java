package com.resume.screening.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminStudentDto {
    private Long id;
    private String studentName;
    private String email;
    private String phone;
    private String college;
    private String degree;
    private String branch;
    private String skills;
    private String githubUrl;
    private String linkedinUrl;
    private String portfolioUrl;
    private boolean hasResume;
    private String resumeStatus;
    private int applicationsCount;
    private int interviewsCount;
    private String finalSelectionStatus;
}
