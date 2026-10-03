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
public class AdminApplicationDto {
    private Long id;
    private String candidateName;
    private String candidateEmail;
    private String jobTitle;
    private String companyName;
    private String recruiterName;
    private Double atsScore;
    private String formattedAtsScore;
    private String recommendation;
    private String applicationStatus;
    private String interviewStatus;
    private String interviewResult;
    private String finalDecision;
    private LocalDateTime appliedAt;
}
