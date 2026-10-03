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
public class ParsedJobRequirementDto {
    private Long id;
    private Long jobId;
    private String jobTitle;
    private String requiredSkillsRaw;
    
    @Builder.Default
    private List<String> requiredSkills = new ArrayList<>();
    
    private String qualification;
    private String experience;
    private String responsibilities;
    private String keywordsRaw;
    
    @Builder.Default
    private List<String> keywords = new ArrayList<>();
    
    private String rawText;
    private String parsingStatus; // PARSED, FAILED
    private String errorMessage;
    private LocalDateTime parsedAt;
    private String formattedParsedAt;
}
