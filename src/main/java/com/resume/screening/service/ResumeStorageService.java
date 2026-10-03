package com.resume.screening.service;

import com.resume.screening.dto.ResumeDto;
import com.resume.screening.entity.Resume;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface ResumeStorageService {
    ResumeDto getCurrentResume(String usernameOrEmail);
    ResumeDto uploadResume(String usernameOrEmail, MultipartFile file);
    Resource loadResumeFile(String usernameOrEmail, Long resumeId);
    Resource loadResumeVersionFile(String usernameOrEmail, Long versionId);
    Resume getResumeEntityWithOwnershipCheck(String usernameOrEmail, Long resumeId);
    Resource loadResumeFileForRecruiter(String recruiterUsername, Long resumeId);
    Resume getResumeEntityForRecruiter(String recruiterUsername, Long resumeId);
    void deleteResume(String usernameOrEmail, Long resumeId);
}
