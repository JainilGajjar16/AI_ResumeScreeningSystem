package com.resume.screening.controller;

import com.resume.screening.dto.InterviewDto;
import com.resume.screening.entity.User;
import com.resume.screening.service.InterviewService;
import com.resume.screening.service.UserResolutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/student/interviews")
public class StudentInterviewController {

    private static final Logger log = LoggerFactory.getLogger(StudentInterviewController.class);

    private final InterviewService interviewService;
    private final UserResolutionService userResolutionService;

    public StudentInterviewController(InterviewService interviewService, UserResolutionService userResolutionService) {
        this.interviewService = interviewService;
        this.userResolutionService = userResolutionService;
    }

    private User resolveAuthenticatedUser(Authentication authentication) {
        return userResolutionService.resolveAuthenticatedUser(authentication);
    }

    // 1. View Student's Own Interviews
    @GetMapping
    public String myInterviews(Model model, Authentication authentication) {
        User user = resolveAuthenticatedUser(authentication);
        List<InterviewDto> interviews = interviewService.getStudentInterviews(user.getUsername());
        model.addAttribute("interviews", interviews);
        String fullName = (user.getFirstName() != null ? user.getFirstName() : "") + " " + (user.getLastName() != null ? user.getLastName() : "");
        fullName = fullName.trim();
        if (fullName.isEmpty()) fullName = user.getUsername();
        model.addAttribute("fullName", fullName);
        return "student/interviews";
    }

    // 2. View Specific Interview Details
    @GetMapping("/{id}")
    public String viewInterviewDetails(@PathVariable("id") Long id,
                                       Model model,
                                       Authentication authentication,
                                       RedirectAttributes redirectAttributes) {
        try {
            User user = resolveAuthenticatedUser(authentication);
            InterviewDto interview = interviewService.getInterviewById(id, user.getUsername());
            model.addAttribute("interview", interview);
            String fullName = (user.getFirstName() != null ? user.getFirstName() : "") + " " + (user.getLastName() != null ? user.getLastName() : "");
            fullName = fullName.trim();
            if (fullName.isEmpty()) fullName = user.getUsername();
            model.addAttribute("fullName", fullName);
            return "student/interview-details";
        } catch (Exception ex) {
            log.error("Error retrieving interview details for student: ", ex);
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/student/interviews";
        }
    }
}
