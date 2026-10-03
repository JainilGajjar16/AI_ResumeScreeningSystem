package com.resume.screening.analysis;

import com.resume.screening.dto.AnalysisResult;
import com.resume.screening.entity.*;
import com.resume.screening.engine.impl.RuleBasedResumeAnalysisEngine;
import com.resume.screening.repository.*;
import com.resume.screening.parser.SkillDictionaryService;
import com.resume.screening.service.impl.ResumeAnalysisServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class Module6AnalysisTests {

    @Mock
    private SkillRepository skillRepository;

    @Mock
    private ResumeRepository resumeRepository;

    @Mock
    private JobPostRepository jobPostRepository;

    @Mock
    private ParsedResumeRepository parsedResumeRepository;

    @Mock
    private ParsedJobRequirementRepository parsedJobRequirementRepository;

    @Mock
    private ResumeAnalysisRepository resumeAnalysisRepository;

    @Mock
    private UserRepository userRepository;

    private SkillDictionaryService skillDictionaryService;
    private RuleBasedResumeAnalysisEngine analysisEngine;
    private ResumeAnalysisServiceImpl resumeAnalysisService;

    @BeforeEach
    void setUp() {
        skillDictionaryService = new SkillDictionaryService(skillRepository);
        analysisEngine = new RuleBasedResumeAnalysisEngine(skillDictionaryService);

        resumeAnalysisService = new ResumeAnalysisServiceImpl(
                resumeRepository,
                jobPostRepository,
                parsedResumeRepository,
                parsedJobRequirementRepository,
                resumeAnalysisRepository,
                userRepository,
                analysisEngine
        );
    }

    @Test
    @DisplayName("Test 1: Exact skill match calculates 100% skill match")
    void testExactSkillMatch() {
        ParsedResume pr = ParsedResume.builder()
                .skills("Java, Spring Boot, MySQL, Git")
                .rawText("Skills: Java, Spring Boot, MySQL, Git")
                .build();
        ParsedJobRequirement pjr = ParsedJobRequirement.builder()
                .requiredSkills("Java, Spring Boot, MySQL, Git")
                .build();

        AnalysisResult result = analysisEngine.analyze(pr, pjr, null);

        assertEquals(100.0, result.getSkillMatchPercentage());
        assertEquals(4, result.getMatchedSkills().size());
        assertTrue(result.getMissingSkills().isEmpty());
    }

    @Test
    @DisplayName("Test 2: Partial skill match calculates correct percentage and missing skills")
    void testPartialSkillMatch() {
        ParsedResume pr = ParsedResume.builder()
                .skills("Java, Spring Boot, MySQL, Git")
                .rawText("Skills: Java, Spring Boot, MySQL, Git")
                .build();
        ParsedJobRequirement pjr = ParsedJobRequirement.builder()
                .requiredSkills("Java, Spring Boot, MySQL, REST API, Git")
                .build();

        AnalysisResult result = analysisEngine.analyze(pr, pjr, null);

        assertEquals(80.0, result.getSkillMatchPercentage());
        assertEquals(4, result.getMatchedSkills().size());
        assertEquals(1, result.getMissingSkills().size());
        assertTrue(result.getMissingSkills().contains("REST API"));
    }

    @Test
    @DisplayName("Test 3: No skill match produces 0% skill match and lists missing skills")
    void testNoSkillMatch() {
        ParsedResume pr = ParsedResume.builder()
                .skills("Python, Django, PostgreSQL")
                .rawText("Skills: Python, Django, PostgreSQL")
                .build();
        ParsedJobRequirement pjr = ParsedJobRequirement.builder()
                .requiredSkills("Java, Spring Boot, MySQL")
                .build();

        AnalysisResult result = analysisEngine.analyze(pr, pjr, null);

        assertEquals(0.0, result.getSkillMatchPercentage());
        assertTrue(result.getMatchedSkills().isEmpty());
        assertEquals(3, result.getMissingSkills().size());
    }

    @Test
    @DisplayName("Test 4: Case-insensitive skill matching works correctly")
    void testCaseInsensitiveSkillMatching() {
        ParsedResume pr = ParsedResume.builder()
                .skills("java, spring boot, mysql")
                .rawText("Skills: java, spring boot, mysql")
                .build();
        ParsedJobRequirement pjr = ParsedJobRequirement.builder()
                .requiredSkills("JAVA, Spring Boot, MySQL")
                .build();

        AnalysisResult result = analysisEngine.analyze(pr, pjr, null);

        assertEquals(100.0, result.getSkillMatchPercentage());
        assertEquals(3, result.getMatchedSkills().size());
    }

    @Test
    @DisplayName("Test 5: Java does not falsely match JavaScript")
    void testJavaDoesNotMatchJavaScript() {
        ParsedResume pr = ParsedResume.builder()
                .skills("JavaScript, React, Node.js")
                .rawText("Skills: JavaScript, React, Node.js")
                .build();
        ParsedJobRequirement pjr = ParsedJobRequirement.builder()
                .requiredSkills("Java, Spring Boot")
                .build();

        AnalysisResult result = analysisEngine.analyze(pr, pjr, null);

        assertEquals(0.0, result.getSkillMatchPercentage());
        assertFalse(result.getMatchedSkills().contains("Java"));
        assertTrue(result.getMissingSkills().contains("Java"));
    }

    @Test
    @DisplayName("Test 6: Qualification match evaluates degree level")
    void testQualificationMatch() {
        ParsedResume pr = ParsedResume.builder()
                .education("B.Tech in Computer Science from National Institute")
                .skills("Java")
                .build();
        ParsedJobRequirement pjr = ParsedJobRequirement.builder()
                .qualification("Bachelor degree in Computer Science")
                .requiredSkills("Java")
                .build();

        AnalysisResult result = analysisEngine.analyze(pr, pjr, null);

        assertEquals("MATCH", result.getQualificationMatch());
        assertTrue(result.getQualificationScore() > 0);
    }

    @Test
    @DisplayName("Test 7: Qualification partial match handles lower degree levels")
    void testQualificationPartialMatch() {
        ParsedResume pr = ParsedResume.builder()
                .education("Diploma in Information Technology")
                .skills("Java")
                .build();
        ParsedJobRequirement pjr = ParsedJobRequirement.builder()
                .qualification("Bachelor degree in Computer Science")
                .requiredSkills("Java")
                .build();

        AnalysisResult result = analysisEngine.analyze(pr, pjr, null);

        assertEquals("PARTIAL_MATCH", result.getQualificationMatch());
    }

    @Test
    @DisplayName("Test 8: Experience match evaluates candidate vs job experience")
    void testExperienceMatch() {
        ParsedResume pr = ParsedResume.builder()
                .experience("Software Engineer for 3 years at Tech Corp")
                .skills("Java")
                .build();
        ParsedJobRequirement pjr = ParsedJobRequirement.builder()
                .experience("2 years experience")
                .requiredSkills("Java")
                .build();

        AnalysisResult result = analysisEngine.analyze(pr, pjr, null);

        assertEquals("MATCH", result.getExperienceMatch());
    }

    @Test
    @DisplayName("Test 9: Missing experience handles missing text gracefully as NOT_AVAILABLE")
    void testMissingExperience() {
        ParsedResume pr = ParsedResume.builder()
                .skills("Java")
                .build();
        ParsedJobRequirement pjr = ParsedJobRequirement.builder()
                .experience("2 years experience")
                .requiredSkills("Java")
                .build();

        AnalysisResult result = analysisEngine.analyze(pr, pjr, null);

        assertEquals("NOT_AVAILABLE", result.getExperienceMatch());
    }

    @Test
    @DisplayName("Test 10: Resume completeness score evaluates section structure")
    void testResumeCompleteness() {
        ParsedResume completeResume = ParsedResume.builder()
                .summary("Enthusiastic developer")
                .skills("Java, Spring")
                .education("BCA")
                .experience("1 year intern")
                .projects("AI Resume Screening")
                .certifications("AWS Certified")
                .build();

        ParsedResume minimalResume = ParsedResume.builder()
                .skills("Java")
                .build();

        ParsedJobRequirement pjr = ParsedJobRequirement.builder().requiredSkills("Java").build();

        AnalysisResult resComplete = analysisEngine.analyze(completeResume, pjr, null);
        AnalysisResult resMinimal = analysisEngine.analyze(minimalResume, pjr, null);

        assertTrue(resComplete.getResumeCompletenessScore() > resMinimal.getResumeCompletenessScore());
    }

    @Test
    @DisplayName("Test 11: Score calculation produces transparent weighted total")
    void testScoreCalculation() {
        ParsedResume pr = ParsedResume.builder()
                .skills("Java, Spring Boot, MySQL")
                .education("B.Tech in Computer Science")
                .experience("2 years")
                .summary("Software Developer")
                .build();
        ParsedJobRequirement pjr = ParsedJobRequirement.builder()
                .requiredSkills("Java, Spring Boot, MySQL")
                .qualification("Bachelor degree")
                .experience("2 years")
                .build();

        AnalysisResult result = analysisEngine.analyze(pr, pjr, null);

        assertNotNull(result.getAtsScore());
        assertTrue(result.getAtsScore() >= 80.0);
        assertNotNull(result.getSkillsMatchScore());
        assertNotNull(result.getExperienceScore());
        assertNotNull(result.getQualificationScore());
    }

    @Test
    @DisplayName("Test 12: ATS Score cannot exceed 100")
    void testScoreCannotExceed100() {
        ParsedResume pr = ParsedResume.builder()
                .summary("Full summary")
                .skills("Java, Spring Boot, MySQL, Git, Docker, AWS")
                .education("PhD in Computer Science")
                .experience("10 years experience")
                .projects("Many projects")
                .certifications("Many certs")
                .rawText("Java Spring Boot MySQL Git Docker AWS PhD Computer Science 10 years experience")
                .build();
        ParsedJobRequirement pjr = ParsedJobRequirement.builder()
                .requiredSkills("Java, Spring Boot, MySQL, Git, Docker, AWS")
                .qualification("Bachelor degree")
                .experience("2 years")
                .keywords("Java, Spring, MySQL")
                .build();

        AnalysisResult result = analysisEngine.analyze(pr, pjr, null);

        assertTrue(result.getAtsScore() <= 100.0);
    }

    @Test
    @DisplayName("Test 13: ATS Score cannot go below 0")
    void testScoreCannotGoBelow0() {
        ParsedResume pr = ParsedResume.builder().build();
        ParsedJobRequirement pjr = ParsedJobRequirement.builder().requiredSkills("NonExistentSkill").build();

        AnalysisResult result = analysisEngine.analyze(pr, pjr, null);

        assertTrue(result.getAtsScore() >= 0.0);
        assertFalse(result.getAtsScore().isNaN());
        assertFalse(result.getAtsScore().isInfinite());
    }

    @Test
    @DisplayName("Test 14: Missing ParsedResume throws IllegalStateException")
    void testMissingParsedResumeThrows() {
        User user = User.builder().id(1L).email("student@test.com").role("ROLE_STUDENT").build();
        StudentProfile sp = StudentProfile.builder().id(1L).user(user).build();
        Resume resume = Resume.builder().id(10L).studentProfile(sp).currentVersion(1).build();
        JobPost job = JobPost.builder().id(20L).build();

        when(resumeRepository.findById(10L)).thenReturn(Optional.of(resume));
        when(jobPostRepository.findById(20L)).thenReturn(Optional.of(job));
        when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(user));
        when(parsedResumeRepository.findByResumeId(10L)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () ->
                resumeAnalysisService.analyzeResumeForJob(10L, 20L, "student@test.com")
        );
    }

    @Test
    @DisplayName("Test 15: Missing ParsedJobRequirement throws IllegalStateException")
    void testMissingParsedJobRequirementThrows() {
        User user = User.builder().id(1L).email("student@test.com").role("ROLE_STUDENT").build();
        StudentProfile sp = StudentProfile.builder().id(1L).user(user).build();
        Resume resume = Resume.builder().id(10L).studentProfile(sp).currentVersion(1).build();
        JobPost job = JobPost.builder().id(20L).build();
        ParsedResume pr = ParsedResume.builder().id(1L).resume(resume).parsingStatus("PARSED").build();

        when(resumeRepository.findById(10L)).thenReturn(Optional.of(resume));
        when(jobPostRepository.findById(20L)).thenReturn(Optional.of(job));
        when(userRepository.findByEmail("student@test.com")).thenReturn(Optional.of(user));
        when(parsedResumeRepository.findByResumeId(10L)).thenReturn(Optional.of(pr));
        when(parsedJobRequirementRepository.findByJobPostId(20L)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () ->
                resumeAnalysisService.analyzeResumeForJob(10L, 20L, "student@test.com")
        );
    }

    @Test
    @DisplayName("Test 16: Student ownership check prevents analyzing unowned resume")
    void testStudentOwnershipVerification() {
        User owner = User.builder().id(1L).email("owner@test.com").role("ROLE_STUDENT").build();
        User stranger = User.builder().id(2L).email("stranger@test.com").role("ROLE_STUDENT").build();
        StudentProfile sp = StudentProfile.builder().id(1L).user(owner).build();
        Resume resume = Resume.builder().id(10L).studentProfile(sp).currentVersion(1).build();
        JobPost job = JobPost.builder().id(20L).build();

        when(resumeRepository.findById(10L)).thenReturn(Optional.of(resume));
        when(jobPostRepository.findById(20L)).thenReturn(Optional.of(job));
        when(userRepository.findByEmail("stranger@test.com")).thenReturn(Optional.of(stranger));

        assertThrows(AccessDeniedException.class, () ->
                resumeAnalysisService.analyzeResumeForJob(10L, 20L, "stranger@test.com")
        );
    }

    @Test
    @DisplayName("Test 17: Recruiter job ownership check prevents analyzing candidate for unowned job")
    void testRecruiterJobOwnershipVerification() {
        User ownerRecruiter = User.builder().id(1L).email("recruiter1@test.com").role("ROLE_RECRUITER").build();
        User strangerRecruiter = User.builder().id(2L).email("recruiter2@test.com").role("ROLE_RECRUITER").build();
        Recruiter recruiter = Recruiter.builder().id(1L).user(ownerRecruiter).build();

        JobPost job = JobPost.builder().id(20L).recruiter(recruiter).build();
        Resume resume = Resume.builder().id(10L).build();

        when(resumeRepository.findById(10L)).thenReturn(Optional.of(resume));
        when(jobPostRepository.findById(20L)).thenReturn(Optional.of(job));
        when(userRepository.findByEmail("recruiter2@test.com")).thenReturn(Optional.of(strangerRecruiter));

        assertThrows(AccessDeniedException.class, () ->
                resumeAnalysisService.analyzeResumeForJob(10L, 20L, "recruiter2@test.com")
        );
    }

    @Test
    @DisplayName("Test 18: Sample Integration Scenario with complete resume & job post")
    void testSampleIntegrationScenario() {
        ParsedResume pr = ParsedResume.builder()
                .skills("Java, Spring Boot, MySQL, Git, HTML")
                .education("BCA")
                .experience("1 year")
                .projects("AI Resume Screening System, E-commerce App")
                .rawText("Java Spring Boot MySQL Git HTML BCA 1 year experience AI Resume Screening System")
                .build();

        ParsedJobRequirement pjr = ParsedJobRequirement.builder()
                .requiredSkills("Java, Spring Boot, MySQL, REST API, Git")
                .qualification("Bachelor degree")
                .experience("1 year")
                .build();

        AnalysisResult result = analysisEngine.analyze(pr, pjr, null);

        assertTrue(result.getMatchedSkills().contains("Java"));
        assertTrue(result.getMatchedSkills().contains("Spring Boot"));
        assertTrue(result.getMatchedSkills().contains("MySQL"));
        assertTrue(result.getMatchedSkills().contains("Git"));
        assertTrue(result.getMissingSkills().contains("REST API"));
        assertEquals(80.0, result.getSkillMatchPercentage());
        assertEquals("MATCH", result.getQualificationMatch());
        assertEquals("MATCH", result.getExperienceMatch());
        assertTrue(result.getAtsScore() > 0.0 && result.getAtsScore() <= 100.0);
    }

    @Test
    @DisplayName("Test 19: getLatestAnalysisForStudent returns latest analysis by timestamp")
    void testGetLatestAnalysisForStudent() {
        User studentUser = User.builder().id(100L).email("student100@test.com").role("ROLE_STUDENT").build();
        StudentProfile sp = StudentProfile.builder().id(50L).user(studentUser).build();
        Resume resume = Resume.builder().id(10L).studentProfile(sp).isCurrent(true).build();

        ResumeAnalysis a1 = ResumeAnalysis.builder().id(1L).studentProfile(sp).atsScore(70.0).analyzedAt(java.time.LocalDateTime.now().minusDays(1)).build();
        ResumeAnalysis a2 = ResumeAnalysis.builder().id(2L).studentProfile(sp).atsScore(85.0).analyzedAt(java.time.LocalDateTime.now()).build();

        when(resumeRepository.findByStudentProfileUserEmailAndIsCurrentTrue("student100@test.com")).thenReturn(Optional.of(resume));
        when(resumeAnalysisRepository.findByStudentProfileId(50L)).thenReturn(java.util.Arrays.asList(a1, a2));

        com.resume.screening.dto.ResumeAnalysisDto result = resumeAnalysisService.getLatestAnalysisForStudent("student100@test.com");

        assertNotNull(result);
        assertEquals(85.0, result.getAtsScore());
    }
}
