package com.resume.screening.controller;

import com.resume.screening.dto.AdminApplicationDto;
import com.resume.screening.dto.AdminUserDto;
import com.resume.screening.dto.CreateInterviewRequestDto;
import com.resume.screening.dto.InterviewFeedbackRequestDto;
import com.resume.screening.entity.*;
import com.resume.screening.enums.ApplicationDecision;
import com.resume.screening.repository.*;
import com.resume.screening.service.AdminService;
import com.resume.screening.service.InterviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class Module10SecurityAndDeploymentAuditTest {

    @Autowired
    private MockMvc mockMvc;

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
    private ResumeAnalysisRepository resumeAnalysisRepository;

    @Autowired
    private AdminService adminService;

    @Autowired
    private InterviewService interviewService;

    private User studentUser1;
    private User studentUser2;
    private User recruiterUser;
    private StudentProfile profile1;
    private StudentProfile profile2;
    private Recruiter recruiter;
    private JobPost jobPost;
    private JobApplication application1;
    private Interview interview1;

    @BeforeEach
    void setUp() {
        // Create 2 student users for cross-access testing
        studentUser1 = userRepository.findByUsername("student_m10_1").orElseGet(() -> {
            User u = User.builder()
                    .username("student_m10_1")
                    .password("pass123")
                    .email("student1@m10.com")
                    .role("STUDENT")
                    .firstName("Student")
                    .lastName("One")
                    .build();
            return userRepository.save(u);
        });

        studentUser2 = userRepository.findByUsername("student_m10_2").orElseGet(() -> {
            User u = User.builder()
                    .username("student_m10_2")
                    .password("pass123")
                    .email("student2@m10.com")
                    .role("STUDENT")
                    .firstName("Student")
                    .lastName("Two")
                    .build();
            return userRepository.save(u);
        });

        profile1 = studentProfileRepository.findByUserId(studentUser1.getId()).orElseGet(() -> {
            StudentProfile sp = StudentProfile.builder().user(studentUser1).college("Tech Univ").build();
            return studentProfileRepository.save(sp);
        });

        profile2 = studentProfileRepository.findByUserId(studentUser2.getId()).orElseGet(() -> {
            StudentProfile sp = StudentProfile.builder().user(studentUser2).college("City College").build();
            return studentProfileRepository.save(sp);
        });

        recruiterUser = userRepository.findByUsername("recruiter_m10").orElseGet(() -> {
            User u = User.builder()
                    .username("recruiter_m10")
                    .password("pass123")
                    .email("recruiter@m10.com")
                    .role("RECRUITER")
                    .firstName("Recruiter")
                    .lastName("M10")
                    .build();
            return userRepository.save(u);
        });

        recruiter = recruiterRepository.findByUserId(recruiterUser.getId()).orElseGet(() -> {
            Recruiter r = Recruiter.builder().user(recruiterUser).companyName("M10 Corp").build();
            return recruiterRepository.save(r);
        });

        jobPost = JobPost.builder()
                .recruiter(recruiter)
                .title("Software Engineer M10")
                .companyName("M10 Corp")
                .description("Engineering role description")
                .status("PUBLISHED")
                .build();
        jobPost = jobPostRepository.save(jobPost);

        application1 = JobApplication.builder()
                .jobPost(jobPost)
                .studentProfile(profile1)
                .status("SHORTLISTED")
                .finalDecision(ApplicationDecision.PENDING)
                .build();
        application1 = jobApplicationRepository.save(application1);

        interview1 = Interview.builder()
                .jobApplication(application1)
                .recruiter(recruiter)
                .student(profile1)
                .roundName("Technical Round 1")
                .interviewDate(LocalDate.now().plusDays(1))
                .interviewTime(LocalTime.of(14, 0))
                .interviewType(InterviewType.ONLINE)
                .meetingLink("https://meet.jit.si/m10-interview")
                .status(InterviewStatus.SCHEDULED)
                .result(InterviewResult.PENDING)
                .build();
        interview1 = interviewRepository.save(interview1);
    }

    @Test
    @WithMockUser(username = "student_m10_1", roles = {"STUDENT"})
    @DisplayName("1. Security: Student is denied access to Admin Dashboard")
    void testStudentDeniedAdminAccess() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "student_m10_1", roles = {"STUDENT"})
    @DisplayName("2. Security: Student is denied access to Recruiter Jobs")
    void testStudentDeniedRecruiterAccess() throws Exception {
        mockMvc.perform(get("/recruiter/jobs"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "recruiter_m10", roles = {"RECRUITER"})
    @DisplayName("3. Security: Recruiter is denied access to Admin Users")
    void testRecruiterDeniedAdminAccess() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "student_m10_2", roles = {"STUDENT"})
    @DisplayName("4. Ownership: Student cannot view another student's interview details")
    void testCrossStudentInterviewAccessDenied() {
        assertThrows(AccessDeniedException.class, () -> {
            interviewService.getInterviewById(interview1.getId(), "student_m10_2");
        });
    }

    @Test
    @DisplayName("5. Ownership: Student can view their own interview details")
    void testStudentViewOwnInterviewAllowed() {
        assertDoesNotThrow(() -> {
            interviewService.getInterviewById(interview1.getId(), "student_m10_1");
        });
    }

    @Test
    @DisplayName("6. Input Validation: ONLINE interview requires meeting link")
    void testOnlineInterviewRequiresMeetingLink() {
        CreateInterviewRequestDto req = CreateInterviewRequestDto.builder()
                .jobApplicationId(application1.getId())
                .roundName("HR Round")
                .interviewDate(LocalDate.now().plusDays(2))
                .interviewTime(LocalTime.of(15, 0))
                .interviewType(InterviewType.ONLINE)
                .meetingLink("") // empty meeting link
                .build();

        assertThrows(IllegalArgumentException.class, () -> {
            interviewService.createInterview(req, "recruiter_m10");
        });
    }

    @Test
    @DisplayName("7. Input Validation: Rating must be between 1 and 5")
    void testInvalidRatingRejected() {
        interview1.setStatus(InterviewStatus.COMPLETED);
        interviewRepository.save(interview1);

        InterviewFeedbackRequestDto fb = InterviewFeedbackRequestDto.builder()
                .rating(10) // invalid rating > 5
                .feedback("Great performance")
                .result(InterviewResult.SELECTED)
                .build();

        assertThrows(IllegalArgumentException.class, () -> {
            interviewService.submitFeedback(interview1.getId(), fb, "recruiter_m10");
        });
    }

    @Test
    @DisplayName("8. ATS Mapping: ResumeAnalysis score is mapped accurately to AdminApplicationDto")
    void testResumeAnalysisAtsMappingInAdmin() {
        ResumeAnalysis ra = ResumeAnalysis.builder()
                .studentProfile(profile1)
                .jobPost(jobPost)
                .resumeVersionNumber(1)
                .atsScore(91.2)
                .skillMatchPercentage(92.0)
                .matchedSkills("Java, Spring Boot")
                .analyzedAt(java.time.LocalDateTime.now())
                .build();
        resumeAnalysisRepository.save(ra);

        List<AdminApplicationDto> apps = adminService.getApplications("Software Engineer M10", null);
        assertFalse(apps.isEmpty());
        AdminApplicationDto dto = apps.get(0);
        assertEquals(91.2, dto.getAtsScore());
        assertEquals("91.2%", dto.getFormattedAtsScore());
    }

    @Test
    @DisplayName("9. Password Security: AdminUserDto does not expose password fields")
    void testAdminUserDtoSecurity() {
        List<AdminUserDto> users = adminService.getUsers(null, null);
        assertFalse(users.isEmpty());
        for (AdminUserDto u : users) {
            try {
                AdminUserDto.class.getDeclaredField("password");
                fail("AdminUserDto must not contain password field!");
            } catch (NoSuchFieldException expected) {}
            
            try {
                AdminUserDto.class.getDeclaredField("passwordHash");
                fail("AdminUserDto must not contain passwordHash field!");
            } catch (NoSuchFieldException expected) {}
        }
    }
}
