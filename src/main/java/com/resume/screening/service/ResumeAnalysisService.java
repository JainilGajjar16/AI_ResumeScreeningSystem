package com.resume.screening.service;

import com.resume.screening.dto.ResumeAnalysisDto;

public interface ResumeAnalysisService {
    ResumeAnalysisDto analyzeResumeForJob(Long resumeId, Long jobId, String requestingUsername);
    ResumeAnalysisDto getAnalysisResult(Long resumeId, Long jobId, String requestingUsername);
    ResumeAnalysisDto getAnalysisById(Long analysisId, String requestingUsername);
    ResumeAnalysisDto getLatestAnalysisForStudent(String requestingUsername);
}
