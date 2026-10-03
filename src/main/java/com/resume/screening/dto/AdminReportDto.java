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
public class AdminReportDto {

    private RecruitmentSummary recruitmentSummary;
    private CandidatePipeline candidatePipeline;
    private List<JobReportSummary> jobSummaries;
    private List<RecruiterReportSummary> recruiterSummaries;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecruitmentSummary {
        private long totalJobs;
        private long totalApplications;
        private long totalShortlisted;
        private long totalInterviews;
        private long completedInterviews;
        private long selectedInterviewResults;
        private long rejectedInterviewResults;
        private long pendingFinalDecisions;
        private long finalSelected;
        private long finalRejected;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CandidatePipeline {
        private long applicationsCount;
        private long shortlistedCount;
        private long interviewedCount;
        private long interviewSelectedCount;
        private long finalSelectedCount;
        private long finalRejectedCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class JobReportSummary {
        private Long jobId;
        private String jobTitle;
        private String recruiterName;
        private String companyName;
        private long applicationsCount;
        private long shortlistedCount;
        private long interviewsCount;
        private long completedInterviewsCount;
        private long interviewSelectedCount;
        private long finalSelectedCount;
        private long finalRejectedCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecruiterReportSummary {
        private Long recruiterId;
        private String recruiterName;
        private String companyName;
        private long jobsCount;
        private long applicationsCount;
        private long interviewsCount;
        private long completedInterviewsCount;
        private long finalSelectedCount;
        private long finalRejectedCount;
    }
}
