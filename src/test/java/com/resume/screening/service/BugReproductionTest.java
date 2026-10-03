package com.resume.screening.service;

import com.resume.screening.config.CandidateRecommendationConfig;
import com.resume.screening.controller.RecruiterJobController;
import com.resume.screening.dto.CandidateRecommendationDto;
import com.resume.screening.entity.*;
import com.resume.screening.repository.*;
import com.resume.screening.service.impl.CandidateRecommendationServiceImpl;
import com.resume.screening.service.impl.JobApplicationServiceImpl;
import com.resume.screening.service.impl.ShortlistServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BugReproductionTest {

    @Mock private JobPostRepository jobPostRepository;
    @Mock private JobApplicationRepository jobApplicationRepository;
    @Mock private ResumeAnalysisRepository resumeAnalysisRepository;
    @Mock private ShortlistedCandidateRepository shortlistedCandidateRepository;
    @Mock private RecruiterRepository recruiterRepository;
    @Mock private UserRepository userRepository;
    @Mock private StudentProfileRepository studentProfileRepository;
    @Mock private UserResolutionService userResolutionService;
    @Mock private JobService jobService;
    @Mock private ResumeStorageService resumeStorageService;
    @Mock private JobDescriptionParsingService jobDescriptionParsingService;
    @Mock private ResumeAnalysisService resumeAnalysisService;
    @Mock private ParsedResumeRepository parsedResumeRepository;

    private CandidateRecommendationConfig recommendationConfig;
    private CandidateRecommendationServiceImpl candidateRecommendationService;
    private ShortlistServiceImpl shortlistService;
    private JobApplicationServiceImpl jobApplicationService;
    private RecruiterJobController recruiterJobController;

    private User recruiterUser;
    private Recruiter recruiter;
    private JobPost jobPost;

    @BeforeEach
    void setUp() {
        recommendationConfig = new CandidateRecommendationConfig();

        candidateRecommendationService = new CandidateRecommendationServiceImpl(
                jobPostRepository, jobApplicationRepository, resumeAnalysisRepository,
                shortlistedCandidateRepository, recruiterRepository, userRepository,
                userResolutionService, recommendationConfig
        );

        shortlistService = new ShortlistServiceImpl(
                shortlistedCandidateRepository, jobPostRepository, jobApplicationRepository,
                studentProfileRepository, recruiterRepository, resumeAnalysisRepository, userResolutionService
        );

        jobApplicationService = new JobApplicationServiceImpl(
                jobApplicationRepository, jobPostRepository, studentProfileRepository,
                resumeRepository(), recruiterRepository, userRepository, userResolutionService
        );

        recruiterJobController = new RecruiterJobController(
                jobService, jobApplicationService, resumeStorageService,
                jobDescriptionParsingService, resumeAnalysisService, userResolutionService,
                candidateRecommendationService, shortlistService, parsedResumeRepository
        );

        recruiterUser = User.builder().id(1L).username("recruiter1").email("recruiter1@test.com").role("RECRUITER").build();
        recruiter = Recruiter.builder().id(10L).user(recruiterUser).companyName("TechCorp").build();
        jobPost = JobPost.builder().id(3L).title("Java Software Engineer").recruiter(recruiter).companyName("TechCorp").status("PUBLISHED").build();
    }

    private ResumeRepository resumeRepository() {
        return mock(ResumeRepository.class);
    }

    @Test
    void testCandidateWithNullAtsScoreDoesNotCrashSort() {
        User u1 = User.builder().id(101L).username("cand1").email("c1@test.com").build();
        StudentProfile sp1 = StudentProfile.builder().id(1001L).user(u1).build();
        Resume r1 = Resume.builder().id(2001L).studentProfile(sp1).currentVersion(1).build();
        JobApplication app1 = JobApplication.builder().id(3001L).jobPost(jobPost).studentProfile(sp1).resume(r1).build();

        User u2 = User.builder().id(102L).username("cand2").email("c2@test.com").build();
        StudentProfile sp2 = StudentProfile.builder().id(1002L).user(u2).build();
        Resume r2 = Resume.builder().id(2002L).studentProfile(sp2).currentVersion(1).build();
        JobApplication app2 = JobApplication.builder().id(3002L).jobPost(jobPost).studentProfile(sp2).resume(r2).build();

        // analysis with null atsScore
        ResumeAnalysis nullScoreAnalysis = ResumeAnalysis.builder().id(4001L).resume(r1).jobPost(jobPost).studentProfile(sp1).atsScore(null).resumeVersionNumber(1).build();
        ResumeAnalysis validScoreAnalysis = ResumeAnalysis.builder().id(4002L).resume(r2).jobPost(jobPost).studentProfile(sp2).atsScore(85.0).resumeVersionNumber(1).build();

        when(userResolutionService.resolveUser("recruiter1")).thenReturn(recruiterUser);
        when(recruiterRepository.findByUserId(1L)).thenReturn(Optional.of(recruiter));
        when(jobPostRepository.findById(3L)).thenReturn(Optional.of(jobPost));
        when(jobApplicationRepository.findByJobPostId(3L)).thenReturn(List.of(app1, app2));
        when(resumeAnalysisRepository.findByResumeIdAndJobPostIdAndResumeVersionNumber(2001L, 3L, 1)).thenReturn(Optional.of(nullScoreAnalysis));
        when(resumeAnalysisRepository.findByResumeIdAndJobPostIdAndResumeVersionNumber(2002L, 3L, 1)).thenReturn(Optional.of(validScoreAnalysis));

        assertDoesNotThrow(() -> {
            List<CandidateRecommendationDto> results = candidateRecommendationService.getRankedCandidatesForJob(3L, "recruiter1");
            assertNotNull(results);
            assertEquals(2, results.size());
        });
    }

    @Test
    void testCandidateWithNullStudentProfileDoesNotCrash() {
        JobApplication appWithNullProfile = JobApplication.builder().id(3003L).jobPost(jobPost).studentProfile(null).resume(null).build();

        when(userResolutionService.resolveUser("recruiter1")).thenReturn(recruiterUser);
        when(recruiterRepository.findByUserId(1L)).thenReturn(Optional.of(recruiter));
        when(jobPostRepository.findById(3L)).thenReturn(Optional.of(jobPost));
        when(jobApplicationRepository.findByJobPostId(3L)).thenReturn(List.of(appWithNullProfile));

        assertDoesNotThrow(() -> {
            List<CandidateRecommendationDto> results = candidateRecommendationService.getRankedCandidatesForJob(3L, "recruiter1");
            assertNotNull(results);
            assertEquals(1, results.size());
            assertEquals("ATS Analysis Not Available", results.get(0).getFormattedAtsScore());
        });
    }
}
