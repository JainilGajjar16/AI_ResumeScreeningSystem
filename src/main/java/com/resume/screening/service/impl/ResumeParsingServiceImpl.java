package com.resume.screening.service.impl;

import com.resume.screening.dto.ParsedResumeDto;
import com.resume.screening.entity.ParsedResume;
import com.resume.screening.entity.Resume;
import com.resume.screening.parser.*;
import com.resume.screening.repository.ParsedResumeRepository;
import com.resume.screening.repository.ResumeRepository;
import com.resume.screening.service.ResumeParsingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ResumeParsingServiceImpl implements ResumeParsingService {

    private static final Logger logger = LoggerFactory.getLogger(ResumeParsingServiceImpl.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ResumeRepository resumeRepository;
    private final ParsedResumeRepository parsedResumeRepository;
    private final PdfResumeParser pdfResumeParser;
    private final DocxResumeParser docxResumeParser;
    private final ResumeTextCleaner textCleaner;
    private final SkillDictionaryService skillDictionaryService;
    private final ResumeSectionDetector sectionDetector;

    public ResumeParsingServiceImpl(ResumeRepository resumeRepository,
                                    ParsedResumeRepository parsedResumeRepository,
                                    PdfResumeParser pdfResumeParser,
                                    DocxResumeParser docxResumeParser,
                                    ResumeTextCleaner textCleaner,
                                    SkillDictionaryService skillDictionaryService,
                                    ResumeSectionDetector sectionDetector) {
        this.resumeRepository = resumeRepository;
        this.parsedResumeRepository = parsedResumeRepository;
        this.pdfResumeParser = pdfResumeParser;
        this.docxResumeParser = docxResumeParser;
        this.textCleaner = textCleaner;
        this.skillDictionaryService = skillDictionaryService;
        this.sectionDetector = sectionDetector;
    }

    @Override
    @Transactional
    public ParsedResumeDto parseAndSaveResume(Long resumeId) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new IllegalArgumentException("Resume not found with ID: " + resumeId));

        ParsedResume parsedEntity = parsedResumeRepository.findByResumeId(resumeId)
                .orElseGet(() -> ParsedResume.builder()
                        .resume(resume)
                        .studentProfile(resume.getStudentProfile())
                        .parsingStatus("PROCESSING")
                        .build());

        parsedEntity.setParsingStatus("PROCESSING");
        parsedEntity.setErrorMessage(null);

        File file = new File(resume.getFilePath());
        if (!file.exists()) {
            parsedEntity.setParsingStatus("FAILED");
            parsedEntity.setErrorMessage("Resume file not found on disk: " + resume.getFileName());
            parsedEntity.setParsedAt(LocalDateTime.now());
            ParsedResume savedFailed = parsedResumeRepository.save(parsedEntity);
            return mapToDto(savedFailed);
        }

        String fileType = resume.getFileType() != null ? resume.getFileType().toUpperCase() : "";
        String fileName = resume.getOriginalFileName() != null ? resume.getOriginalFileName().toLowerCase() : "";

        String rawText = "";
        boolean ocrUsed = false;
        try {
            if ("PDF".equals(fileType) || fileName.endsWith(".pdf")) {
                PdfResumeParser.PdfParseResult parseResult = pdfResumeParser.extractTextWithResult(file);
                rawText = parseResult.getText();
                ocrUsed = parseResult.isOcrUsed();
            } else if ("DOCX".equals(fileType) || fileName.endsWith(".docx")) {
                rawText = docxResumeParser.extractText(file);
            } else {
                throw new IllegalArgumentException("Unsupported file type for parsing: " + fileType);
            }

            String cleanText = textCleaner.cleanText(rawText);
            Map<ResumeSectionDetector.SectionType, String> sections = sectionDetector.parseSections(cleanText);
            List<String> extractedSkillsList = skillDictionaryService.extractSkills(cleanText);

            parsedEntity.setRawText(cleanText);
            parsedEntity.setSummary(sections.getOrDefault(ResumeSectionDetector.SectionType.SUMMARY, ""));
            parsedEntity.setSkills(String.join(", ", extractedSkillsList));
            parsedEntity.setEducation(sections.getOrDefault(ResumeSectionDetector.SectionType.EDUCATION, ""));
            parsedEntity.setExperience(sections.getOrDefault(ResumeSectionDetector.SectionType.EXPERIENCE, ""));
            parsedEntity.setProjects(sections.getOrDefault(ResumeSectionDetector.SectionType.PROJECTS, ""));
            parsedEntity.setCertifications(sections.getOrDefault(ResumeSectionDetector.SectionType.CERTIFICATIONS, ""));

            parsedEntity.setParsingStatus("PARSED");
            if (ocrUsed) {
                parsedEntity.setErrorMessage("Text extracted using OCR.");
                parsedEntity.setParserVersion("v1.0-OCR");
            } else {
                parsedEntity.setErrorMessage("Resume text extracted successfully.");
                parsedEntity.setParserVersion("v1.0");
            }
            parsedEntity.setParsedAt(LocalDateTime.now());

            // Update parent resume status to reflect successful parse
            resume.setStatus("PARSED");
            resumeRepository.save(resume);

        } catch (Exception ex) {
            logger.error("Failed to parse resume ID {}: {}", resumeId, ex.getMessage(), ex);
            parsedEntity.setParsingStatus("FAILED");
            String friendlyMsg = ex.getMessage();
            if (friendlyMsg == null || friendlyMsg.contains("Failed to extract") || friendlyMsg.contains("NullPointer") || friendlyMsg.contains("Exception")) {
                friendlyMsg = "Unable to extract readable text from this PDF. Please upload a text-based PDF or DOCX resume.";
            }
            parsedEntity.setErrorMessage(friendlyMsg);
            parsedEntity.setParsedAt(LocalDateTime.now());
        }

        ParsedResume saved = parsedResumeRepository.save(parsedEntity);
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ParsedResumeDto getParsedResumeByResumeId(Long resumeId) {
        Optional<ParsedResume> opt = parsedResumeRepository.findByResumeId(resumeId);
        return opt.map(this::mapToDto).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public ParsedResumeDto getParsedResumeForStudent(String usernameOrEmail) {
        Optional<Resume> resumeOpt = resumeRepository.findByStudentProfileUserUsernameAndIsCurrentTrue(usernameOrEmail);
        if (resumeOpt.isEmpty()) {
            resumeOpt = resumeRepository.findByStudentProfileUserEmailAndIsCurrentTrue(usernameOrEmail);
        }

        if (resumeOpt.isPresent()) {
            return getParsedResumeByResumeId(resumeOpt.get().getId());
        }
        return null;
    }

    private ParsedResumeDto mapToDto(ParsedResume entity) {
        if (entity == null) return null;

        List<String> skillList = new ArrayList<>();
        if (entity.getSkills() != null && !entity.getSkills().trim().isEmpty()) {
            String[] split = entity.getSkills().split(",");
            for (String s : split) {
                if (!s.trim().isEmpty()) {
                    skillList.add(s.trim());
                }
            }
        }

        return ParsedResumeDto.builder()
                .id(entity.getId())
                .resumeId(entity.getResume() != null ? entity.getResume().getId() : null)
                .studentId(entity.getStudentProfile() != null ? entity.getStudentProfile().getId() : null)
                .rawText(entity.getRawText())
                .summary(entity.getSummary())
                .skillsRaw(entity.getSkills())
                .skills(skillList)
                .education(entity.getEducation())
                .experience(entity.getExperience())
                .projects(entity.getProjects())
                .certifications(entity.getCertifications())
                .parsingStatus(entity.getParsingStatus() != null ? entity.getParsingStatus() : "NOT_PARSED")
                .errorMessage(entity.getErrorMessage())
                .parserVersion(entity.getParserVersion())
                .parsedAt(entity.getParsedAt())
                .formattedParsedAt(entity.getParsedAt() != null ? entity.getParsedAt().format(DATE_FORMATTER) : "")
                .build();
    }
}
