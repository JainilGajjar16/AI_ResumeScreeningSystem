package com.resume.screening.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobPostDto {

    private Long id;
    private Long recruiterId;
    private String recruiterName;
    private String companyName;
    private String title;
    private String description;
    private String requiredSkills;
    private String requirements;
    private String qualification;
    private String experienceRequired;
    private String location;
    private String jobType; // Full Time, Part Time, Internship, Contract
    private String salaryRange;
    private LocalDate deadline;
    private String formattedDeadline;
    private String status; // DRAFT, PUBLISHED, CLOSED
    private long applicationCount;
    private LocalDateTime createdAt;
    private String formattedCreatedAt;
}
