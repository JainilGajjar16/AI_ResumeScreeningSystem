package com.resume.screening.controller;

import com.resume.screening.dto.CandidateRecommendationDto;
import com.resume.screening.dto.JobPostDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
public class ThymeleafApplicantsRenderingTest {

    @Autowired
    private SpringTemplateEngine templateEngine;

    @Test
    void testApplicantsTemplateWithNullFields() {
        JobPostDto jobDto = JobPostDto.builder()
                .id(3L)
                .title("Java Software Engineer")
                .companyName("TechCorp")
                .build();

        // Candidate with ALL NULL optional fields (null studentProfileId, null resumeId, null matchedSkills, null scores)
        CandidateRecommendationDto nullFieldsCandidate = CandidateRecommendationDto.builder()
                .applicationId(100L)
                .jobId(3L)
                .studentProfileId(null) // NULL studentProfileId
                .studentName("Unknown")
                .studentEmail("nullprofile@test.com")
                .hasAtsAnalysis(false)
                .atsScore(null)
                .formattedAtsScore("ATS Analysis Not Available")
                .skillMatchPercentage(null)
                .formattedSkillMatch("N/A")
                .qualificationMatch(null)
                .experienceMatch(null)
                .matchedSkills(null) // NULL matchedSkills
                .missingSkills(null)
                .recommendationLabel("ATS Analysis Not Available")
                .recommendationBadgeClass("bg-secondary")
                .isShortlisted(false)
                .shortlistedCandidateId(null)
                .build();

        CandidateRecommendationDto partialCandidate = CandidateRecommendationDto.builder()
                .applicationId(101L)
                .jobId(3L)
                .studentProfileId(5L)
                .studentName("Khush Patel")
                .studentEmail("khush@test.com")
                .hasAtsAnalysis(true)
                .atsScore(70.4)
                .formattedAtsScore("70.4%")
                .skillMatchPercentage(80.0)
                .formattedSkillMatch("80.0%")
                .qualificationMatch("MATCH")
                .experienceMatch(null) // NULL match
                .matchedSkills(null) // NULL matchedSkills list
                .missingSkills(Collections.emptyList())
                .recommendationLabel("Compatible")
                .recommendationBadgeClass("bg-info")
                .isShortlisted(true)
                .shortlistedCandidateId(10L)
                .build();

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockServletContext servletContext = new MockServletContext();

        JakartaServletWebApplication webApplication = JakartaServletWebApplication.buildApplication(servletContext);
        WebContext ctx = new WebContext(webApplication.buildExchange(request, response));

        ctx.setVariable("job", jobDto);
        ctx.setVariable("applicants", Collections.emptyList());
        ctx.setVariable("recommendations", List.of(nullFieldsCandidate, partialCandidate));

        assertDoesNotThrow(() -> {
            String renderedHtml = templateEngine.process("recruiter/applicants", ctx);
            assertNotNull(renderedHtml);
            System.out.println("Rendered HTML length: " + renderedHtml.length());
        });
    }
}
