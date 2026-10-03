package com.resume.screening.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminRecruiterDto {
    private Long id;
    private String recruiterName;
    private String email;
    private String companyName;
    private String companyWebsite;
    private String designation;
    private long jobsPosted;
    private long applicationsReceived;
    private long interviewsConducted;
    private long selectedCandidates;
}
