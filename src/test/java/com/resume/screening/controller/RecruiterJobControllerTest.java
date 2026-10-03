package com.resume.screening.controller;

import com.resume.screening.config.CandidateRecommendationConfig;
import com.resume.screening.dto.CandidateRecommendationDto;
import com.resume.screening.dto.JobApplicationDto;
import com.resume.screening.dto.JobPostDto;
import com.resume.screening.entity.User;
import com.resume.screening.repository.ParsedResumeRepository;
import com.resume.screening.service.CandidateRecommendationService;
import com.resume.screening.service.JobApplicationService;
import com.resume.screening.service.JobDescriptionParsingService;
import com.resume.screening.service.JobService;
import com.resume.screening.service.ResumeAnalysisService;
import com.resume.screening.service.ResumeStorageService;
import com.resume.screening.service.ShortlistService;
import com.resume.screening.service.UserResolutionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RecruiterJobController.class)
@AutoConfigureMockMvc(addFilters = false)
public class RecruiterJobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private JobService jobService;
    @MockBean private JobApplicationService jobApplicationService;
    @MockBean private ResumeStorageService resumeStorageService;
    @MockBean private JobDescriptionParsingService jobDescriptionParsingService;
    @MockBean private ResumeAnalysisService resumeAnalysisService;
    @MockBean private UserResolutionService userResolutionService;
    @MockBean private CandidateRecommendationService candidateRecommendationService;
    @MockBean private ShortlistService shortlistService;
    @MockBean private ParsedResumeRepository parsedResumeRepository;
    @MockBean private CandidateRecommendationConfig recommendationConfig;

    @Test
    @WithMockUser(username = "recruiter1", roles = {"RECRUITER"})
    void testViewJobApplicantsWithNullStudentProfileIdInRecommendation() throws Exception {
        User user = User.builder().id(1L).username("recruiter1").role("RECRUITER").build();
        when(userResolutionService.resolveAuthenticatedUser(any())).thenReturn(user);

        JobPostDto jobDto = JobPostDto.builder().id(3L).title("Java Software Engineer").companyName("TechCorp").build();
        when(jobService.getJobForRecruiter(eq(3L), any())).thenReturn(jobDto);

        JobApplicationDto appDto = JobApplicationDto.builder().id(3001L).jobId(3L).studentProfileId(null).studentName("Unknown").build();
        when(jobApplicationService.getApplicantsForJob(eq(3L), any())).thenReturn(List.of(appDto));

        CandidateRecommendationDto recDto = CandidateRecommendationDto.builder()
                .applicationId(3001L)
                .jobId(3L)
                .studentProfileId(null) // NULL studentProfileId!
                .studentName("Candidate With Null Profile")
                .studentEmail("nullprofile@test.com")
                .hasAtsAnalysis(false)
                .formattedAtsScore("ATS Analysis Not Available")
                .recommendationLabel("ATS Analysis Not Available")
                .recommendationBadgeClass("bg-secondary")
                .isShortlisted(false)
                .build();

        when(candidateRecommendationService.getRankedCandidatesForJob(eq(3L), any())).thenReturn(List.of(recDto));

        mockMvc.perform(get("/recruiter/jobs/3/applicants"))
                .andExpect(status().isOk());
    }
}
