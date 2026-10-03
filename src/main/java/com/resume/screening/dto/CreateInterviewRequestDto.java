package com.resume.screening.dto;

import com.resume.screening.entity.InterviewStatus;
import com.resume.screening.entity.InterviewType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateInterviewRequestDto {

    @NotNull(message = "Job Application ID is required")
    private Long jobApplicationId;

    @NotBlank(message = "Round name is required")
    private String roundName;

    @NotNull(message = "Interview date is required")
    private LocalDate interviewDate;

    @NotNull(message = "Interview time is required")
    private LocalTime interviewTime;

    @NotNull(message = "Interview type is required")
    private InterviewType interviewType;

    private String meetingLink;

    private String location;

    private InterviewStatus status;

    private String notes;
}
