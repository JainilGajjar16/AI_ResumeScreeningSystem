package com.resume.screening.controller;

import com.resume.screening.dto.JobApplicationDto;
import com.resume.screening.dto.JobPostDto;
import com.resume.screening.dto.ResumeDto;
import com.resume.screening.security.CustomUserDetails;
import com.resume.screening.service.JobApplicationService;
import com.resume.screening.service.JobService;
import com.resume.screening.service.ResumeStorageService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
@RequestMapping("/student")
public class StudentJobController {

    private static final Logger log = LoggerFactory.getLogger(StudentJobController.class);

    private final JobService jobService;
    private final JobApplicationService jobApplicationService;
    private final ResumeStorageService resumeStorageService;
    private final com.resume.screening.service.UserResolutionService userResolutionService;

    public StudentJobController(JobService jobService,
                                JobApplicationService jobApplicationService,
                                ResumeStorageService resumeStorageService,
                                com.resume.screening.service.UserResolutionService userResolutionService) {
        this.jobService = jobService;
        this.jobApplicationService = jobApplicationService;
        this.resumeStorageService = resumeStorageService;
        this.userResolutionService = userResolutionService;
    }

    private com.resume.screening.entity.User resolveAuthenticatedUser(org.springframework.security.core.Authentication authentication) {
        return userResolutionService.resolveAuthenticatedUser(authentication);
    }

    // 1. Available Published Jobs Board
    @GetMapping("/jobs")
    public String viewJobsBoard(Model model, org.springframework.security.core.Authentication authentication) {
        com.resume.screening.entity.User user = resolveAuthenticatedUser(authentication);
        String username = user != null ? user.getUsername() : "anonymous";
        log.info("Student '{}' requested available jobs board.", username);
        try {
            List<JobPostDto> jobs = jobService.getPublishedJobsForStudents();
            log.info("Loaded {} published jobs for student '{}'.", jobs.size(), username);
            model.addAttribute("jobs", jobs);
            return "student/jobs";
        } catch (Exception ex) {
            log.error("Exception occurred while loading published jobs for student '{}': ", username, ex);
            throw ex;
        }
    }

    // 2. View Job Details
    @GetMapping("/jobs/{id}")
    public String viewJobDetails(@PathVariable("id") Long id,
                                Model model,
                                org.springframework.security.core.Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        try {
            com.resume.screening.entity.User user = resolveAuthenticatedUser(authentication);
            JobPostDto job = jobService.getJobById(id);
            if (!"PUBLISHED".equalsIgnoreCase(job.getStatus())) {
                redirectAttributes.addFlashAttribute("errorMessage", "This job is not currently active.");
                return "redirect:/student/jobs";
            }

            boolean alreadyApplied = jobApplicationService.hasStudentApplied(id, user.getUsername());
            ResumeDto currentResume = resumeStorageService.getCurrentResume(user.getUsername());

            model.addAttribute("job", job);
            model.addAttribute("alreadyApplied", alreadyApplied);
            model.addAttribute("hasResume", currentResume != null);
            model.addAttribute("currentResume", currentResume);
            return "student/job-details";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/student/jobs";
        }
    }

    // 3. Submit Job Application
    @PostMapping("/jobs/{id}/apply")
    public String applyForJob(@PathVariable("id") Long id,
                              org.springframework.security.core.Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        try {
            com.resume.screening.entity.User user = resolveAuthenticatedUser(authentication);
            jobApplicationService.applyForJob(id, user.getUsername());
            redirectAttributes.addFlashAttribute("successMessage", "Application submitted successfully!");
            return "redirect:/student/applications";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/student/jobs/" + id;
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Could not submit application: " + ex.getMessage());
            return "redirect:/student/jobs/" + id;
        }
    }

    // 4. View My Submitted Applications
    @GetMapping("/applications")
    public String viewMyApplications(Model model, org.springframework.security.core.Authentication authentication) {
        com.resume.screening.entity.User user = resolveAuthenticatedUser(authentication);
        List<JobApplicationDto> applications = jobApplicationService.getApplicationsForStudent(user.getUsername());
        model.addAttribute("applications", applications);
        return "student/applications";
    }
}
