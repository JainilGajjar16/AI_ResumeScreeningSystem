package com.resume.screening.controller;

import com.resume.screening.entity.JobPost;
import com.resume.screening.entity.User;
import com.resume.screening.repository.JobPostRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class VerifyUrlTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JobPostRepository jobPostRepository;

    @Test
    @org.springframework.transaction.annotation.Transactional
    void testRecruiterJobs3ApplicantsUrlLoadsSuccessfully() throws Exception {
        JobPost job = jobPostRepository.findById(3L).orElse(null);
        assertNotNull(job, "Job 3 must exist in database");

        User recruiterUser = (job.getRecruiter() != null && job.getRecruiter().getUser() != null)
                ? job.getRecruiter().getUser()
                : null;
        assertNotNull(recruiterUser, "Recruiter user for job 3 must exist");

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                recruiterUser.getUsername(),
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_RECRUITER"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        MvcResult result = mockMvc.perform(get("/recruiter/jobs/3/applicants").principal(auth))
                .andExpect(status().isOk())
                .andExpect(view().name("recruiter/applicants"))
                .andExpect(model().attributeExists("job"))
                .andExpect(model().attributeExists("applicants"))
                .andExpect(model().attributeExists("recommendations"))
                .andReturn();

        String htmlContent = result.getResponse().getContentAsString();
        assertNotNull(htmlContent);
        assertTrue(htmlContent.contains("Applicants"), "HTML response should contain 'Applicants'");
        System.out.println("VERIFY URL SUCCESS! Rendered HTML length: " + htmlContent.length());
    }
}
