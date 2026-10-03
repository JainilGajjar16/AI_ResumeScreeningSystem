package com.resume.screening.parser;

import com.resume.screening.dto.ParsedJobRequirementDto;
import com.resume.screening.entity.JobPost;
import com.resume.screening.repository.JobPostRepository;
import com.resume.screening.repository.ParsedJobRequirementRepository;
import com.resume.screening.repository.SkillRepository;
import com.resume.screening.service.impl.JobDescriptionParsingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class Module5ParsingTests {

    private PdfResumeParser pdfResumeParser;
    private DocxResumeParser docxResumeParser;
    private ResumeTextCleaner textCleaner;
    private ResumeSectionDetector sectionDetector;
    private SkillDictionaryService skillDictionaryService;

    @Mock
    private SkillRepository skillRepository;

    @Mock
    private JobPostRepository jobPostRepository;

    @Mock
    private ParsedJobRequirementRepository parsedJobRequirementRepository;

    private JobDescriptionParsingServiceImpl jobParsingService;

    @BeforeEach
    void setUp() {
        OcrService ocrService = new OcrService();
        pdfResumeParser = new PdfResumeParser(ocrService);
        docxResumeParser = new DocxResumeParser();
        textCleaner = new ResumeTextCleaner();
        sectionDetector = new ResumeSectionDetector();
        skillDictionaryService = new SkillDictionaryService(skillRepository);

        jobParsingService = new JobDescriptionParsingServiceImpl(
                jobPostRepository,
                parsedJobRequirementRepository,
                skillDictionaryService
        );
    }

    @Test
    @DisplayName("Test 1: PdfResumeParser handles missing and empty files gracefully")
    void testPdfParserErrorHandling() {
        assertThrows(IllegalArgumentException.class, () -> pdfResumeParser.extractText(null));
        assertThrows(IllegalArgumentException.class, () -> pdfResumeParser.extractText(new File("non_existent_file.pdf")));
    }

    @Test
    @DisplayName("Test 2: DocxResumeParser handles missing and empty files gracefully")
    void testDocxParserErrorHandling() {
        assertThrows(IllegalArgumentException.class, () -> docxResumeParser.extractText(null));
        assertThrows(IllegalArgumentException.class, () -> docxResumeParser.extractText(new File("non_existent_file.docx")));
    }

    @Test
    @DisplayName("Test 3: ResumeTextCleaner normalizes line endings and whitespace without losing content")
    void testTextCleaner() {
        String raw = "John Doe \r\n\r\n Software Engineer\n\n\nSkills:\r\n  Java   Spring Boot   MySQL  ";
        String cleaned = textCleaner.cleanText(raw);

        assertTrue(cleaned.contains("John Doe"));
        assertTrue(cleaned.contains("Software Engineer"));
        assertTrue(cleaned.contains("Java Spring Boot MySQL"));
        assertFalse(cleaned.contains("\r"));
    }

    @Test
    @DisplayName("Test 4: ResumeSectionDetector identifies education, skills, experience, and projects")
    void testSectionDetector() {
        String resumeText = "SUMMARY\n" +
                "Enthusiastic Java Developer.\n" +
                "TECHNICAL SKILLS\n" +
                "Java, Spring Boot, MySQL, REST API, Git\n" +
                "EDUCATION\n" +
                "B.Tech in Computer Science from ABC University\n" +
                "WORK EXPERIENCE\n" +
                "Software Engineer Intern at XYZ Corp\n" +
                "PROJECTS\n" +
                "AI Resume Screening System\n";

        Map<ResumeSectionDetector.SectionType, String> sections = sectionDetector.parseSections(resumeText);

        assertEquals("Enthusiastic Java Developer.", sections.get(ResumeSectionDetector.SectionType.SUMMARY));
        assertEquals("Java, Spring Boot, MySQL, REST API, Git", sections.get(ResumeSectionDetector.SectionType.SKILLS));
        assertEquals("B.Tech in Computer Science from ABC University", sections.get(ResumeSectionDetector.SectionType.EDUCATION));
        assertEquals("Software Engineer Intern at XYZ Corp", sections.get(ResumeSectionDetector.SectionType.EXPERIENCE));
        assertEquals("AI Resume Screening System", sections.get(ResumeSectionDetector.SectionType.PROJECTS));
    }

    @Test
    @DisplayName("Test 5: SkillDictionaryService extracts canonical skills and removes duplicates")
    void testSkillExtraction() {
        String sampleText = "Experienced with java, Java, Spring Boot, spring boot, MySQL, REST API, Git and Docker.";
        List<String> skills = skillDictionaryService.extractSkills(sampleText);

        assertTrue(skills.contains("Java"));
        assertTrue(skills.contains("Spring Boot"));
        assertTrue(skills.contains("MySQL"));
        assertTrue(skills.contains("REST API"));
        assertTrue(skills.contains("Git"));
        assertTrue(skills.contains("Docker"));

        // Verify duplicate elimination
        long javaCount = skills.stream().filter(s -> s.equalsIgnoreCase("Java")).count();
        assertEquals(1, javaCount, "Java should appear only once in extracted list.");
    }

    @Test
    @DisplayName("Test 6: JobDescriptionParsingService parses job description and extracts required skills")
    void testJobDescriptionParsing() {
        JobPost job = JobPost.builder()
                .id(101L)
                .title("Java Software Engineer")
                .companyName("Tech Corp")
                .requiredSkills("Java, Spring Boot, MySQL")
                .qualification("B.Tech / B.E.")
                .experienceRequired("1-3 Years")
                .description("We are looking for a Java developer with Spring Boot, MySQL, REST API and Git experience.")
                .build();

        when(jobPostRepository.findById(101L)).thenReturn(Optional.of(job));
        when(parsedJobRequirementRepository.findByJobPostId(101L)).thenReturn(Optional.empty());
        when(parsedJobRequirementRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ParsedJobRequirementDto dto = jobParsingService.parseAndSaveJobRequirement(101L);

        assertNotNull(dto);
        assertEquals("PARSED", dto.getParsingStatus());
        assertTrue(dto.getRequiredSkills().contains("Java"));
        assertTrue(dto.getRequiredSkills().contains("Spring Boot"));
        assertTrue(dto.getRequiredSkills().contains("MySQL"));
        assertTrue(dto.getRequiredSkills().contains("REST API"));
        assertTrue(dto.getRequiredSkills().contains("Git"));
    }
}
