package com.resume.screening.service;

import com.resume.screening.dto.FinalCandidateDecisionDto;
import com.resume.screening.entity.*;
import com.resume.screening.enums.ApplicationDecision;
import com.resume.screening.repository.InterviewRepository;
import com.resume.screening.repository.JobApplicationRepository;
import com.resume.screening.service.impl.FinalCandidateSelectionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
class Module8FinalCandidateSelectionTests {

    @Mock
    private JobApplicationRepository jobApplicationRepository;

    @Mock
    private InterviewRepository interviewRepository;

    @Mock
    private UserResolutionService userResolutionService;

    @InjectMocks
    private FinalCandidateSelectionServiceImpl finalCandidateSelectionService;

    private User recruiterUser;
    private User otherRecruiterUser;
    private User studentUser;
    private Recruiter recruiter;
    private Recruiter otherRecruiter;
    private StudentProfile studentProfile;
    private JobPost jobPost;
    private JobApplication jobApplication;
    private Interview completedInterview;

    @BeforeEach
    void setUp() {
        recruiterUser = User.builder().id(1L).username("recruiter").role("ROLE_RECRUITER").email("recruiter@test.com").build();
        otherRecruiterUser = User.builder().id(2L).username("other_recruiter").role("ROLE_RECRUITER").email("other@test.com").build();
        studentUser = User.builder().id(3L).username("student").role("ROLE_STUDENT").email("student@test.com").build();

        recruiter = Recruiter.builder().id(1L).user(recruiterUser).companyName("TechCorp").build();
        otherRecruiter = Recruiter.builder().id(2L).user(otherRecruiterUser).companyName("OtherCorp").build();
        studentProfile = StudentProfile.builder().id(1L).user(studentUser).degree("B.Tech").build();

        jobPost = JobPost.builder().id(10L).title("Java Engineer").recruiter(recruiter).companyName("TechCorp").build();

        jobApplication = JobApplication.builder()
                .id(100L)
                .jobPost(jobPost)
                .studentProfile(studentProfile)
                .status("SHORTLISTED")
                .finalDecision(ApplicationDecision.PENDING)
                .build();

        completedInterview = Interview.builder()
                .id(500L)
                .jobApplication(jobApplication)
                .recruiter(recruiter)
                .student(studentProfile)
                .roundName("Tech Round")
                .interviewDate(LocalDate.now())
                .interviewTime(LocalTime.of(14, 0))
                .interviewType(InterviewType.ONLINE)
                .status(InterviewStatus.COMPLETED)
                .result(InterviewResult.SELECTED)
                .feedback("Great performance")
                .rating(5)
                .build();
    }

    @Test
    @DisplayName("1. Verify ApplicationDecision enum values")
    void testApplicationDecisionEnumValues() {
        assertEquals(3, ApplicationDecision.values().length);
        assertEquals(ApplicationDecision.PENDING, ApplicationDecision.valueOf("PENDING"));
        assertEquals(ApplicationDecision.SELECTED, ApplicationDecision.valueOf("SELECTED"));
        assertEquals(ApplicationDecision.REJECTED, ApplicationDecision.valueOf("REJECTED"));
    }

    @Test
    @DisplayName("2. Default final decision is PENDING")
    void testDefaultFinalDecisionIsPending() {
        JobApplication app = new JobApplication();
        assertEquals(ApplicationDecision.PENDING, app.getFinalDecision());
    }

    @Test
    @DisplayName("3. Recruiter can select an eligible candidate")
    void testSelectCandidate_Success() {
        when(jobApplicationRepository.findById(100L)).thenReturn(Optional.of(jobApplication));
        when(userResolutionService.resolveUser("recruiter")).thenReturn(recruiterUser);
        when(interviewRepository.findByJobApplicationId(100L))
                .thenReturn(List.of(completedInterview));
        when(jobApplicationRepository.save(any(JobApplication.class))).thenAnswer(i -> i.getArgument(0));

        FinalCandidateDecisionDto dto = finalCandidateSelectionService.selectCandidate(100L, "recruiter");

        assertNotNull(dto);
        assertEquals(ApplicationDecision.SELECTED, dto.getFinalDecision());
        assertEquals(100L, dto.getApplicationId());
        assertNotNull(dto.getDecidedAt());
        verify(jobApplicationRepository, times(1)).save(jobApplication);
    }

    @Test
    @DisplayName("4. Recruiter can reject an eligible candidate")
    void testRejectCandidate_Success() {
        when(jobApplicationRepository.findById(100L)).thenReturn(Optional.of(jobApplication));
        when(userResolutionService.resolveUser("recruiter")).thenReturn(recruiterUser);
        when(interviewRepository.findByJobApplicationId(100L))
                .thenReturn(List.of(completedInterview));
        when(jobApplicationRepository.save(any(JobApplication.class))).thenAnswer(i -> i.getArgument(0));

        FinalCandidateDecisionDto dto = finalCandidateSelectionService.rejectCandidate(100L, "recruiter");

        assertNotNull(dto);
        assertEquals(ApplicationDecision.REJECTED, dto.getFinalDecision());
        verify(jobApplicationRepository, times(1)).save(jobApplication);
    }

    @Test
    @DisplayName("5. Selection throws exception if interview is still SCHEDULED")
    void testSelectCandidate_ScheduledInterview_ThrowsException() {
        completedInterview.setStatus(InterviewStatus.SCHEDULED);
        when(jobApplicationRepository.findById(100L)).thenReturn(Optional.of(jobApplication));
        when(userResolutionService.resolveUser("recruiter")).thenReturn(recruiterUser);
        when(interviewRepository.findByJobApplicationId(100L))
                .thenReturn(List.of(completedInterview));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                finalCandidateSelectionService.selectCandidate(100L, "recruiter"));
        assertTrue(ex.getMessage().contains("must be completed"));
    }

    @Test
    @DisplayName("6. Selection throws exception if interview is CANCELLED")
    void testSelectCandidate_CancelledInterview_ThrowsException() {
        completedInterview.setStatus(InterviewStatus.CANCELLED);
        when(jobApplicationRepository.findById(100L)).thenReturn(Optional.of(jobApplication));
        when(userResolutionService.resolveUser("recruiter")).thenReturn(recruiterUser);
        when(interviewRepository.findByJobApplicationId(100L))
                .thenReturn(List.of(completedInterview));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                finalCandidateSelectionService.selectCandidate(100L, "recruiter"));
        assertTrue(ex.getMessage().contains("must be completed"));
    }

    @Test
    @DisplayName("7. Selection throws exception if InterviewResult is REJECTED")
    void testSelectCandidate_InterviewResultRejected_ThrowsException() {
        completedInterview.setResult(InterviewResult.REJECTED);
        when(jobApplicationRepository.findById(100L)).thenReturn(Optional.of(jobApplication));
        when(userResolutionService.resolveUser("recruiter")).thenReturn(recruiterUser);
        when(interviewRepository.findByJobApplicationId(100L))
                .thenReturn(List.of(completedInterview));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                finalCandidateSelectionService.selectCandidate(100L, "recruiter"));
        assertTrue(ex.getMessage().contains("must be SELECTED"));
    }

    @Test
    @DisplayName("8. Selection throws exception if InterviewResult is PENDING")
    void testSelectCandidate_InterviewResultPending_ThrowsException() {
        completedInterview.setResult(InterviewResult.PENDING);
        when(jobApplicationRepository.findById(100L)).thenReturn(Optional.of(jobApplication));
        when(userResolutionService.resolveUser("recruiter")).thenReturn(recruiterUser);
        when(interviewRepository.findByJobApplicationId(100L))
                .thenReturn(List.of(completedInterview));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                finalCandidateSelectionService.selectCandidate(100L, "recruiter"));
        assertTrue(ex.getMessage().contains("must be SELECTED"));
    }

    @Test
    @DisplayName("9. Selection throws exception if InterviewResult is ON_HOLD")
    void testSelectCandidate_InterviewResultOnHold_ThrowsException() {
        completedInterview.setResult(InterviewResult.ON_HOLD);
        when(jobApplicationRepository.findById(100L)).thenReturn(Optional.of(jobApplication));
        when(userResolutionService.resolveUser("recruiter")).thenReturn(recruiterUser);
        when(interviewRepository.findByJobApplicationId(100L))
                .thenReturn(List.of(completedInterview));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                finalCandidateSelectionService.selectCandidate(100L, "recruiter"));
        assertTrue(ex.getMessage().contains("must be SELECTED"));
    }

    @Test
    @DisplayName("10. Unauthorized recruiter cannot make final decision for another recruiter's job")
    void testSelectCandidate_UnauthorizedRecruiter_ThrowsException() {
        when(jobApplicationRepository.findById(100L)).thenReturn(Optional.of(jobApplication));
        when(userResolutionService.resolveUser("other_recruiter")).thenReturn(otherRecruiterUser);

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                finalCandidateSelectionService.selectCandidate(100L, "other_recruiter"));
        assertTrue(ex.getMessage().contains("not authorized"));
    }

    @Test
    @DisplayName("11. Student cannot perform final candidate selection action")
    void testSelectCandidate_StudentUser_ThrowsException() {
        when(jobApplicationRepository.findById(100L)).thenReturn(Optional.of(jobApplication));
        when(userResolutionService.resolveUser("student")).thenReturn(studentUser);

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                finalCandidateSelectionService.selectCandidate(100L, "student"));
        assertTrue(ex.getMessage().contains("Only recruiters"));
    }

    @Test
    @DisplayName("12. Student can view own application decision")
    void testGetDecision_StudentOwner_Success() {
        when(jobApplicationRepository.findById(100L)).thenReturn(Optional.of(jobApplication));
        when(userResolutionService.resolveUser("student")).thenReturn(studentUser);
        when(interviewRepository.findByJobApplicationId(100L))
                .thenReturn(List.of(completedInterview));

        FinalCandidateDecisionDto dto = finalCandidateSelectionService.getDecision(100L, "student");

        assertNotNull(dto);
        assertEquals(ApplicationDecision.PENDING, dto.getFinalDecision());
        assertEquals(InterviewStatus.COMPLETED, dto.getInterviewStatus());
        assertEquals(InterviewResult.SELECTED, dto.getInterviewResult());
    }

    @Test
    @DisplayName("13. Student cannot view another student's application decision")
    void testGetDecision_OtherStudent_ThrowsException() {
        User anotherStudent = User.builder().id(99L).username("another_student").role("ROLE_STUDENT").build();
        when(jobApplicationRepository.findById(100L)).thenReturn(Optional.of(jobApplication));
        when(userResolutionService.resolveUser("another_student")).thenReturn(anotherStudent);

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                finalCandidateSelectionService.getDecision(100L, "another_student"));
        assertTrue(ex.getMessage().contains("not authorized"));
    }
}
