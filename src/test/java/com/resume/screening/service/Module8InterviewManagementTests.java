package com.resume.screening.service;

import com.resume.screening.dto.CreateInterviewRequestDto;
import com.resume.screening.dto.InterviewDto;
import com.resume.screening.entity.*;
import com.resume.screening.repository.*;
import com.resume.screening.service.impl.InterviewServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class Module8InterviewManagementTests {

    @Mock
    private InterviewRepository interviewRepository;

    @Mock
    private JobApplicationRepository jobApplicationRepository;

    @Mock
    private RecruiterRepository recruiterRepository;

    @Mock
    private StudentProfileRepository studentProfileRepository;

    @Mock
    private ShortlistedCandidateRepository shortlistedCandidateRepository;

    @Mock
    private UserResolutionService userResolutionService;

    @InjectMocks
    private InterviewServiceImpl interviewService;

    private User recruiterUser;
    private User otherRecruiterUser;
    private User studentUser;
    private User otherStudentUser;

    private Recruiter recruiter;
    private Recruiter otherRecruiter;
    private StudentProfile studentProfile;
    private StudentProfile otherStudentProfile;

    private JobPost jobPost;
    private JobApplication jobApplication;
    private Interview interview;

    @BeforeEach
    void setUp() {
        recruiterUser = User.builder().id(10L).username("recruiter1").role("RECRUITER").firstName("John").lastName("Doe").email("john@corp.com").build();
        otherRecruiterUser = User.builder().id(20L).username("recruiter2").role("RECRUITER").firstName("Jane").lastName("Smith").email("jane@other.com").build();
        studentUser = User.builder().id(30L).username("student1").role("STUDENT").firstName("Alice").lastName("Brown").email("alice@student.com").build();
        otherStudentUser = User.builder().id(40L).username("student2").role("STUDENT").firstName("Bob").lastName("Green").email("bob@student.com").build();

        recruiter = Recruiter.builder().id(100L).user(recruiterUser).companyName("Tech Corp").build();
        otherRecruiter = Recruiter.builder().id(200L).user(otherRecruiterUser).companyName("Other Corp").build();

        studentProfile = StudentProfile.builder().id(300L).user(studentUser).build();
        otherStudentProfile = StudentProfile.builder().id(400L).user(otherStudentUser).build();

        jobPost = JobPost.builder().id(1000L).title("Senior Java Developer").recruiter(recruiter).build();

        jobApplication = JobApplication.builder()
                .id(5000L)
                .jobPost(jobPost)
                .studentProfile(studentProfile)
                .status("SHORTLISTED")
                .build();

        interview = Interview.builder()
                .id(8000L)
                .jobApplication(jobApplication)
                .recruiter(recruiter)
                .student(studentProfile)
                .roundName("Technical Round 1")
                .interviewDate(LocalDate.now().plusDays(2))
                .interviewTime(LocalTime.of(10, 30))
                .interviewType(InterviewType.ONLINE)
                .meetingLink("https://meet.google.com/abc-defg-hij")
                .status(InterviewStatus.SCHEDULED)
                .notes("Focus on Java 17 and Spring Boot")
                .build();
    }

    @Test
    void testCreateInterview_Success() {
        CreateInterviewRequestDto request = CreateInterviewRequestDto.builder()
                .jobApplicationId(5000L)
                .roundName("Technical Round 1")
                .interviewDate(LocalDate.now().plusDays(2))
                .interviewTime(LocalTime.of(10, 30))
                .interviewType(InterviewType.ONLINE)
                .meetingLink("https://meet.google.com/abc-defg-hij")
                .notes("Focus on Java 17")
                .build();

        when(userResolutionService.resolveUser("recruiter1")).thenReturn(recruiterUser);
        when(recruiterRepository.findByUserId(10L)).thenReturn(Optional.of(recruiter));
        when(jobApplicationRepository.findById(5000L)).thenReturn(Optional.of(jobApplication));
        when(interviewRepository.save(any(Interview.class))).thenReturn(interview);

        InterviewDto result = interviewService.createInterview(request, "recruiter1");

        assertNotNull(result);
        assertEquals(8000L, result.getId());
        assertEquals("Technical Round 1", result.getRoundName());
        assertEquals(InterviewType.ONLINE, result.getInterviewType());
        assertEquals(InterviewStatus.SCHEDULED, result.getStatus());
        assertEquals("Senior Java Developer", result.getJobTitle());
        assertEquals("Tech Corp", result.getCompanyName());

        verify(interviewRepository, times(1)).save(any(Interview.class));
    }

    @Test
    void testCreateInterview_NotShortlisted_ThrowsException() {
        jobApplication.setStatus("APPLIED");
        CreateInterviewRequestDto request = CreateInterviewRequestDto.builder()
                .jobApplicationId(5000L)
                .roundName("Technical Round 1")
                .interviewDate(LocalDate.now().plusDays(2))
                .interviewTime(LocalTime.of(10, 30))
                .interviewType(InterviewType.ONLINE)
                .meetingLink("https://meet.google.com/abc")
                .build();

        when(userResolutionService.resolveUser("recruiter1")).thenReturn(recruiterUser);
        when(recruiterRepository.findByUserId(10L)).thenReturn(Optional.of(recruiter));
        when(jobApplicationRepository.findById(5000L)).thenReturn(Optional.of(jobApplication));
        when(shortlistedCandidateRepository.existsByJobPostIdAndStudentProfileId(1000L, 300L)).thenReturn(false);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                interviewService.createInterview(request, "recruiter1")
        );

        assertTrue(exception.getMessage().contains("Candidate is not shortlisted"));
        verify(interviewRepository, never()).save(any());
    }

    @Test
    void testCreateInterview_OnlineWithoutMeetingLink_ThrowsException() {
        CreateInterviewRequestDto request = CreateInterviewRequestDto.builder()
                .jobApplicationId(5000L)
                .roundName("Technical Round 1")
                .interviewDate(LocalDate.now().plusDays(2))
                .interviewTime(LocalTime.of(10, 30))
                .interviewType(InterviewType.ONLINE)
                .meetingLink("   ")
                .build();

        when(userResolutionService.resolveUser("recruiter1")).thenReturn(recruiterUser);
        when(recruiterRepository.findByUserId(10L)).thenReturn(Optional.of(recruiter));
        when(jobApplicationRepository.findById(5000L)).thenReturn(Optional.of(jobApplication));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                interviewService.createInterview(request, "recruiter1")
        );

        assertTrue(exception.getMessage().contains("Meeting link is required for ONLINE interviews"));
    }

    @Test
    void testCreateInterview_OfflineWithoutLocation_ThrowsException() {
        CreateInterviewRequestDto request = CreateInterviewRequestDto.builder()
                .jobApplicationId(5000L)
                .roundName("HR Round")
                .interviewDate(LocalDate.now().plusDays(2))
                .interviewTime(LocalTime.of(10, 30))
                .interviewType(InterviewType.OFFLINE)
                .location("")
                .build();

        when(userResolutionService.resolveUser("recruiter1")).thenReturn(recruiterUser);
        when(recruiterRepository.findByUserId(10L)).thenReturn(Optional.of(recruiter));
        when(jobApplicationRepository.findById(5000L)).thenReturn(Optional.of(jobApplication));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                interviewService.createInterview(request, "recruiter1")
        );

        assertTrue(exception.getMessage().contains("Location is required for OFFLINE interviews"));
    }

    @Test
    void testCreateInterview_OfflineWithLocation_Success() {
        CreateInterviewRequestDto request = CreateInterviewRequestDto.builder()
                .jobApplicationId(5000L)
                .roundName("HR Round")
                .interviewDate(LocalDate.now().plusDays(2))
                .interviewTime(LocalTime.of(14, 0))
                .interviewType(InterviewType.OFFLINE)
                .location("Headquarters, Conference Room A")
                .notes("In-person interview")
                .build();

        Interview offlineInterview = Interview.builder()
                .id(8001L)
                .jobApplication(jobApplication)
                .recruiter(recruiter)
                .student(studentProfile)
                .roundName("HR Round")
                .interviewDate(LocalDate.now().plusDays(2))
                .interviewTime(LocalTime.of(14, 0))
                .interviewType(InterviewType.OFFLINE)
                .location("Headquarters, Conference Room A")
                .status(InterviewStatus.SCHEDULED)
                .notes("In-person interview")
                .build();

        when(userResolutionService.resolveUser("recruiter1")).thenReturn(recruiterUser);
        when(recruiterRepository.findByUserId(10L)).thenReturn(Optional.of(recruiter));
        when(jobApplicationRepository.findById(5000L)).thenReturn(Optional.of(jobApplication));
        when(interviewRepository.existsByJobApplicationIdAndStatusIn(eq(5000L), anyList())).thenReturn(false);
        when(interviewRepository.save(any(Interview.class))).thenReturn(offlineInterview);

        InterviewDto result = interviewService.createInterview(request, "recruiter1");

        assertNotNull(result);
        assertEquals(8001L, result.getId());
        assertEquals("HR Round", result.getRoundName());
        assertEquals(InterviewType.OFFLINE, result.getInterviewType());
        assertEquals("Headquarters, Conference Room A", result.getLocation());
    }

    @Test
    void testCreateInterview_DuplicateActiveInterview_ThrowsException() {
        CreateInterviewRequestDto request = CreateInterviewRequestDto.builder()
                .jobApplicationId(5000L)
                .roundName("Technical Round 2")
                .interviewDate(LocalDate.now().plusDays(3))
                .interviewTime(LocalTime.of(11, 0))
                .interviewType(InterviewType.ONLINE)
                .meetingLink("https://meet.google.com/xyz")
                .build();

        when(userResolutionService.resolveUser("recruiter1")).thenReturn(recruiterUser);
        when(recruiterRepository.findByUserId(10L)).thenReturn(Optional.of(recruiter));
        when(jobApplicationRepository.findById(5000L)).thenReturn(Optional.of(jobApplication));
        when(interviewRepository.existsByJobApplicationIdAndStatusIn(eq(5000L), anyList())).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                interviewService.createInterview(request, "recruiter1")
        );

        assertTrue(exception.getMessage().contains("An interview is already scheduled for this candidate"));
        verify(interviewRepository, never()).save(any());
    }

    @Test
    void testCreateInterview_RecruiterNotOwner_ThrowsAccessDeniedException() {
        CreateInterviewRequestDto request = CreateInterviewRequestDto.builder()
                .jobApplicationId(5000L)
                .roundName("Technical Round 1")
                .interviewDate(LocalDate.now().plusDays(2))
                .interviewTime(LocalTime.of(10, 30))
                .interviewType(InterviewType.ONLINE)
                .meetingLink("https://meet.google.com/abc")
                .build();

        when(userResolutionService.resolveUser("recruiter2")).thenReturn(otherRecruiterUser);
        when(recruiterRepository.findByUserId(20L)).thenReturn(Optional.of(otherRecruiter));
        when(jobApplicationRepository.findById(5000L)).thenReturn(Optional.of(jobApplication));

        assertThrows(AccessDeniedException.class, () ->
                interviewService.createInterview(request, "recruiter2")
        );
    }

    @Test
    void testGetInterviewById_RecruiterOwnership_Success() {
        when(interviewRepository.findById(8000L)).thenReturn(Optional.of(interview));
        when(userResolutionService.resolveUser("recruiter1")).thenReturn(recruiterUser);

        InterviewDto dto = interviewService.getInterviewById(8000L, "recruiter1");
        assertNotNull(dto);
        assertEquals(8000L, dto.getId());
    }

    @Test
    void testGetInterviewById_StudentOwnership_Success() {
        when(interviewRepository.findById(8000L)).thenReturn(Optional.of(interview));
        when(userResolutionService.resolveUser("student1")).thenReturn(studentUser);

        InterviewDto dto = interviewService.getInterviewById(8000L, "student1");
        assertNotNull(dto);
        assertEquals(8000L, dto.getId());
    }

    @Test
    void testGetInterviewById_UnauthorizedUser_ThrowsAccessDeniedException() {
        when(interviewRepository.findById(8000L)).thenReturn(Optional.of(interview));
        when(userResolutionService.resolveUser("student2")).thenReturn(otherStudentUser);

        assertThrows(AccessDeniedException.class, () ->
                interviewService.getInterviewById(8000L, "student2")
        );
    }

    @Test
    void testGetRecruiterInterviews() {
        when(interviewRepository.findByRecruiter_User_Username("recruiter1")).thenReturn(List.of(interview));

        List<InterviewDto> result = interviewService.getRecruiterInterviews("recruiter1");
        assertEquals(1, result.size());
        assertEquals("Technical Round 1", result.get(0).getRoundName());
    }

    @Test
    void testGetStudentInterviews() {
        when(interviewRepository.findByStudent_User_Username("student1")).thenReturn(List.of(interview));

        List<InterviewDto> result = interviewService.getStudentInterviews("student1");
        assertEquals(1, result.size());
        assertEquals("Tech Corp", result.get(0).getCompanyName());
    }

    @Test
    void testUpdateInterview_StatusAndNotes_Success() {
        CreateInterviewRequestDto updateRequest = CreateInterviewRequestDto.builder()
                .status(InterviewStatus.COMPLETED)
                .notes("Candidate passed technical round")
                .build();

        when(interviewRepository.findById(8000L)).thenReturn(Optional.of(interview));
        when(userResolutionService.resolveUser("recruiter1")).thenReturn(recruiterUser);
        when(interviewRepository.save(any(Interview.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InterviewDto updated = interviewService.updateInterview(8000L, updateRequest, "recruiter1");

        assertEquals(InterviewStatus.COMPLETED, updated.getStatus());
        assertEquals("Candidate passed technical round", updated.getNotes());
    }

    @Test
    void testCancelInterview_Success() {
        when(interviewRepository.findById(8000L)).thenReturn(Optional.of(interview));
        when(userResolutionService.resolveUser("recruiter1")).thenReturn(recruiterUser);
        when(interviewRepository.save(any(Interview.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InterviewDto result = interviewService.cancelInterview(8000L, "recruiter1");

        assertEquals(InterviewStatus.CANCELLED, result.getStatus());
    }

    @Test
    void testInterviewEnums() {
        assertEquals(2, InterviewType.values().length);
        assertEquals(InterviewType.ONLINE, InterviewType.valueOf("ONLINE"));
        assertEquals(InterviewType.OFFLINE, InterviewType.valueOf("OFFLINE"));

        assertEquals(4, InterviewStatus.values().length);
        assertEquals(InterviewStatus.SCHEDULED, InterviewStatus.valueOf("SCHEDULED"));
        assertEquals(InterviewStatus.COMPLETED, InterviewStatus.valueOf("COMPLETED"));
        assertEquals(InterviewStatus.CANCELLED, InterviewStatus.valueOf("CANCELLED"));
        assertEquals(InterviewStatus.RESCHEDULED, InterviewStatus.valueOf("RESCHEDULED"));
    }
}
