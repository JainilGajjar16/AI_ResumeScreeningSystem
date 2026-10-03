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
public class AdminJobDto {
    private Long id;
    private String title;
    private String companyName;
    private String recruiterName;
    private String recruiterEmail;
    private String location;
    private String jobType;
    private String status;
    private LocalDateTime createdAt;
    private LocalDate deadline;
    private long applicationsCount;
    private long shortlistedCount;
    private long interviewsCount;
    private long selectedCount;
}
