package com.resume.screening.service;

import com.resume.screening.dto.ParsedResumeDto;

public interface ResumeParsingService {
    ParsedResumeDto parseAndSaveResume(Long resumeId);
    ParsedResumeDto getParsedResumeByResumeId(Long resumeId);
    ParsedResumeDto getParsedResumeForStudent(String usernameOrEmail);
}
