package com.resume.screening.service;

import com.resume.screening.dto.InterviewDto;
import com.resume.screening.dto.InterviewFeedbackRequestDto;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class Module8InterviewFeedbackTests {

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

    private Recruiter recruiter;
    private Recruiter otherRecruiter;
    private StudentProfile studentProfile;

    private JobPost jobPost;
    private JobApplication jobApplication;
    private Interview interviewScheduled;
    private Interview interviewCompleted;
    private Interview interviewCancelled;

    @BeforeEach
    void setUp() {
        recruiterUser = User.builder().id(10L).username("recruiter1").role("RECRUITER").firstName("John").lastName("Doe").email("john@corp.com").build();
        otherRecruiterUser = User.builder().id(20L).username("recruiter2").role("RECRUITER").firstName("Jane").lastName("Smith").email("jane@other.com").build();
        studentUser = User.builder().id(30L).username("student1").role("STUDENT").firstName("Alice").lastName("Brown").email("alice@student.com").build();

        recruiter = Recruiter.builder().id(100L).user(recruiterUser).companyName("Tech Corp").build();
        otherRecruiter = Recruiter.builder().id(200L).user(otherRecruiterUser).companyName("Other Corp").build();
        studentProfile = StudentProfile.builder().id(300L).user(studentUser).build();

        jobPost = JobPost.builder().id(1000L).title("Senior Java Developer").recruiter(recruiter).build();
        jobApplication = JobApplication.builder().id(5000L).jobPost(jobPost).studentProfile(studentProfile).status("SHORTLISTED").build();

        interviewScheduled = Interview.builder()
                .id(8000L)
                .jobApplication(jobApplication)
                .recruiter(recruiter)
                .student(studentProfile)
                .roundName("Technical Round 1")
                .interviewDate(LocalDate.now().plusDays(2))
                .interviewTime(LocalTime.of(10, 30))
                .interviewType(InterviewType.ONLINE)
                .meetingLink("https://meet.google.com/abc")
                .status(InterviewStatus.SCHEDULED)
                .result(InterviewResult.PENDING)
                .build();

        interviewCompleted = Interview.builder()
                .id(8001L)
                .jobApplication(jobApplication)
                .recruiter(recruiter)
                .student(studentProfile)
                .roundName("Technical Round 1")
                .interviewDate(LocalDate.now().minusDays(1))
                .interviewTime(LocalTime.of(14, 0))
                .interviewType(InterviewType.OFFLINE)
                .location("Ahmedabad")
                .status(InterviewStatus.COMPLETED)
                .result(InterviewResult.PENDING)
                .build();

        interviewCancelled = Interview.builder()
                .id(8002L)
                .jobApplication(jobApplication)
                .recruiter(recruiter)
                .student(studentProfile)
                .roundName("HR Round")
                .interviewDate(LocalDate.now().minusDays(2))
                .interviewTime(LocalTime.of(11, 0))
                .interviewType(InterviewType.ONLINE)
                .status(InterviewStatus.CANCELLED)
                .result(InterviewResult.PENDING)
                .build();
    }

    @Test
    void testInterviewResultEnumValues() {
        assertEquals(4, InterviewResult.values().length);
        assertEquals(InterviewResult.PENDING, InterviewResult.valueOf("PENDING"));
        assertEquals(InterviewResult.SELECTED, InterviewResult.valueOf("SELECTED"));
        assertEquals(InterviewResult.REJECTED, InterviewResult.valueOf("REJECTED"));
        assertEquals(InterviewResult.ON_HOLD, InterviewResult.valueOf("ON_HOLD"));
    }

    @Test
    void testDefaultResultIsPending() {
        Interview newInterview = Interview.builder()
                .roundName("Round 1")
                .interviewDate(LocalDate.now())
                .interviewTime(LocalTime.now())
                .interviewType(InterviewType.ONLINE)
                .build();

        assertEquals(InterviewResult.PENDING, newInterview.getResult());
    }

    @Test
    void testCompleteInterview_Success() {
        when(interviewRepository.findById(8000L)).thenReturn(Optional.of(interviewScheduled));
        when(userResolutionService.resolveUser("recruiter1")).thenReturn(recruiterUser);
        when(interviewRepository.save(any(Interview.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InterviewDto result = interviewService.completeInterview(8000L, "recruiter1");

        assertNotNull(result);
        assertEquals(InterviewStatus.COMPLETED, result.getStatus());
        assertEquals(InterviewResult.PENDING, result.getResult());
        verify(interviewRepository, times(1)).save(any(Interview.class));
    }

    @Test
    void testCompleteInterview_CancelledInterview_ThrowsException() {
        when(interviewRepository.findById(8002L)).thenReturn(Optional.of(interviewCancelled));
        when(userResolutionService.resolveUser("recruiter1")).thenReturn(recruiterUser);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                interviewService.completeInterview(8002L, "recruiter1")
        );

        assertTrue(ex.getMessage().contains("Cannot complete a cancelled interview"));
        verify(interviewRepository, never()).save(any());
    }

    @Test
    void testCompleteInterview_NotOwner_ThrowsAccessDeniedException() {
        when(interviewRepository.findById(8000L)).thenReturn(Optional.of(interviewScheduled));
        when(userResolutionService.resolveUser("recruiter2")).thenReturn(otherRecruiterUser);

        assertThrows(AccessDeniedException.class, () ->
                interviewService.completeInterview(8000L, "recruiter2")
        );
    }

    @Test
    void testSubmitFeedback_Success() {
        InterviewFeedbackRequestDto request = InterviewFeedbackRequestDto.builder()
                .rating(4)
                .result(InterviewResult.SELECTED)
                .feedback("Strong technical skills and clear communication.")
                .build();

        when(interviewRepository.findById(8001L)).thenReturn(Optional.of(interviewCompleted));
        when(userResolutionService.resolveUser("recruiter1")).thenReturn(recruiterUser);
        when(interviewRepository.save(any(Interview.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InterviewDto dto = interviewService.submitFeedback(8001L, request, "recruiter1");

        assertNotNull(dto);
        assertEquals(4, dto.getRating());
        assertEquals(InterviewResult.SELECTED, dto.getResult());
        assertEquals("Strong technical skills and clear communication.", dto.getFeedback());
    }

    @Test
    void testSubmitFeedback_ScheduledInterview_ThrowsException() {
        InterviewFeedbackRequestDto request = InterviewFeedbackRequestDto.builder()
                .rating(5)
                .result(InterviewResult.SELECTED)
                .feedback("Attempting feedback on scheduled interview")
                .build();

        when(interviewRepository.findById(8000L)).thenReturn(Optional.of(interviewScheduled));
        when(userResolutionService.resolveUser("recruiter1")).thenReturn(recruiterUser);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                interviewService.submitFeedback(8000L, request, "recruiter1")
        );

        assertTrue(ex.getMessage().contains("Feedback can only be submitted for completed interviews"));
        verify(interviewRepository, never()).save(any());
    }

    @Test
    void testSubmitFeedback_CancelledInterview_ThrowsException() {
        InterviewFeedbackRequestDto request = InterviewFeedbackRequestDto.builder()
                .rating(3)
                .result(InterviewResult.REJECTED)
                .feedback("Feedback on cancelled interview")
                .build();

        when(interviewRepository.findById(8002L)).thenReturn(Optional.of(interviewCancelled));
        when(userResolutionService.resolveUser("recruiter1")).thenReturn(recruiterUser);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                interviewService.submitFeedback(8002L, request, "recruiter1")
        );

        assertTrue(ex.getMessage().contains("Feedback can only be submitted for completed interviews"));
    }

    @Test
    void testSubmitFeedback_InvalidRatingRange_ThrowsException() {
        InterviewFeedbackRequestDto requestHigh = InterviewFeedbackRequestDto.builder().rating(6).build();
        InterviewFeedbackRequestDto requestLow = InterviewFeedbackRequestDto.builder().rating(0).build();

        when(interviewRepository.findById(8001L)).thenReturn(Optional.of(interviewCompleted));
        when(userResolutionService.resolveUser("recruiter1")).thenReturn(recruiterUser);

        assertThrows(IllegalArgumentException.class, () -> interviewService.submitFeedback(8001L, requestHigh, "recruiter1"));
        assertThrows(IllegalArgumentException.class, () -> interviewService.submitFeedback(8001L, requestLow, "recruiter1"));
    }

    @Test
    void testSubmitFeedback_NotOwner_ThrowsAccessDeniedException() {
        InterviewFeedbackRequestDto request = InterviewFeedbackRequestDto.builder().rating(4).build();

        when(interviewRepository.findById(8001L)).thenReturn(Optional.of(interviewCompleted));
        when(userResolutionService.resolveUser("recruiter2")).thenReturn(otherRecruiterUser);

        assertThrows(AccessDeniedException.class, () ->
                interviewService.submitFeedback(8001L, request, "recruiter2")
        );
    }
}
