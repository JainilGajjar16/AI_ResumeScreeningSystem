package com.resume.screening.controller;

import com.resume.screening.dto.ForgotPasswordDto;
import com.resume.screening.dto.StudentRegisterDto;
import com.resume.screening.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/login")
    public String viewLoginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("student", new StudentRegisterDto());
        return "register";
    }

    @PostMapping("/register")
    public String registerStudent(
            @Valid @ModelAttribute("student") StudentRegisterDto registerDto,
            BindingResult result,
            Model model) {

        if (!registerDto.getPassword().isEmpty() && !registerDto.getConfirmPassword().isEmpty()) {
            if (!registerDto.getPassword().equals(registerDto.getConfirmPassword())) {
                result.rejectValue("confirmPassword", "error.student", "Passwords do not match");
            }
        }

        if (authService.existsByUsername(registerDto.getUsername())) {
            result.rejectValue("username", "error.student", "Username is already taken");
        }

        if (authService.existsByEmail(registerDto.getEmail())) {
            result.rejectValue("email", "error.student", "Email is already registered");
        }

        if (result.hasErrors()) {
            return "register";
        }

        try {
            authService.registerStudent(registerDto);
            return "redirect:/login?registered=true";
        } catch (Exception e) {
            model.addAttribute("globalError", "An unexpected error occurred during registration: " + e.getMessage());
            return "register";
        }
    }

    @GetMapping("/forgot-password")
    public String showForgotPasswordForm(Model model) {
        model.addAttribute("forgotPassword", new ForgotPasswordDto());
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String resetPassword(
            @Valid @ModelAttribute("forgotPassword") ForgotPasswordDto resetDto,
            BindingResult result,
            Model model) {

        if (!resetDto.getNewPassword().isEmpty() && !resetDto.getConfirmPassword().isEmpty()) {
            if (!resetDto.getNewPassword().equals(resetDto.getConfirmPassword())) {
                result.rejectValue("confirmPassword", "error.forgotPassword", "Passwords do not match");
            }
        }

        if (!resetDto.getEmail().isEmpty() && !authService.existsByEmail(resetDto.getEmail())) {
            result.rejectValue("email", "error.forgotPassword", "Email address not found in system");
        }

        if (result.hasErrors()) {
            return "forgot-password";
        }

        try {
            authService.resetPassword(resetDto);
            return "redirect:/login?resetSuccess=true";
        } catch (Exception e) {
            model.addAttribute("globalError", "An error occurred during password reset: " + e.getMessage());
            return "forgot-password";
        }
    }
}
