package com.resume.screening.controller;

import com.resume.screening.dto.StudentProfileDto;
import com.resume.screening.security.CustomUserDetails;
import com.resume.screening.service.StudentProfileService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.resume.screening.dto.JobPostDto;
import com.resume.screening.dto.ParsedResumeDto;
import com.resume.screening.dto.ResumeAnalysisDto;
import com.resume.screening.dto.ResumeDto;
import com.resume.screening.service.JobService;
import com.resume.screening.service.ResumeAnalysisService;
import com.resume.screening.service.ResumeParsingService;
import com.resume.screening.service.ResumeStorageService;
import java.util.List;

@Controller
@RequestMapping("/student")
public class StudentProfileController {

    private final StudentProfileService studentProfileService;
    private final ResumeStorageService resumeStorageService;
    private final ResumeParsingService resumeParsingService;
    private final ResumeAnalysisService resumeAnalysisService;
    private final JobService jobService;
    private final com.resume.screening.service.UserResolutionService userResolutionService;

    public StudentProfileController(StudentProfileService studentProfileService,
                                  ResumeStorageService resumeStorageService,
                                  ResumeParsingService resumeParsingService,
                                  ResumeAnalysisService resumeAnalysisService,
                                  JobService jobService,
                                  com.resume.screening.service.UserResolutionService userResolutionService) {
        this.studentProfileService = studentProfileService;
        this.resumeStorageService = resumeStorageService;
        this.resumeParsingService = resumeParsingService;
        this.resumeAnalysisService = resumeAnalysisService;
        this.jobService = jobService;
        this.userResolutionService = userResolutionService;
    }

    private com.resume.screening.entity.User resolveAuthenticatedUser(org.springframework.security.core.Authentication authentication) {
        return userResolutionService.resolveAuthenticatedUser(authentication);
    }

    @GetMapping("/profile")
    public String viewProfile(Model model, org.springframework.security.core.Authentication authentication) {
        com.resume.screening.entity.User user = resolveAuthenticatedUser(authentication);
        StudentProfileDto profile = studentProfileService.getProfileByUsernameOrEmail(user.getUsername());
        model.addAttribute("profile", profile);
        model.addAttribute("completionPercentage", profile.getCompletionPercentage());
        return "student/profile";
    }

    @GetMapping("/profile/edit")
    public String editProfileForm(Model model, org.springframework.security.core.Authentication authentication) {
        com.resume.screening.entity.User user = resolveAuthenticatedUser(authentication);
        StudentProfileDto profile = studentProfileService.getProfileByUsernameOrEmail(user.getUsername());
        model.addAttribute("profile", profile);
        return "student/profile-edit";
    }

    @PostMapping("/profile")
    public String saveProfile(@Valid @ModelAttribute("profile") StudentProfileDto profileDto,
                              BindingResult bindingResult,
                              org.springframework.security.core.Authentication authentication,
                              Model model,
                              RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return "student/profile-edit";
        }

        try {
            com.resume.screening.entity.User user = resolveAuthenticatedUser(authentication);
            studentProfileService.updateProfile(user.getUsername(), profileDto);
            redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully!");
            return "redirect:/student/profile";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "student/profile-edit";
        }
    }

    @GetMapping("/ats-analysis")
    public String atsAnalysis(@RequestParam(name = "jobId", required = false) Long jobId,
                              Model model,
                              org.springframework.security.core.Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        com.resume.screening.entity.User user = resolveAuthenticatedUser(authentication);
        String username = user.getUsername();

        // 1. Check if student has uploaded a resume
        ResumeDto currentResume = resumeStorageService.getCurrentResume(username);
        if (currentResume == null) {
            model.addAttribute("hasResume", false);
            model.addAttribute("errorMessage", "Please upload your resume before running ATS analysis.");
            return "student/analysis";
        }
        model.addAttribute("hasResume", true);
        model.addAttribute("currentResume", currentResume);

        // 2. Check if resume is parsed
        ParsedResumeDto parsedResume = resumeParsingService.getParsedResumeByResumeId(currentResume.getId());
        boolean isParsed = (parsedResume != null && "PARSED".equalsIgnoreCase(parsedResume.getParsingStatus()));
        if (!isParsed) {
            model.addAttribute("isParsed", false);
            model.addAttribute("errorMessage", "Please parse your resume before running ATS analysis.");
            return "student/analysis";
        }
        model.addAttribute("isParsed", true);

        // 3. If no job is selected, load published jobs for selection
        if (jobId == null) {
            List<JobPostDto> publishedJobs = jobService.getPublishedJobsForStudents();
            model.addAttribute("publishedJobs", publishedJobs);
            return "student/analysis";
        }

        // 4. Job is selected, validate existence & published status
        JobPostDto job;
        try {
            job = jobService.getJobById(jobId);
            if (!"PUBLISHED".equalsIgnoreCase(job.getStatus())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Selected job is not active or published.");
                return "redirect:/student/ats-analysis";
            }
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Job not found.");
            return "redirect:/student/ats-analysis";
        }
        model.addAttribute("job", job);

        // 5. Check if analysis has been generated for this resume + job
        try {
            ResumeAnalysisDto analysis = resumeAnalysisService.getAnalysisResult(currentResume.getId(), jobId, username);
            model.addAttribute("analysis", analysis);
        } catch (Exception ex) {
            model.addAttribute("analysis", null);
        }

        return "student/analysis";
    }
}
