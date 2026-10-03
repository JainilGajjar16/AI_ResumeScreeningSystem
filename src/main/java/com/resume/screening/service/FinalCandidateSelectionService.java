package com.resume.screening.service;

import com.resume.screening.dto.FinalCandidateDecisionDto;

public interface FinalCandidateSelectionService {

    FinalCandidateDecisionDto selectCandidate(Long applicationId, String recruiterUsername);

    FinalCandidateDecisionDto rejectCandidate(Long applicationId, String recruiterUsername);

    FinalCandidateDecisionDto getDecision(Long applicationId, String username);
}
