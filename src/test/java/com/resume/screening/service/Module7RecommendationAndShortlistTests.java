package com.resume.screening.service;

import com.resume.screening.config.CandidateRecommendationConfig;
import com.resume.screening.dto.CandidateRecommendationDto;
import com.resume.screening.dto.ShortlistedCandidateDto;
import com.resume.screening.entity.*;
import com.resume.screening.repository.*;
import com.resume.screening.service.impl.CandidateRecommendationServiceImpl;
import com.resume.screening.service.impl.ShortlistServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class Module7RecommendationAndShortlistTests {

    @Mock
    private JobPostRepository jobPostRepository;

    @Mock
    private JobApplicationRepository jobApplicationRepository;

    @Mock
    private ResumeAnalysisRepository resumeAnalysisRepository;

    @Mock
    private ShortlistedCandidateRepository shortlistedCandidateRepository;

    @Mock
    private RecruiterRepository recruiterRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudentProfileRepository studentProfileRepository;

    @Mock
    private UserResolutionService userResolutionService;

    private CandidateRecommendationConfig recommendationConfig;

    private CandidateRecommendationServiceImpl candidateRecommendationService;

    private ShortlistServiceImpl shortlistService;

    private User recruiterUser;
    private User otherRecruiterUser;
    private Recruiter recruiter;
    private Recruiter otherRecruiter;
    private JobPost jobPost;

    private StudentProfile student1;
    private StudentProfile student2;
    private StudentProfile student3;

    private Resume resume1;
    private Resume resume2;
    private Resume resume3;

    private JobApplication app1;
    private JobApplication app2;
    private JobApplication app3;

    private ResumeAnalysis analysis1;
    private ResumeAnalysis analysis2;

    @BeforeEach
    void setUp() {
        recommendationConfig = new CandidateRecommendationConfig();

        candidateRecommendationService = new CandidateRecommendationServiceImpl(
                jobPostRepository,
                jobApplicationRepository,
                resumeAnalysisRepository,
                shortlistedCandidateRepository,
                recruiterRepository,
                userRepository,
                userResolutionService,
                recommendationConfig
        );

        shortlistService = new ShortlistServiceImpl(
                shortlistedCandidateRepository,
                jobPostRepository,
                jobApplicationRepository,
                studentProfileRepository,
                recruiterRepository,
                resumeAnalysisRepository,
                userResolutionService
        );

        // Recruiter 1
        recruiterUser = User.builder()
                .id(1L)
                .username("recruiter1")
                .email("recruiter1@company.com")
                .role("RECRUITER")
                .build();

        recruiter = Recruiter.builder()
                .id(10L)
                .user(recruiterUser)
                .companyName("TechCorp")
                .build();

        // Recruiter 2 (Other)
        otherRecruiterUser = User.builder()
                .id(2L)
                .username("recruiter2")
                .email("recruiter2@company.com")
                .role("RECRUITER")
                .build();

        otherRecruiter = Recruiter.builder()
                .id(20L)
                .user(otherRecruiterUser)
                .companyName("OtherCorp")
                .build();

        // Job Post owned by Recruiter 1
        jobPost = JobPost.builder()
                .id(100L)
                .title("Java Software Engineer")
                .companyName("TechCorp")
                .recruiter(recruiter)
                .status("PUBLISHED")
                .build();

        // Students
        User u1 = User.builder().id(101L).username("kashish").email("kashish@test.com").firstName("Kashish").lastName("Tank").build();
        student1 = StudentProfile.builder().id(1001L).user(u1).college("LDRP").degree("B.Tech").skillsText("Java, Spring").build();

        User u2 = User.builder().id(102L).username("khush").email("khush@test.com").firstName("Khush").lastName("Patel").build();
        student2 = StudentProfile.builder().id(1002L).user(u2).college("LDRP").degree("B.Tech").skillsText("Java").build();

        User u3 = User.builder().id(103L).username("jainil").email("jainil@test.com").firstName("Jainil").lastName("Gajjar").build();
        student3 = StudentProfile.builder().id(1003L).user(u3).college("LDRP").degree("B.Tech").skillsText("Python").build();

        resume1 = Resume.builder().id(2001L).studentProfile(student1).fileName("kashish.pdf").currentVersion(1).build();
        resume2 = Resume.builder().id(2002L).studentProfile(student2).fileName("khush.pdf").currentVersion(1).build();
        resume3 = Resume.builder().id(2003L).studentProfile(student3).fileName("jainil.pdf").currentVersion(1).build();

        app1 = JobApplication.builder().id(3001L).jobPost(jobPost).studentProfile(student1).resume(resume1).status("APPLIED").appliedAt(LocalDateTime.now().minusHours(3)).build();
        app2 = JobApplication.builder().id(3002L).jobPost(jobPost).studentProfile(student2).resume(resume2).status("APPLIED").appliedAt(LocalDateTime.now().minusHours(2)).build();
        app3 = JobApplication.builder().id(3003L).jobPost(jobPost).studentProfile(student3).resume(resume3).status("APPLIED").appliedAt(LocalDateTime.now().minusHours(1)).build();

        // Existing ATS Analysis data from Module 6
        analysis1 = ResumeAnalysis.builder()
                .id(4001L)
                .resume(resume1)
                .jobPost(jobPost)
                .studentProfile(student1)
                .atsScore(89.0)
                .skillMatchPercentage(100.0)
                .qualificationMatch("MATCH")
                .experienceMatch("MATCH")
                .matchedSkills("Java, Spring")
                .resumeVersionNumber(1)
                .analyzedAt(LocalDateTime.now())
                .build();

        analysis2 = ResumeAnalysis.builder()
                .id(4002L)
                .resume(resume2)
                .jobPost(jobPost)
                .studentProfile(student2)
                .atsScore(72.5)
                .skillMatchPercentage(80.0)
                .qualificationMatch("PARTIAL_MATCH")
                .experienceMatch("PARTIAL_MATCH")
                .matchedSkills("Java")
                .resumeVersionNumber(1)
                .analyzedAt(LocalDateTime.now())
                .build();

        // Candidate 3 has NO ATS analysis record!
    }

    // Test 1: Recruiter can retrieve applicants for own job
    @Test
    void testRecruiterCanRetrieveApplicantsForOwnJob() {
        when(userResolutionService.resolveUser("recruiter1")).thenReturn(recruiterUser);
        when(recruiterRepository.findByUserId(1L)).thenReturn(Optional.of(recruiter));
        when(jobPostRepository.findById(100L)).thenReturn(Optional.of(jobPost));
        when(jobApplicationRepository.findByJobPostId(100L)).thenReturn(List.of(app1, app2, app3));
        when(shortlistedCandidateRepository.findByJobPostId(100L)).thenReturn(Collections.emptyList());

        List<CandidateRecommendationDto> candidates = candidateRecommendationService.getRankedCandidatesForJob(100L, "recruiter1");

        assertNotNull(candidates);
        assertEquals(3, candidates.size());
    }

    // Test 2: Recruiter cannot retrieve another recruiter's applicants
    @Test
    void testRecruiterCannotRetrieveAnotherRecruiterApplicants() {
        when(userResolutionService.resolveUser("recruiter2")).thenReturn(otherRecruiterUser);
        when(recruiterRepository.findByUserId(2L)).thenReturn(Optional.of(otherRecruiter));
        when(jobPostRepository.findById(100L)).thenReturn(Optional.of(jobPost)); // jobPost belongs to recruiter1

        assertThrows(AccessDeniedException.class, () -> {
            candidateRecommendationService.getRankedCandidatesForJob(100L, "recruiter2");
        });
    }

    // Test 3: Existing ATS score is displayed correctly
    @Test
    void testExistingAtsScoreIsDisplayedCorrectly() {
        when(userResolutionService.resolveUser("recruiter1")).thenReturn(recruiterUser);
        when(recruiterRepository.findByUserId(1L)).thenReturn(Optional.of(recruiter));
        when(jobPostRepository.findById(100L)).thenReturn(Optional.of(jobPost));
        when(jobApplicationRepository.findByJobPostId(100L)).thenReturn(List.of(app1));
        when(resumeAnalysisRepository.findByResumeIdAndJobPostIdAndResumeVersionNumber(2001L, 100L, 1)).thenReturn(Optional.of(analysis1));

        List<CandidateRecommendationDto> result = candidateRecommendationService.getRankedCandidatesForJob(100L, "recruiter1");

        assertEquals(1, result.size());
        assertTrue(result.get(0).isHasAtsAnalysis());
        assertEquals(89.0, result.get(0).getAtsScore());
        assertEquals("89.0%", result.get(0).getFormattedAtsScore());
    }

    // Test 4: Candidates are ordered by existing ATS score (highest first)
    @Test
    void testCandidatesAreOrderedByExistingAtsScore() {
        when(userResolutionService.resolveUser("recruiter1")).thenReturn(recruiterUser);
        when(recruiterRepository.findByUserId(1L)).thenReturn(Optional.of(recruiter));
        when(jobPostRepository.findById(100L)).thenReturn(Optional.of(jobPost));
        when(jobApplicationRepository.findByJobPostId(100L)).thenReturn(List.of(app3, app2, app1)); // Unordered input

        when(resumeAnalysisRepository.findByResumeIdAndJobPostIdAndResumeVersionNumber(2001L, 100L, 1)).thenReturn(Optional.of(analysis1)); // 89.0%
        when(resumeAnalysisRepository.findByResumeIdAndJobPostIdAndResumeVersionNumber(2002L, 100L, 1)).thenReturn(Optional.of(analysis2)); // 72.5%
        when(resumeAnalysisRepository.findByResumeIdAndJobPostIdAndResumeVersionNumber(2003L, 100L, 1)).thenReturn(Optional.empty()); // No analysis

        List<CandidateRecommendationDto> ranked = candidateRecommendationService.getRankedCandidatesForJob(100L, "recruiter1");

        assertEquals(3, ranked.size());
        assertEquals("Kashish Tank", ranked.get(0).getStudentName());
        assertEquals(89.0, ranked.get(0).getAtsScore());

        assertEquals("Khush Patel", ranked.get(1).getStudentName());
        assertEquals(72.5, ranked.get(1).getAtsScore());

        assertEquals("Jainil Gajjar", ranked.get(2).getStudentName());
        assertFalse(ranked.get(2).isHasAtsAnalysis());
        assertEquals("ATS Analysis Not Available", ranked.get(2).getFormattedAtsScore());
    }

    // Test 5: Candidate without ATS analysis shows "ATS Analysis Not Available"
    @Test
    void testCandidateWithoutAtsAnalysisShowsNotAvailable() {
        when(userResolutionService.resolveUser("recruiter1")).thenReturn(recruiterUser);
        when(recruiterRepository.findByUserId(1L)).thenReturn(Optional.of(recruiter));
        when(jobPostRepository.findById(100L)).thenReturn(Optional.of(jobPost));
        when(jobApplicationRepository.findByJobPostId(100L)).thenReturn(List.of(app3));
        when(resumeAnalysisRepository.findByResumeIdAndJobPostIdAndResumeVersionNumber(2003L, 100L, 1)).thenReturn(Optional.empty());

        List<CandidateRecommendationDto> result = candidateRecommendationService.getRankedCandidatesForJob(100L, "recruiter1");

        assertEquals(1, result.size());
        assertFalse(result.get(0).isHasAtsAnalysis());
        assertEquals("ATS Analysis Not Available", result.get(0).getFormattedAtsScore());
        assertEquals("ATS Analysis Not Available", result.get(0).getRecommendationLabel());
    }

    // Test 6: Recommendation label uses configured threshold
    @Test
    void testRecommendationLabelUsesConfiguredThreshold() {
        assertEquals("Highly Compatible", recommendationConfig.getRecommendationLabel(89.0));
        assertEquals("Highly Compatible", recommendationConfig.getRecommendationLabel(80.0));
        assertEquals("Compatible", recommendationConfig.getRecommendationLabel(72.5));
        assertEquals("Compatible", recommendationConfig.getRecommendationLabel(60.0));
        assertEquals("Needs Review", recommendationConfig.getRecommendationLabel(49.2));
        assertEquals("ATS Analysis Not Available", recommendationConfig.getRecommendationLabel(null));
    }

    // Test 7: Candidate can be shortlisted
    @Test
    void testCandidateCanBeShortlisted() {
        when(userResolutionService.resolveUser("recruiter1")).thenReturn(recruiterUser);
        when(recruiterRepository.findByUserId(1L)).thenReturn(Optional.of(recruiter));
        when(jobPostRepository.findById(100L)).thenReturn(Optional.of(jobPost));
        when(studentProfileRepository.findById(1001L)).thenReturn(Optional.of(student1));
        when(jobApplicationRepository.findByJobPostId(100L)).thenReturn(List.of(app1));

        shortlistService.shortlistCandidate(100L, 1001L, "recruiter1");

        verify(shortlistedCandidateRepository, times(1)).save(any(ShortlistedCandidate.class));
        assertEquals("SHORTLISTED", app1.getStatus());
    }

    // Test 8: Candidate can be removed from shortlist
    @Test
    void testCandidateCanBeRemovedFromShortlist() {
        when(userResolutionService.resolveUser("recruiter1")).thenReturn(recruiterUser);
        when(recruiterRepository.findByUserId(1L)).thenReturn(Optional.of(recruiter));
        when(jobPostRepository.findById(100L)).thenReturn(Optional.of(jobPost));
        when(jobApplicationRepository.findByJobPostId(100L)).thenReturn(List.of(app1));

        app1.setStatus("SHORTLISTED");

        shortlistService.removeCandidateFromShortlist(100L, 1001L, "recruiter1");

        verify(shortlistedCandidateRepository, times(1)).deleteByJobPostIdAndStudentProfileId(100L, 1001L);
        assertEquals("APPLIED", app1.getStatus());
    }

    // Test 9: Removing shortlist does NOT delete JobApplication
    @Test
    void testRemovingShortlistDoesNotDeleteJobApplication() {
        when(userResolutionService.resolveUser("recruiter1")).thenReturn(recruiterUser);
        when(recruiterRepository.findByUserId(1L)).thenReturn(Optional.of(recruiter));
        when(jobPostRepository.findById(100L)).thenReturn(Optional.of(jobPost));
        when(jobApplicationRepository.findByJobPostId(100L)).thenReturn(List.of(app1));

        shortlistService.removeCandidateFromShortlist(100L, 1001L, "recruiter1");

        verify(jobApplicationRepository, never()).delete(any(JobApplication.class));
        verify(jobApplicationRepository, never()).deleteById(anyLong());
    }
}
