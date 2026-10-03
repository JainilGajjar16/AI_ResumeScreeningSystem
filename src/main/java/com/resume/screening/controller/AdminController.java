package com.resume.screening.controller;

import com.resume.screening.dto.*;
import com.resume.screening.entity.User;
import com.resume.screening.repository.UserRepository;
import com.resume.screening.service.AdminService;
import com.resume.screening.service.UserResolutionService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;
    private final UserResolutionService userResolutionService;
    private final UserRepository userRepository;

    public AdminController(AdminService adminService,
                           UserResolutionService userResolutionService,
                           UserRepository userRepository) {
        this.adminService = adminService;
        this.userResolutionService = userResolutionService;
        this.userRepository = userRepository;
    }

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null) {
            authentication = SecurityContextHolder.getContext().getAuthentication();
        }
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equalsIgnoreCase(authentication.getName())) {
            try {
                return userResolutionService.resolveAuthenticatedUser(authentication);
            } catch (Exception e) {
                if (authentication.getName() != null) {
                    return userRepository.findByUsernameOrEmail(authentication.getName()).orElse(null);
                }
            }
        }
        return null;
    }

    private void addAdminInfo(Model model, User adminUser) {
        if (adminUser != null) {
            String fullName = ((adminUser.getFirstName() != null ? adminUser.getFirstName() : "") + " " +
                               (adminUser.getLastName() != null ? adminUser.getLastName() : "")).trim();
            if (fullName.isEmpty()) {
                fullName = adminUser.getUsername() != null ? adminUser.getUsername() : "Admin";
            }
            model.addAttribute("fullName", fullName);
            model.addAttribute("username", adminUser.getUsername() != null ? adminUser.getUsername() : "admin");
            model.addAttribute("email", adminUser.getEmail() != null ? adminUser.getEmail() : "admin@enterprise.com");
        } else {
            model.addAttribute("fullName", "Admin");
            model.addAttribute("username", "admin");
            model.addAttribute("email", "admin@enterprise.com");
        }
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication) {
        User adminUser = getAuthenticatedUser(authentication);
        addAdminInfo(model, adminUser);

        AdminDashboardStatsDto stats = adminService.getDashboardStats();
        if (stats == null) {
            stats = new AdminDashboardStatsDto();
        }
        model.addAttribute("stats", stats);
        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String users(@RequestParam(required = false) String query,
                        @RequestParam(required = false) String role,
                        Model model, Authentication authentication) {
        User adminUser = getAuthenticatedUser(authentication);
        addAdminInfo(model, adminUser);

        List<AdminUserDto> users = adminService.getUsers(query, role);
        model.addAttribute("users", users != null ? users : Collections.emptyList());
        model.addAttribute("query", query != null ? query : "");
        model.addAttribute("roleFilter", role != null ? role : "");
        return "admin/users";
    }

    @GetMapping("/students")
    public String students(@RequestParam(required = false) String query,
                           Model model, Authentication authentication) {
        User adminUser = getAuthenticatedUser(authentication);
        addAdminInfo(model, adminUser);

        List<AdminStudentDto> students = adminService.getStudents(query);
        model.addAttribute("students", students != null ? students : Collections.emptyList());
        model.addAttribute("query", query != null ? query : "");
        return "admin/students";
    }

    @GetMapping("/recruiters")
    public String recruiters(@RequestParam(required = false) String query,
                             Model model, Authentication authentication) {
        User adminUser = getAuthenticatedUser(authentication);
        addAdminInfo(model, adminUser);

        List<AdminRecruiterDto> recruiters = adminService.getRecruiters(query);
        model.addAttribute("recruiters", recruiters != null ? recruiters : Collections.emptyList());
        model.addAttribute("query", query != null ? query : "");
        return "admin/recruiters";
    }

    @GetMapping("/jobs")
    public String jobs(@RequestParam(required = false) String query,
                       @RequestParam(required = false) String status,
                       Model model, Authentication authentication) {
        User adminUser = getAuthenticatedUser(authentication);
        addAdminInfo(model, adminUser);

        List<AdminJobDto> jobs = adminService.getJobs(query, status);
        model.addAttribute("jobs", jobs != null ? jobs : Collections.emptyList());
        model.addAttribute("query", query != null ? query : "");
        model.addAttribute("statusFilter", status != null ? status : "");
        return "admin/jobs";
    }

    @GetMapping("/applications")
    public String applications(@RequestParam(required = false) String query,
                               @RequestParam(required = false) String status,
                               Model model, Authentication authentication) {
        User adminUser = getAuthenticatedUser(authentication);
        addAdminInfo(model, adminUser);

        List<AdminApplicationDto> applications = adminService.getApplications(query, status);
        model.addAttribute("applications", applications != null ? applications : Collections.emptyList());
        model.addAttribute("query", query != null ? query : "");
        model.addAttribute("statusFilter", status != null ? status : "");
        return "admin/applications";
    }

    @GetMapping("/reports")
    public String reports(Model model, Authentication authentication) {
        User adminUser = getAuthenticatedUser(authentication);
        addAdminInfo(model, adminUser);

        AdminReportDto reports = adminService.getReports();
        model.addAttribute("reports", reports != null ? reports : new AdminReportDto());
        return "admin/reports";
    }
}
