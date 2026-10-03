package com.resume.screening.service;

import com.resume.screening.dto.CreateInterviewRequestDto;
import com.resume.screening.dto.InterviewDto;
import com.resume.screening.dto.InterviewFeedbackRequestDto;

import java.util.List;

public interface InterviewService {

    /**
     * Create/schedule a new interview for a candidate application.
     */
    InterviewDto createInterview(CreateInterviewRequestDto requestDto, String recruiterUsername);

    /**
     * Retrieve interview by ID with ownership security checks.
     */
    InterviewDto getInterviewById(Long id, String username);

    /**
     * Retrieve all interviews for a specific recruiter.
     */
    List<InterviewDto> getRecruiterInterviews(String recruiterUsername);

    /**
     * Retrieve all interviews for a specific student.
     */
    List<InterviewDto> getStudentInterviews(String studentUsername);

    /**
     * Retrieve all interviews associated with a job application.
     */
    List<InterviewDto> getInterviewsForApplication(Long applicationId, String recruiterUsername);

    /**
     * Update an existing interview.
     */
    InterviewDto updateInterview(Long id, CreateInterviewRequestDto requestDto, String recruiterUsername);

    /**
     * Cancel an interview.
     */
    InterviewDto cancelInterview(Long id, String recruiterUsername);

    /**
     * Mark an interview as COMPLETED.
     */
    InterviewDto completeInterview(Long id, String recruiterUsername);

    /**
     * Submit or update interview feedback, rating, and result.
     */
    InterviewDto submitFeedback(Long id, InterviewFeedbackRequestDto feedbackDto, String recruiterUsername);
}
