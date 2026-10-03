package com.resume.screening.service;

import com.resume.screening.dto.ParsedJobRequirementDto;

public interface JobDescriptionParsingService {
    ParsedJobRequirementDto parseAndSaveJobRequirement(Long jobId);
    ParsedJobRequirementDto getParsedJobRequirementByJobId(Long jobId);
}
