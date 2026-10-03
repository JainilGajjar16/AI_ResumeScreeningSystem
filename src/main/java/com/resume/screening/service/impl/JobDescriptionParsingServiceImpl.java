package com.resume.screening.service.impl;

import com.resume.screening.dto.ParsedJobRequirementDto;
import com.resume.screening.entity.JobPost;
import com.resume.screening.entity.ParsedJobRequirement;
import com.resume.screening.parser.SkillDictionaryService;
import com.resume.screening.repository.JobPostRepository;
import com.resume.screening.repository.ParsedJobRequirementRepository;
import com.resume.screening.service.JobDescriptionParsingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class JobDescriptionParsingServiceImpl implements JobDescriptionParsingService {

    private static final Logger logger = LoggerFactory.getLogger(JobDescriptionParsingServiceImpl.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final JobPostRepository jobPostRepository;
    private final ParsedJobRequirementRepository parsedJobRequirementRepository;
    private final SkillDictionaryService skillDictionaryService;

    public JobDescriptionParsingServiceImpl(JobPostRepository jobPostRepository,
                                            ParsedJobRequirementRepository parsedJobRequirementRepository,
                                            SkillDictionaryService skillDictionaryService) {
        this.jobPostRepository = jobPostRepository;
        this.parsedJobRequirementRepository = parsedJobRequirementRepository;
        this.skillDictionaryService = skillDictionaryService;
    }

    @Override
    @Transactional
    public ParsedJobRequirementDto parseAndSaveJobRequirement(Long jobId) {
        JobPost job = jobPostRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job post not found with ID: " + jobId));

        ParsedJobRequirement parsedEntity = parsedJobRequirementRepository.findByJobPostId(jobId)
                .orElseGet(() -> ParsedJobRequirement.builder()
                        .jobPost(job)
                        .parsingStatus("PARSED")
                        .build());

        try {
            // Build full textual representation
            StringBuilder fullTextBuilder = new StringBuilder();
            if (job.getTitle() != null) fullTextBuilder.append("Title: ").append(job.getTitle()).append("\n");
            if (job.getCompanyName() != null) fullTextBuilder.append("Company: ").append(job.getCompanyName()).append("\n");
            if (job.getRequiredSkills() != null) fullTextBuilder.append("Skills: ").append(job.getRequiredSkills()).append("\n");
            if (job.getQualification() != null) fullTextBuilder.append("Qualification: ").append(job.getQualification()).append("\n");
            if (job.getExperienceRequired() != null) fullTextBuilder.append("Experience: ").append(job.getExperienceRequired()).append("\n");
            if (job.getRequirements() != null) fullTextBuilder.append("Requirements:\n").append(job.getRequirements()).append("\n");
            if (job.getDescription() != null) fullTextBuilder.append("Description:\n").append(job.getDescription()).append("\n");

            String fullText = fullTextBuilder.toString();

            // Extract Skills
            Set<String> allExtractedSkills = new LinkedHashSet<>();

            // 1. Include explicit skills specified in job.requiredSkills
            if (job.getRequiredSkills() != null && !job.getRequiredSkills().trim().isEmpty()) {
                String[] explicitSkills = job.getRequiredSkills().split("[,;]");
                for (String sk : explicitSkills) {
                    if (!sk.trim().isEmpty()) {
                        allExtractedSkills.add(sk.trim());
                    }
                }
            }

            // 2. Extract from full description using skill dictionary
            List<String> dictionarySkills = skillDictionaryService.extractSkills(fullText);
            allExtractedSkills.addAll(dictionarySkills);

            // Extract Keywords
            List<String> keywordsList = extractKeywords(job);

            parsedEntity.setRawText(fullText);
            parsedEntity.setRequiredSkills(String.join(", ", allExtractedSkills));
            parsedEntity.setQualification(job.getQualification() != null ? job.getQualification() : "Not specified");
            parsedEntity.setExperience(job.getExperienceRequired() != null ? job.getExperienceRequired() : "Not specified");
            parsedEntity.setResponsibilities(extractResponsibilities(job.getDescription()));
            parsedEntity.setKeywords(String.join(", ", keywordsList));
            parsedEntity.setParsingStatus("PARSED");
            parsedEntity.setErrorMessage(null);
            parsedEntity.setParsedAt(LocalDateTime.now());

        } catch (Exception ex) {
            logger.error("Failed to parse job description ID {}: {}", jobId, ex.getMessage(), ex);
            parsedEntity.setParsingStatus("FAILED");
            parsedEntity.setErrorMessage(ex.getMessage());
            parsedEntity.setParsedAt(LocalDateTime.now());
        }

        ParsedJobRequirement saved = parsedJobRequirementRepository.save(parsedEntity);
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ParsedJobRequirementDto getParsedJobRequirementByJobId(Long jobId) {
        Optional<ParsedJobRequirement> opt = parsedJobRequirementRepository.findByJobPostId(jobId);
        return opt.map(this::mapToDto).orElse(null);
    }

    private List<String> extractKeywords(JobPost job) {
        Set<String> keywords = new LinkedHashSet<>();
        if (job.getTitle() != null) {
            keywords.addAll(Arrays.asList(job.getTitle().split("\\s+")));
        }
        if (job.getJobType() != null) {
            keywords.add(job.getJobType());
        }
        if (job.getLocation() != null) {
            keywords.add(job.getLocation());
        }

        // Clean keywords
        return keywords.stream()
                .map(k -> k.replaceAll("[^a-zA-Z0-9#+]", "").trim())
                .filter(k -> k.length() > 2)
                .distinct()
                .sorted()
                .toList();
    }

    private String extractResponsibilities(String description) {
        if (description == null || description.trim().isEmpty()) {
            return "Refer to full job description.";
        }
        // Return clean truncated snippet or full description
        if (description.length() <= 500) {
            return description.trim();
        }
        return description.substring(0, 500).trim() + "...";
    }

    private ParsedJobRequirementDto mapToDto(ParsedJobRequirement entity) {
        if (entity == null) return null;

        List<String> skillList = new ArrayList<>();
        if (entity.getRequiredSkills() != null && !entity.getRequiredSkills().trim().isEmpty()) {
            String[] split = entity.getRequiredSkills().split(",");
            for (String s : split) {
                if (!s.trim().isEmpty()) {
                    skillList.add(s.trim());
                }
            }
        }

        List<String> keywordList = new ArrayList<>();
        if (entity.getKeywords() != null && !entity.getKeywords().trim().isEmpty()) {
            String[] split = entity.getKeywords().split(",");
            for (String k : split) {
                if (!k.trim().isEmpty()) {
                    keywordList.add(k.trim());
                }
            }
        }

        return ParsedJobRequirementDto.builder()
                .id(entity.getId())
                .jobId(entity.getJobPost() != null ? entity.getJobPost().getId() : null)
                .jobTitle(entity.getJobPost() != null ? entity.getJobPost().getTitle() : "")
                .requiredSkillsRaw(entity.getRequiredSkills())
                .requiredSkills(skillList)
                .qualification(entity.getQualification())
                .experience(entity.getExperience())
                .responsibilities(entity.getResponsibilities())
                .keywordsRaw(entity.getKeywords())
                .keywords(keywordList)
                .rawText(entity.getRawText())
                .parsingStatus(entity.getParsingStatus() != null ? entity.getParsingStatus() : "PARSED")
                .errorMessage(entity.getErrorMessage())
                .parsedAt(entity.getParsedAt())
                .formattedParsedAt(entity.getParsedAt() != null ? entity.getParsedAt().format(DATE_FORMATTER) : "")
                .build();
    }
}
