package com.resume.screening.engine;

import com.resume.screening.dto.AnalysisResult;
import com.resume.screening.entity.ParsedJobRequirement;
import com.resume.screening.entity.ParsedResume;
import com.resume.screening.entity.StudentProfile;

public interface ResumeAnalysisEngine {
    AnalysisResult analyze(ParsedResume parsedResume, ParsedJobRequirement parsedJobRequirement, StudentProfile studentProfile);
}
