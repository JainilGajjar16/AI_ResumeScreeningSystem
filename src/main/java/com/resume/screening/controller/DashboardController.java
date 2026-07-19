package com.resume.screening.controller;

import com.resume.screening.security.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @GetMapping("/student/dashboard")
    public String studentDashboard(Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        model.addAttribute("fullName", userDetails.getFullName());
        model.addAttribute("username", userDetails.getUsername());
        model.addAttribute("email", userDetails.getEmail());
        return "student/dashboard";
    }

    @GetMapping("/recruiter/dashboard")
    public String recruiterDashboard(Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        model.addAttribute("fullName", userDetails.getFullName());
        model.addAttribute("username", userDetails.getUsername());
        model.addAttribute("email", userDetails.getEmail());
        return "recruiter/dashboard";
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        model.addAttribute("fullName", userDetails.getFullName());
        model.addAttribute("username", userDetails.getUsername());
        model.addAttribute("email", userDetails.getEmail());
        return "admin/dashboard";
    }
}
