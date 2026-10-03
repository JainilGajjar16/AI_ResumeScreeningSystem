package com.resume.screening.service;

import com.resume.screening.dto.CandidateRecommendationDto;

import java.util.List;

public interface CandidateRecommendationService {

    /**
     * Retrieves all applicants for a recruiter-owned job, merged with their existing ATS analysis,
     * ranked descending by existing ATS score (highest score first, followed by unanalyzed candidates).
     */
    List<CandidateRecommendationDto> getRankedCandidatesForJob(Long jobId, String recruiterUsername);

    /**
     * Retrieves single candidate recommendation details for a recruiter-owned job.
     */
    CandidateRecommendationDto getCandidateRecommendationForJob(Long jobId, Long studentProfileId, String recruiterUsername);
}
