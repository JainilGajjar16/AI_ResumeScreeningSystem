package com.resume.screening.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParsedResumeDto {
    private Long id;
    private Long resumeId;
    private Long studentId;
    private String rawText;
    private String summary;
    private String skillsRaw;
    
    @Builder.Default
    private List<String> skills = new ArrayList<>();
    
    private String education;
    private String experience;
    private String projects;
    private String certifications;
    
    private String parsingStatus; // NOT_PARSED, PROCESSING, PARSED, FAILED
    private String errorMessage;
    private String parserVersion;
    private LocalDateTime parsedAt;
    private String formattedParsedAt;
}
