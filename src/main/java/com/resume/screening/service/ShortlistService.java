package com.resume.screening.service;

import com.resume.screening.dto.ShortlistedCandidateDto;

import java.util.List;

public interface ShortlistService {

    /**
     * Shortlist candidate for a recruiter-owned job.
     */
    void shortlistCandidate(Long jobId, Long studentProfileId, String recruiterUsername);

    /**
     * Remove candidate from shortlist for a recruiter-owned job.
     * Note: Original JobApplication must NOT be deleted.
     */
    void removeCandidateFromShortlist(Long jobId, Long studentProfileId, String recruiterUsername);

    /**
     * Remove shortlist by shortlist record ID directly.
     */
    void removeShortlistById(Long shortlistId, String recruiterUsername);

    /**
     * Retrieves all shortlisted candidates for recruiter-owned jobs.
     */
    List<ShortlistedCandidateDto> getShortlistedCandidatesForRecruiter(String recruiterUsername);

    /**
     * Check if a candidate is shortlisted for a job.
     */
    boolean isCandidateShortlisted(Long jobId, Long studentProfileId);
}
