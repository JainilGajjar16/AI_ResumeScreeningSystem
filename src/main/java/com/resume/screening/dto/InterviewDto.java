package com.resume.screening.dto;

import com.resume.screening.enums.ApplicationDecision;
import com.resume.screening.entity.InterviewResult;
import com.resume.screening.entity.InterviewStatus;
import com.resume.screening.entity.InterviewType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewDto {

    private Long id;

    // Application & Job Details
    private Long jobApplicationId;
    private Long jobPostId;
    private String jobTitle;
    private String companyName;

    // Recruiter Details
    private Long recruiterId;
    private String recruiterName;

    // Student Details
    private Long studentProfileId;
    private String studentName;
    private String studentEmail;

    // Interview Details
    private String roundName;
    private LocalDate interviewDate;
    private LocalTime interviewTime;
    private InterviewType interviewType;
    private String meetingLink;
    private String location;
    private InterviewStatus status;
    private String notes;

    // Feedback & Result Details
    private String feedback;
    private Integer rating;
    private InterviewResult result;

    // Final Decision Details
    private ApplicationDecision finalDecision;
    private LocalDateTime decidedAt;

    // Timestamps & Formatting
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String formattedInterviewDate;
    private String formattedInterviewTime;
}
