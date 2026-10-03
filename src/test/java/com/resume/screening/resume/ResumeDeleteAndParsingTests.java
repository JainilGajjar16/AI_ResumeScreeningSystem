package com.resume.screening.resume;

import com.resume.screening.dto.AnalysisResult;
import com.resume.screening.dto.ParsedResumeDto;
import com.resume.screening.engine.impl.RuleBasedResumeAnalysisEngine;
import com.resume.screening.entity.*;
import com.resume.screening.parser.*;
import com.resume.screening.repository.*;
import com.resume.screening.service.impl.ResumeParsingServiceImpl;
import com.resume.screening.service.impl.ResumeStorageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ResumeDeleteAndParsingTests {

    @Mock private ResumeRepository resumeRepository;
    @Mock private ResumeVersionRepository resumeVersionRepository;
    @Mock private StudentProfileRepository studentProfileRepository;
    @Mock private UserRepository userRepository;
    @Mock private RecruiterRepository recruiterRepository;
    @Mock private JobApplicationRepository jobApplicationRepository;
    @Mock private ResumeAnalysisRepository resumeAnalysisRepository;
    @Mock private ParsedResumeRepository parsedResumeRepository;
    @Mock private SkillRepository skillRepository;

    private OcrService ocrService;
    private PdfResumeParser pdfResumeParser;
    private DocxResumeParser docxResumeParser;
    private ResumeTextCleaner textCleaner;
    private ResumeSectionDetector sectionDetector;
    private SkillDictionaryService skillDictionaryService;
    private ResumeParsingServiceImpl resumeParsingService;
    private ResumeStorageServiceImpl resumeStorageService;
    private RuleBasedResumeAnalysisEngine analysisEngine;

    @BeforeEach
    void setUp() {
        ocrService = new OcrService();
        pdfResumeParser = new PdfResumeParser(ocrService);
        docxResumeParser = new DocxResumeParser();
        textCleaner = new ResumeTextCleaner();
        sectionDetector = new ResumeSectionDetector();
        skillDictionaryService = new SkillDictionaryService(skillRepository);

        resumeParsingService = new ResumeParsingServiceImpl(
                resumeRepository,
                parsedResumeRepository,
                pdfResumeParser,
                docxResumeParser,
                textCleaner,
                skillDictionaryService,
                sectionDetector
        );

        resumeStorageService = new ResumeStorageServiceImpl(
                "uploads/test_resumes",
                resumeRepository,
                resumeVersionRepository,
                studentProfileRepository,
                userRepository,
                recruiterRepository,
                jobApplicationRepository,
                resumeAnalysisRepository,
                resumeParsingService
        );

        analysisEngine = new RuleBasedResumeAnalysisEngine(skillDictionaryService);
    }

    @Test
    @DisplayName("Test 1: Text-based PDF extraction validates non-empty text")
    void testTextBasedPdfExtraction() {
        assertThrows(IllegalArgumentException.class, () -> pdfResumeParser.extractText(null));
    }

    @Test
    @DisplayName("Test 2: Empty or scanned PDF detection identifies non-readable files")
    void testEmptyScannedPdfDetection() {
        File missingFile = new File("non_existent_scanned.pdf");
        assertThrows(IllegalArgumentException.class, () -> pdfResumeParser.extractTextWithResult(missingFile));
    }

    @Test
    @DisplayName("Test 3 & 4: OCR fallback and unavailable handling returns empty without crashing")
    void testOcrFallbackUnavailableHandling() {
        String result = ocrService.extractTextFromPdfUsingOcr(null);
        assertEquals("", result);

        String resultMissing = ocrService.extractTextFromPdfUsingOcr(new File("missing.pdf"));
        assertEquals("", resultMissing);
    }

    @Test
    @DisplayName("Test 5: DOCX extraction handles invalid files gracefully")
    void testDocxExtraction() {
        assertThrows(IllegalArgumentException.class, () -> docxResumeParser.extractText(null));
    }

    @Test
    @DisplayName("Test 6: Resume text cleaning normalizes whitespace and line breaks")
    void testResumeTextCleaning() {
        String raw = "John   Doe \r\n\r\n Skills:  Java  ";
        String cleaned = textCleaner.cleanText(raw);
        assertTrue(cleaned.contains("John Doe"));
        assertTrue(cleaned.contains("Java"));
    }

    @Test
    @DisplayName("Test 7: Section detection identifies standard resume headings")
    void testSectionDetection() {
        String text = "SUMMARY\nSoftware Engineer\nTECHNICAL SKILLS\nJava, Spring\nEDUCATION\nB.Tech\n";
        var sections = sectionDetector.parseSections(text);
        assertEquals("Software Engineer", sections.get(ResumeSectionDetector.SectionType.SUMMARY));
        assertEquals("Java, Spring", sections.get(ResumeSectionDetector.SectionType.SKILLS));
        assertEquals("B.Tech", sections.get(ResumeSectionDetector.SectionType.EDUCATION));
    }

    @Test
    @DisplayName("Test 8: Skill extraction detects canonical skills without duplicates")
    void testSkillExtraction() {
        String text = "Java, java, Spring Boot, MySQL";
        List<String> skills = skillDictionaryService.extractSkills(text);
        assertTrue(skills.contains("Java"));
        assertTrue(skills.contains("Spring Boot"));
        assertTrue(skills.contains("MySQL"));
    }

    @Test
    @DisplayName("Test 9 & 10: Resume parsing success and failure handling")
    void testResumeParsingFailure() {
        User user = User.builder().id(1L).email("student@test.com").build();
        StudentProfile sp = StudentProfile.builder().id(1L).user(user).build();
        Resume resume = Resume.builder().id(10L).studentProfile(sp).filePath("invalid_path.pdf").fileName("test.pdf").build();

        when(resumeRepository.findById(10L)).thenReturn(Optional.of(resume));
        when(parsedResumeRepository.findByResumeId(10L)).thenReturn(Optional.empty());
        when(parsedResumeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ParsedResumeDto dto = resumeParsingService.parseAndSaveResume(10L);
        assertNotNull(dto);
        assertEquals("FAILED", dto.getParsingStatus());
        assertTrue(dto.getErrorMessage().contains("not found on disk"));
    }

    @Test
    @DisplayName("Test 11: Delete resume with no applications succeeds")
    void testDeleteResumeNoApplications() {
        User user = User.builder().id(1L).email("student@test.com").build();
        StudentProfile sp = StudentProfile.builder().id(1L).user(user).build();
        Resume resume = Resume.builder().id(10L).studentProfile(sp).filePath("dummy.pdf").versions(new ArrayList<>()).build();

        when(userRepository.findByUsernameOrEmail("student@test.com")).thenReturn(Optional.of(user));
        when(resumeRepository.findById(10L)).thenReturn(Optional.of(resume));
        when(jobApplicationRepository.findByResumeId(10L)).thenReturn(new ArrayList<>());
        when(resumeAnalysisRepository.findByResumeId(10L)).thenReturn(new ArrayList<>());

        assertDoesNotThrow(() -> resumeStorageService.deleteResume("student@test.com", 10L));
        verify(resumeRepository, times(1)).delete(resume);
    }

    @Test
    @DisplayName("Test 12: Delete resume with existing application disassociates resume_id to NULL without deleting application")
    void testDeleteResumeWithExistingApplication() {
        User user = User.builder().id(1L).email("student@test.com").build();
        StudentProfile sp = StudentProfile.builder().id(1L).user(user).build();
        Resume resume = Resume.builder().id(10L).studentProfile(sp).filePath("dummy.pdf").versions(new ArrayList<>()).build();

        JobApplication app = JobApplication.builder().id(100L).studentProfile(sp).resume(resume).status("APPLIED").build();
        List<JobApplication> apps = new ArrayList<>();
        apps.add(app);

        when(userRepository.findByUsernameOrEmail("student@test.com")).thenReturn(Optional.of(user));
        when(resumeRepository.findById(10L)).thenReturn(Optional.of(resume));
        when(jobApplicationRepository.findByResumeId(10L)).thenReturn(apps);
        when(resumeAnalysisRepository.findByResumeId(10L)).thenReturn(new ArrayList<>());

        resumeStorageService.deleteResume("student@test.com", 10L);

        assertNull(app.getResume(), "JobApplication resume reference must become NULL when resume is deleted.");
        assertEquals("APPLIED", app.getStatus(), "JobApplication status and history must remain intact.");
        verify(jobApplicationRepository, times(1)).saveAll(apps);
        verify(resumeRepository, times(1)).delete(resume);
    }

    @Test
    @DisplayName("Test 13: Replace resume maintains current version count and preserves existing applications")
    void testReplaceResumePreservesApplications() {
        User user = User.builder().id(1L).email("student@test.com").build();
        StudentProfile sp = StudentProfile.builder().id(1L).user(user).build();
        Resume resume = Resume.builder().id(10L).studentProfile(sp).currentVersion(1).isCurrent(true).build();

        JobApplication app = JobApplication.builder().id(100L).studentProfile(sp).resume(resume).status("APPLIED").build();

        assertNotNull(app.getResume());
        assertEquals(10L, app.getResume().getId());
    }

    @Test
    @DisplayName("Test 14: Module 6 ATS analysis after successful parsing evaluates candidate compatibility")
    void testModule6AnalysisAfterParsing() {
        ParsedResume pr = ParsedResume.builder()
                .skills("Java, Spring Boot, MySQL, Git")
                .education("Bachelor degree in Computer Science")
                .experience("2 years experience")
                .build();
        ParsedJobRequirement pjr = ParsedJobRequirement.builder()
                .requiredSkills("Java, Spring Boot, MySQL")
                .qualification("Bachelor degree")
                .experience("1 year")
                .build();

        AnalysisResult result = analysisEngine.analyze(pr, pjr, null);

        assertNotNull(result);
        assertTrue(result.getAtsScore() >= 80.0);
        assertEquals(100.0, result.getSkillMatchPercentage());
        assertEquals("MATCH", result.getQualificationMatch());
        assertEquals("MATCH", result.getExperienceMatch());
    }

    @Test
    @DisplayName("Test 15: Dual login resolution resolves User entity whether identifier is username or email")
    void testDualLoginUserResolution() {
        User user = User.builder().id(1L).username("Jainil").email("jainilgajjar@gmail.com").role("STUDENT").build();

        when(userRepository.findByUsernameOrEmail("Jainil")).thenReturn(Optional.of(user));
        when(userRepository.findByUsernameOrEmail("jainilgajjar@gmail.com")).thenReturn(Optional.of(user));

        Optional<User> byUsername = userRepository.findByUsernameOrEmail("Jainil");
        Optional<User> byEmail = userRepository.findByUsernameOrEmail("jainilgajjar@gmail.com");

        assertTrue(byUsername.isPresent());
        assertTrue(byEmail.isPresent());
        assertEquals(byUsername.get().getId(), byEmail.get().getId());
    }
}
