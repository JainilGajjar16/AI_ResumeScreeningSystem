package com.resume.screening.controller;

import com.resume.screening.dto.ShortlistedCandidateDto;
import com.resume.screening.entity.User;
import com.resume.screening.repository.UserRepository;
import com.resume.screening.service.ShortlistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class Module8ShortlistedPageTest {

    @Autowired
    private ShortlistService shortlistService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SpringTemplateEngine templateEngine;

    @Test
    void testShortlistedPageRendersSuccessfully() {
        User recruiterUser = userRepository.findAll().stream()
                .filter(u -> "RECRUITER".equalsIgnoreCase(u.getRole()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No recruiter user found"));

        List<ShortlistedCandidateDto> list = shortlistService.getShortlistedCandidatesForRecruiter(recruiterUser.getUsername());
        assertNotNull(list);

        boolean hasJobAppId = list.stream().anyMatch(dto -> dto.getJobApplicationId() != null);
        assertTrue(hasJobAppId, "At least one shortlisted candidate should have jobApplicationId populated");

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockServletContext servletContext = new MockServletContext();

        JakartaServletWebApplication webApplication = JakartaServletWebApplication.buildApplication(servletContext);
        WebContext ctx = new WebContext(webApplication.buildExchange(request, response));
        ctx.setVariable("shortlistedCandidates", list);

        String renderedHtml = templateEngine.process("recruiter/shortlisted", ctx);
        assertNotNull(renderedHtml);
        assertTrue(renderedHtml.contains("Kashish Tank"), "Shortlisted page HTML should contain candidate Kashish Tank");
        assertTrue(renderedHtml.contains("/recruiter/interviews/schedule"), "Shortlisted page HTML should contain Schedule Interview link");
        assertTrue(renderedHtml.contains("applicationId="), "Shortlisted page HTML should contain applicationId query parameter in Schedule Interview link");
    }
}
