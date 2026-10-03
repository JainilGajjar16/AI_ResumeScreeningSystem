package com.resume.screening.controller;

import com.resume.screening.dto.ResumeDto;
import com.resume.screening.dto.StudentProfileDto;
import com.resume.screening.security.CustomUserDetails;
import com.resume.screening.service.ResumeStorageService;
import com.resume.screening.service.StudentProfileService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.resume.screening.dto.RecruiterDashboardDto;
import com.resume.screening.service.JobService;

import com.resume.screening.service.JobApplicationService;

import com.resume.screening.dto.ResumeAnalysisDto;
import com.resume.screening.service.ResumeAnalysisService;
import java.util.Locale;

@Controller
public class DashboardController {

    private final StudentProfileService studentProfileService;
    private final ResumeStorageService resumeStorageService;
    private final JobService jobService;
    private final JobApplicationService jobApplicationService;
    private final ResumeAnalysisService resumeAnalysisService;
    private final com.resume.screening.service.UserResolutionService userResolutionService;

    public DashboardController(StudentProfileService studentProfileService,
                               ResumeStorageService resumeStorageService,
                               JobService jobService,
                               JobApplicationService jobApplicationService,
                               ResumeAnalysisService resumeAnalysisService,
                               com.resume.screening.service.UserResolutionService userResolutionService) {
        this.studentProfileService = studentProfileService;
        this.resumeStorageService = resumeStorageService;
        this.jobService = jobService;
        this.jobApplicationService = jobApplicationService;
        this.resumeAnalysisService = resumeAnalysisService;
        this.userResolutionService = userResolutionService;
    }

    private com.resume.screening.entity.User resolveAuthenticatedUser(org.springframework.security.core.Authentication authentication) {
        return userResolutionService.resolveAuthenticatedUser(authentication);
    }

    @GetMapping("/student/dashboard")
    public String studentDashboard(Model model, org.springframework.security.core.Authentication authentication) {
        com.resume.screening.entity.User user = resolveAuthenticatedUser(authentication);
        StudentProfileDto profileDto = studentProfileService.getProfileByUsernameOrEmail(user.getUsername());
        ResumeDto resumeDto = resumeStorageService.getCurrentResume(user.getUsername());
        int appCount = jobApplicationService.getApplicationsForStudent(user.getUsername()).size();
        ResumeAnalysisDto latestAnalysis = resumeAnalysisService.getLatestAnalysisForStudent(user.getUsername());

        model.addAttribute("fullName", (profileDto.getFirstName() != null ? profileDto.getFirstName() : "") + " " + (profileDto.getLastName() != null ? profileDto.getLastName() : ""));
        model.addAttribute("username", user.getUsername());
        model.addAttribute("email", profileDto.getEmail() != null ? profileDto.getEmail() : user.getEmail());
        model.addAttribute("profile", profileDto);
        model.addAttribute("completionPercentage", profileDto.getCompletionPercentage());
        model.addAttribute("resumeStatus", resumeDto != null ? resumeDto.getStatus() : "Not Uploaded");

        if (latestAnalysis != null && latestAnalysis.getAtsScore() != null) {
            model.addAttribute("atsScore", String.format(Locale.US, "%.1f%%", latestAnalysis.getAtsScore()));
            model.addAttribute("latestAnalysis", latestAnalysis);
            model.addAttribute("hasAtsAnalysis", true);
        } else {
            model.addAttribute("atsScore", "Not Analyzed Yet");
            model.addAttribute("hasAtsAnalysis", false);
        }

        model.addAttribute("applicationCount", appCount);
        return "student/dashboard";
    }

    @GetMapping("/recruiter/dashboard")
    public String recruiterDashboard(Model model, org.springframework.security.core.Authentication authentication) {
        com.resume.screening.entity.User user = resolveAuthenticatedUser(authentication);
        RecruiterDashboardDto dashboardStats = jobService.getRecruiterDashboardStats(user.getUsername());
        model.addAttribute("fullName", dashboardStats.getRecruiterName());
        model.addAttribute("username", user.getUsername());
        model.addAttribute("email", user.getEmail());
        model.addAttribute("stats", dashboardStats);
        return "recruiter/dashboard";
    }

}
