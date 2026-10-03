package com.resume.screening.service;

import com.resume.screening.dto.JobApplicationDto;

import java.util.List;

public interface JobApplicationService {
    JobApplicationDto applyForJob(Long jobId, String studentUsername);
    List<JobApplicationDto> getApplicationsForStudent(String studentUsername);
    List<JobApplicationDto> getApplicantsForJob(Long jobId, String recruiterUsername);
    JobApplicationDto getApplicantProfileForRecruiter(Long studentProfileId, String recruiterUsername);
    boolean hasStudentApplied(Long jobId, String studentUsername);
}
