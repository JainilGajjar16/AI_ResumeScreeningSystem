package com.resume.screening.dto;

import com.resume.screening.enums.ApplicationDecision;
import com.resume.screening.entity.InterviewResult;
import com.resume.screening.entity.InterviewStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinalCandidateDecisionDto {

    private Long applicationId;
    private Long studentProfileId;
    private String candidateName;
    private String candidateEmail;
    private Long jobPostId;
    private String jobTitle;
    private String companyName;
    private Double atsScore;
    private String formattedAtsScore;

    private InterviewStatus interviewStatus;
    private InterviewResult interviewResult;
    private ApplicationDecision finalDecision;
    private LocalDateTime decidedAt;
}
