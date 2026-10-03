package com.resume.screening.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminDashboardStatsDto {
    private long totalUsers;
    private long totalStudents;
    private long totalRecruiters;
    private long totalJobs;
    private long activeJobs;
    private long totalApplications;
    private long totalInterviews;
    private long scheduledInterviews;
    private long completedInterviews;
    private long totalShortlistedCandidates;
    private long interviewSelectedCandidates;
    private long finalSelectedCandidates;
    private long finalRejectedCandidates;
    private long pendingFinalDecisions;
}
