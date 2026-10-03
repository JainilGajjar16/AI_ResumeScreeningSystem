package com.resume.screening.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecruiterDashboardDto {

    private String recruiterName;
    private String companyName;
    private String email;
    private String designation;
    
    private long totalJobs;
    private long activeJobs; // PUBLISHED
    private long draftJobs;
    private long closedJobs;
    private long totalApplications;

    private List<JobPostDto> recentJobs;
}
