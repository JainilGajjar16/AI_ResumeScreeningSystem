package com.resume.screening.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentProfileDto {

    @NotBlank(message = "First name is required")
    @Size(max = 50, message = "First name cannot exceed 50 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 50, message = "Last name cannot exceed 50 characters")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Please enter a valid email address")
    @Size(max = 100, message = "Email cannot exceed 100 characters")
    private String email;

    @Size(max = 20, message = "Mobile number cannot exceed 20 characters")
    private String phone;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate dob;

    @Size(max = 150, message = "College name cannot exceed 150 characters")
    private String college;

    @Size(max = 100, message = "Degree cannot exceed 100 characters")
    private String degree;

    @Size(max = 100, message = "Branch cannot exceed 100 characters")
    private String branch;

    @Min(value = 1, message = "Semester must be between 1 and 12")
    @Max(value = 12, message = "Semester must be between 1 and 12")
    private Integer semester;

    @DecimalMin(value = "0.00", message = "CGPA must be at least 0.00")
    @DecimalMax(value = "10.00", message = "CGPA cannot exceed 10.00")
    private Double cgpa;

    @Size(max = 2000, message = "Professional summary cannot exceed 2000 characters")
    private String summary;

    private String skills;

    private String githubUrl;

    private String linkedinUrl;

    private int completionPercentage;
}
