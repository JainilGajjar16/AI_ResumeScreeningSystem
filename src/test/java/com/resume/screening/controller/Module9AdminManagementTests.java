package com.resume.screening.controller;

import com.resume.screening.dto.*;
import com.resume.screening.entity.*;
import com.resume.screening.enums.ApplicationDecision;
import com.resume.screening.repository.*;
import com.resume.screening.service.AdminService;
import com.resume.screening.service.UserResolutionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class Module9AdminManagementTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminService adminService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private RecruiterRepository recruiterRepository;

    @Autowired
    private JobPostRepository jobPostRepository;

    @Autowired
    private JobApplicationRepository jobApplicationRepository;

    @Autowired
    private InterviewRepository interviewRepository;

    @Autowired
    private ShortlistedCandidateRepository shortlistedCandidateRepository;

    @Autowired
    private AtsScoreRepository atsScoreRepository;

    @Autowired
    private ResumeAnalysisRepository resumeAnalysisRepository;

    private User adminUser;
    private User recruiterUser;
    private User studentUser;
    private Recruiter recruiter;
    private StudentProfile studentProfile;
    private JobPost jobPost;
    private JobApplication jobApplication;
    private Interview interview;

    @BeforeEach
    void setUp() {
        // Ensure Admin user exists
        adminUser = userRepository.findByUsername("admin").orElseGet(() -> {
            User u = User.builder()
                    .username("admin")
                    .password("admin123")
                    .email("admin@test.com")
                    .role("ADMIN")
                    .firstName("System")
                    .lastName("Admin")
                    .build();
            return userRepository.save(u);
        });

        // Ensure Recruiter user & profile exists
        recruiterUser = userRepository.findByUsername("recruiter_m9").orElseGet(() -> {
            User u = User.builder()
                    .username("recruiter_m9")
                    .password("recruiter123")
                    .email("recruiter_m9@test.com")
                    .role("RECRUITER")
                    .firstName("Recruiter")
                    .lastName("Test")
                    .build();
            return userRepository.save(u);
        });

        recruiter = recruiterRepository.findByUserId(recruiterUser.getId()).orElseGet(() -> {
            Recruiter r = Recruiter.builder()
                    .user(recruiterUser)
                    .companyName("Module 9 Corp")
                    .designation("HR Lead")
                    .build();
            return recruiterRepository.save(r);
        });

        // Ensure Student user & profile exists
        studentUser = userRepository.findByUsername("student_m9").orElseGet(() -> {
            User u = User.builder()
                    .username("student_m9")
                    .password("student123")
                    .email("student_m9@test.com")
                    .role("STUDENT")
                    .firstName("Student")
                    .lastName("Candidate")
                    .build();
            return userRepository.save(u);
        });

        studentProfile = studentProfileRepository.findByUserId(studentUser.getId()).orElseGet(() -> {
            StudentProfile sp = StudentProfile.builder()
                    .user(studentUser)
                    .phone("9998887770")
                    .college("State University")
                    .degree("B.Tech")
                    .branch("Computer Science")
                    .skillsText("Java, Spring Boot, MySQL")
                    .build();
            return studentProfileRepository.save(sp);
        });

        // Ensure JobPost exists
        jobPost = JobPost.builder()
                .recruiter(recruiter)
                .title("Senior Backend Engineer")
                .companyName("Module 9 Corp")
                .description("Build scalable services")
                .requiredSkills("Java, Spring Boot")
                .location("Remote")
                .jobType("Full Time")
                .status("PUBLISHED")
                .deadline(LocalDate.now().plusDays(30))
                .build();
        jobPost = jobPostRepository.save(jobPost);

        // Ensure JobApplication exists
        jobApplication = JobApplication.builder()
                .jobPost(jobPost)
                .studentProfile(studentProfile)
                .status("APPLIED")
                .finalDecision(ApplicationDecision.PENDING)
                .build();
        jobApplication = jobApplicationRepository.save(jobApplication);

        // Ensure AtsScore exists
        AtsScore atsScore = AtsScore.builder()
                .jobApplication(jobApplication)
                .overallScore(new BigDecimal("88.50"))
                .skillMatchScore(new BigDecimal("90.00"))
                .experienceMatchScore(new BigDecimal("85.00"))
                .educationMatchScore(new BigDecimal("90.00"))
                .feedback("Excellent match")
                .build();
        atsScoreRepository.save(atsScore);

        // Ensure ResumeAnalysis exists (primary ATS source across application)
        ResumeAnalysis resumeAnalysis = ResumeAnalysis.builder()
                .studentProfile(studentProfile)
                .jobPost(jobPost)
                .resumeVersionNumber(1)
                .atsScore(89.5)
                .skillMatchPercentage(90.0)
                .matchedSkills("Java, Spring Boot")
                .analyzedAt(java.time.LocalDateTime.now())
                .build();
        resumeAnalysisRepository.save(resumeAnalysis);

        // Ensure Interview exists
        interview = Interview.builder()
                .jobApplication(jobApplication)
                .recruiter(recruiter)
                .student(studentProfile)
                .roundName("Technical Interview")
                .interviewDate(LocalDate.now().plusDays(2))
                .interviewTime(java.time.LocalTime.of(10, 30))
                .interviewType(InterviewType.ONLINE)
                .status(InterviewStatus.SCHEDULED)
                .result(InterviewResult.PENDING)
                .build();
        interview = interviewRepository.save(interview);
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("1. Admin dashboard loads successfully for ADMIN role")
    void testAdminDashboardLoadsForAdmin() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(model().attributeExists("stats"))
                .andExpect(model().attributeExists("fullName"));
    }

    @Test
    @WithMockUser(username = "recruiter_m9", roles = {"RECRUITER"})
    @DisplayName("2a. Recruiter is denied access to /admin/dashboard")
    void testRecruiterDeniedAdminDashboard() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "student_m9", roles = {"STUDENT"})
    @DisplayName("2b. Student is denied access to /admin/dashboard")
    void testStudentDeniedAdminDashboard() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("2c. Unauthenticated user is denied or redirected for /admin/dashboard")
    void testUnauthenticatedDeniedAdminDashboard() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection()); // Redirected to /login
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("3. Admin users page loads for ADMIN")
    void testAdminUsersPageLoads() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users"))
                .andExpect(model().attributeExists("users"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("4. Admin students page loads for ADMIN")
    void testAdminStudentsPageLoads() throws Exception {
        mockMvc.perform(get("/admin/students"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/students"))
                .andExpect(model().attributeExists("students"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("5. Admin recruiters page loads for ADMIN")
    void testAdminRecruitersPageLoads() throws Exception {
        mockMvc.perform(get("/admin/recruiters"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/recruiters"))
                .andExpect(model().attributeExists("recruiters"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("6. Admin jobs page loads for ADMIN")
    void testAdminJobsPageLoads() throws Exception {
        mockMvc.perform(get("/admin/jobs"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/jobs"))
                .andExpect(model().attributeExists("jobs"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("7. Admin applications page loads for ADMIN")
    void testAdminApplicationsPageLoads() throws Exception {
        mockMvc.perform(get("/admin/applications"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/applications"))
                .andExpect(model().attributeExists("applications"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("8. Admin reports page loads for ADMIN")
    void testAdminReportsPageLoads() throws Exception {
        mockMvc.perform(get("/admin/reports"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/reports"))
                .andExpect(model().attributeExists("reports"));
    }

    @Test
    @DisplayName("9. Dashboard statistics reflect actual database records")
    void testDashboardStatsRealDatabaseValues() {
        AdminDashboardStatsDto stats = adminService.getDashboardStats();
        assertNotNull(stats);
        assertTrue(stats.getTotalUsers() >= 3);
        assertTrue(stats.getTotalStudents() >= 1);
        assertTrue(stats.getTotalRecruiters() >= 1);
        assertTrue(stats.getTotalJobs() >= 1);
        assertTrue(stats.getTotalApplications() >= 1);
        assertTrue(stats.getTotalInterviews() >= 1);
    }

    @Test
    @DisplayName("10. Password and password hash are not exposed in AdminUserDto")
    void testPasswordNotExposedInUserDto() {
        List<AdminUserDto> users = adminService.getUsers(null, null);
        assertFalse(users.isEmpty());
        for (AdminUserDto u : users) {
            // Verify AdminUserDto class definition has no password property
            assertFalse(hasProperty(AdminUserDto.class, "password"), "AdminUserDto must NOT have a password field");
            assertFalse(hasProperty(AdminUserDto.class, "passwordHash"), "AdminUserDto must NOT have a passwordHash field");
        }
    }

    @Test
    @DisplayName("11. User filtering by query and role works accurately")
    void testUserFiltering() {
        List<AdminUserDto> filtered = adminService.getUsers("student_m9", "STUDENT");
        assertFalse(filtered.isEmpty());
        assertEquals("student_m9", filtered.get(0).getUsername());
    }

    @Test
    @DisplayName("12. Student searching by skill/college works accurately")
    void testStudentFiltering() {
        List<AdminStudentDto> filtered = adminService.getStudents("State University");
        assertFalse(filtered.isEmpty());
        assertEquals("State University", filtered.get(0).getCollege());
    }

    @Test
    @DisplayName("13. Job filtering by status works accurately")
    void testJobFiltering() {
        List<AdminJobDto> filtered = adminService.getJobs("Backend", "PUBLISHED");
        assertFalse(filtered.isEmpty());
        assertEquals("Senior Backend Engineer", filtered.get(0).getTitle());
    }

    @Test
    @DisplayName("14. Application management reflects correct ATS score and status")
    void testApplicationManagementData() {
        List<AdminApplicationDto> apps = adminService.getApplications("Senior Backend Engineer", null);
        assertFalse(apps.isEmpty());
        AdminApplicationDto dto = apps.get(0);
        assertEquals("Senior Backend Engineer", dto.getJobTitle());
        assertEquals(89.5, dto.getAtsScore());
        assertEquals("89.5%", dto.getFormattedAtsScore());
    }

    @Test
    @DisplayName("15. Recruitment reports generate correct pipeline metrics")
    void testReportsGeneration() {
        AdminReportDto reports = adminService.getReports();
        assertNotNull(reports);
        assertNotNull(reports.getRecruitmentSummary());
        assertNotNull(reports.getCandidatePipeline());
        assertNotNull(reports.getJobSummaries());
        assertNotNull(reports.getRecruiterSummaries());
        assertTrue(reports.getRecruitmentSummary().getTotalJobs() >= 1);
    }

    private boolean hasProperty(Class<?> clazz, String propertyName) {
        try {
            clazz.getDeclaredField(propertyName);
            return true;
        } catch (NoSuchFieldException e) {
            return false;
        }
    }
}
