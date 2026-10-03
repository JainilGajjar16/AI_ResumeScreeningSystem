package com.resume.screening.controller;

import com.resume.screening.entity.Interview;
import com.resume.screening.repository.InterviewRepository;
import com.resume.screening.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class Module8DetailsPageIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InterviewRepository interviewRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @Transactional
    void testRecruiterInterviewDetailsRendersWithAmPmAndActions() throws Exception {
        Interview interview = interviewRepository.findAll().stream().findFirst().orElse(null);
        assertNotNull(interview, "An interview must exist in DB for testing");

        String recruiterUsername = interview.getRecruiter().getUser().getUsername();
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                recruiterUsername,
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_RECRUITER"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        MvcResult result = mockMvc.perform(get("/recruiter/interviews/" + interview.getId()).principal(auth))
                .andExpect(status().isOk())
                .andExpect(view().name("recruiter/interview-details"))
                .andExpect(model().attributeExists("interview"))
                .andExpect(model().attributeExists("interviewRequest"))
                .andExpect(model().attributeExists("feedbackRequest"))
                .andExpect(model().attributeExists("fullName"))
                .andReturn();

        String html = result.getResponse().getContentAsString();
        assertNotNull(html);
        assertTrue(html.contains("Interview Details"), "Recruiter page should contain Interview Details heading");
        assertTrue(html.contains("Interview Feedback & Result"), "Recruiter page should contain Interview Feedback & Result section");
        assertTrue(html.contains("Back to Interviews"), "Recruiter page should contain Back to Interviews button");
        assertTrue(html.contains("Edit / Reschedule"), "Recruiter page should contain Edit / Reschedule button");
        assertTrue(html.contains("Cancel Interview"), "Recruiter page should contain Cancel Interview button");
    }

    @Test
    @Transactional
    void testStudentInterviewDetailsRendersReadOnlyWithAmPm() throws Exception {
        Interview interview = interviewRepository.findAll().stream().findFirst().orElse(null);
        assertNotNull(interview, "An interview must exist in DB for testing");

        String studentUsername = interview.getStudent().getUser().getUsername();
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                studentUsername,
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_STUDENT"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        MvcResult result = mockMvc.perform(get("/student/interviews/" + interview.getId()).principal(auth))
                .andExpect(status().isOk())
                .andExpect(view().name("student/interview-details"))
                .andExpect(model().attributeExists("interview"))
                .andExpect(model().attributeExists("fullName"))
                .andReturn();

        String html = result.getResponse().getContentAsString();
        assertNotNull(html);
        assertTrue(html.contains("Interview Details"), "Student page should contain Interview Details heading");
        assertTrue(html.contains("Back to My Interviews"), "Student page should contain Back to My Interviews button");
        assertFalse(html.contains("Cancel Interview"), "Student page MUST NOT contain Cancel Interview button");
        assertFalse(html.contains("Edit / Reschedule"), "Student page MUST NOT contain Edit / Reschedule button");
    }
}
