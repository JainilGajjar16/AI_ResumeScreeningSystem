package com.resume.screening.service;

import com.resume.screening.dto.JobPostDto;
import com.resume.screening.dto.RecruiterDashboardDto;

import java.util.List;

public interface JobService {
    JobPostDto createJob(JobPostDto dto, String recruiterUsername);
    JobPostDto updateJob(Long jobId, JobPostDto dto, String recruiterUsername);
    void publishJob(Long jobId, String recruiterUsername);
    void closeJob(Long jobId, String recruiterUsername);
    JobPostDto getJobById(Long jobId);
    JobPostDto getJobForRecruiter(Long jobId, String recruiterUsername);
    List<JobPostDto> getJobsByRecruiter(String recruiterUsername);
    List<JobPostDto> getPublishedJobsForStudents();
    RecruiterDashboardDto getRecruiterDashboardStats(String recruiterUsername);
}
