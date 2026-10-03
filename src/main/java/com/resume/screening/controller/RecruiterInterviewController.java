package com.resume.screening.controller;

import com.resume.screening.dto.CreateInterviewRequestDto;
import com.resume.screening.dto.InterviewDto;
import com.resume.screening.dto.InterviewFeedbackRequestDto;
import com.resume.screening.entity.JobApplication;
import com.resume.screening.entity.User;
import com.resume.screening.repository.JobApplicationRepository;
import com.resume.screening.service.InterviewService;
import com.resume.screening.service.UserResolutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import com.resume.screening.service.FinalCandidateSelectionService;

@Controller
@RequestMapping("/recruiter/interviews")
public class RecruiterInterviewController {

    private static final Logger log = LoggerFactory.getLogger(RecruiterInterviewController.class);

    private final InterviewService interviewService;
    private final UserResolutionService userResolutionService;
    private final JobApplicationRepository jobApplicationRepository;
    private final FinalCandidateSelectionService finalCandidateSelectionService;

    public RecruiterInterviewController(InterviewService interviewService,
                                        UserResolutionService userResolutionService,
                                        JobApplicationRepository jobApplicationRepository,
                                        FinalCandidateSelectionService finalCandidateSelectionService) {
        this.interviewService = interviewService;
        this.userResolutionService = userResolutionService;
        this.jobApplicationRepository = jobApplicationRepository;
        this.finalCandidateSelectionService = finalCandidateSelectionService;
    }

    private User resolveAuthenticatedUser(Authentication authentication) {
        return userResolutionService.resolveAuthenticatedUser(authentication);
    }

    // 1. List Recruiter Interviews
    @GetMapping
    public String listInterviews(Model model, Authentication authentication) {
        User user = resolveAuthenticatedUser(authentication);
        List<InterviewDto> interviews = interviewService.getRecruiterInterviews(user.getUsername());
        model.addAttribute("interviews", interviews);
        String fullName = (user.getFirstName() != null ? user.getFirstName() : "") + " " + (user.getLastName() != null ? user.getLastName() : "");
        fullName = fullName.trim();
        if (fullName.isEmpty()) fullName = user.getUsername();
        model.addAttribute("fullName", fullName);
        return "recruiter/interviews";
    }

    // 2. Show Schedule Interview Form
    @GetMapping("/schedule")
    @Transactional(readOnly = true)
    public String showScheduleForm(@RequestParam(value = "applicationId", required = false) Long applicationId,
                                   @RequestParam(value = "jobId", required = false) Long jobId,
                                   @RequestParam(value = "studentProfileId", required = false) Long studentProfileId,
                                   Model model,
                                   Authentication authentication,
                                   RedirectAttributes redirectAttributes) {
        User user = resolveAuthenticatedUser(authentication);
        CreateInterviewRequestDto dto = new CreateInterviewRequestDto();

        JobApplication jobApplication = null;
        if (applicationId != null) {
            jobApplication = jobApplicationRepository.findById(applicationId).orElse(null);
        } else if (jobId != null && studentProfileId != null) {
            jobApplication = jobApplicationRepository.findByJobPostIdAndStudentProfileId(jobId, studentProfileId).orElse(null);
        }

        if (jobApplication != null) {
            if (!jobApplication.getJobPost().getRecruiter().getUser().getId().equals(user.getId())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Unauthorized access: You do not own this job posting.");
                return "redirect:/recruiter/interviews";
            }

            dto.setJobApplicationId(jobApplication.getId());

            String candidateName = "Student";
            if (jobApplication.getStudentProfile() != null && jobApplication.getStudentProfile().getUser() != null) {
                User sUser = jobApplication.getStudentProfile().getUser();
                candidateName = (sUser.getFirstName() != null ? sUser.getFirstName() : "") + " " +
                                (sUser.getLastName() != null ? sUser.getLastName() : "");
                candidateName = candidateName.trim();
                if (candidateName.isEmpty()) candidateName = sUser.getUsername();
            }

            model.addAttribute("candidateName", candidateName);
            model.addAttribute("candidateEmail", jobApplication.getStudentProfile() != null && jobApplication.getStudentProfile().getUser() != null ? jobApplication.getStudentProfile().getUser().getEmail() : "");
            model.addAttribute("jobTitle", jobApplication.getJobPost().getTitle());
            model.addAttribute("companyName", jobApplication.getJobPost().getCompanyName());
            model.addAttribute("jobApplication", jobApplication);
        }

        model.addAttribute("interviewRequest", dto);
        String fullName = (user.getFirstName() != null ? user.getFirstName() : "") + " " + (user.getLastName() != null ? user.getLastName() : "");
        fullName = fullName.trim();
        if (fullName.isEmpty()) fullName = user.getUsername();
        model.addAttribute("fullName", fullName);
        return "recruiter/interview-schedule";
    }

    // 3. Post Schedule Interview
    @PostMapping("/schedule")
    public String scheduleInterview(@ModelAttribute("interviewRequest") CreateInterviewRequestDto dto,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
        try {
            User user = resolveAuthenticatedUser(authentication);
            InterviewDto created = interviewService.createInterview(dto, user.getUsername());
            redirectAttributes.addFlashAttribute("successMessage", "Interview scheduled successfully.");
            return "redirect:/recruiter/interviews";
        } catch (Exception ex) {
            log.error("Failed to schedule interview: ", ex);
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/recruiter/interviews";
        }
    }

    // 4. View Specific Interview Details
    @GetMapping("/{id}")
    public String viewInterviewDetails(@PathVariable("id") Long id,
                                       Model model,
                                       Authentication authentication,
                                       RedirectAttributes redirectAttributes) {
        try {
            User user = resolveAuthenticatedUser(authentication);
            InterviewDto interview = interviewService.getInterviewById(id, user.getUsername());
            model.addAttribute("interview", interview);

            CreateInterviewRequestDto editDto = CreateInterviewRequestDto.builder()
                    .jobApplicationId(interview.getJobApplicationId())
                    .roundName(interview.getRoundName())
                    .interviewDate(interview.getInterviewDate())
                    .interviewTime(interview.getInterviewTime())
                    .interviewType(interview.getInterviewType())
                    .meetingLink(interview.getMeetingLink())
                    .location(interview.getLocation())
                    .status(interview.getStatus())
                    .notes(interview.getNotes())
                    .build();
            model.addAttribute("interviewRequest", editDto);

            InterviewFeedbackRequestDto feedbackDto = InterviewFeedbackRequestDto.builder()
                    .rating(interview.getRating())
                    .result(interview.getResult())
                    .feedback(interview.getFeedback())
                    .build();
            model.addAttribute("feedbackRequest", feedbackDto);

            String fullName = (user.getFirstName() != null ? user.getFirstName() : "") + " " + (user.getLastName() != null ? user.getLastName() : "");
            fullName = fullName.trim();
            if (fullName.isEmpty()) fullName = user.getUsername();
            model.addAttribute("fullName", fullName);

            return "recruiter/interview-details";
        } catch (Exception ex) {
            log.error("Error retrieving interview details: ", ex);
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/recruiter/interviews";
        }
    }

    // 5. Update Interview
    @PostMapping("/{id}/update")
    public String updateInterview(@PathVariable("id") Long id,
                                  @ModelAttribute("interviewRequest") CreateInterviewRequestDto dto,
                                  Authentication authentication,
                                  RedirectAttributes redirectAttributes) {
        try {
            User user = resolveAuthenticatedUser(authentication);
            InterviewDto updated = interviewService.updateInterview(id, dto, user.getUsername());
            redirectAttributes.addFlashAttribute("successMessage", "Interview updated successfully (Status: " + updated.getStatus() + ").");
            return "redirect:/recruiter/interviews/" + id;
        } catch (Exception ex) {
            log.error("Failed to update interview: ", ex);
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/recruiter/interviews/" + id;
        }
    }

    // 6. Cancel Interview
    @PostMapping("/{id}/cancel")
    public String cancelInterview(@PathVariable("id") Long id,
                                  Authentication authentication,
                                  RedirectAttributes redirectAttributes) {
        try {
            User user = resolveAuthenticatedUser(authentication);
            interviewService.cancelInterview(id, user.getUsername());
            redirectAttributes.addFlashAttribute("successMessage", "Interview cancelled successfully.");
            return "redirect:/recruiter/interviews";
        } catch (Exception ex) {
            log.error("Failed to cancel interview: ", ex);
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/recruiter/interviews";
        }
    }

    // 7. Complete Interview
    @PostMapping("/{id}/complete")
    public String completeInterview(@PathVariable("id") Long id,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
        try {
            User user = resolveAuthenticatedUser(authentication);
            interviewService.completeInterview(id, user.getUsername());
            redirectAttributes.addFlashAttribute("successMessage", "Interview marked as COMPLETED successfully.");
            return "redirect:/recruiter/interviews/" + id;
        } catch (Exception ex) {
            log.error("Failed to complete interview: ", ex);
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/recruiter/interviews/" + id;
        }
    }

    // 8. Submit Feedback & Result
    @PostMapping("/{id}/feedback")
    public String submitFeedback(@PathVariable("id") Long id,
                                 @ModelAttribute("feedbackRequest") InterviewFeedbackRequestDto dto,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        try {
            User user = resolveAuthenticatedUser(authentication);
            interviewService.submitFeedback(id, dto, user.getUsername());
            redirectAttributes.addFlashAttribute("successMessage", "Interview feedback and result recorded successfully.");
            return "redirect:/recruiter/interviews/" + id;
        } catch (Exception ex) {
            log.error("Failed to submit interview feedback: ", ex);
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/recruiter/interviews/" + id;
        }
    }

    // 9. Final Candidate Selection (Select Candidate)
    @PostMapping("/{id}/select-candidate")
    public String selectCandidate(@PathVariable("id") Long id,
                                  Authentication authentication,
                                  RedirectAttributes redirectAttributes) {
        try {
            User user = resolveAuthenticatedUser(authentication);
            InterviewDto interview = interviewService.getInterviewById(id, user.getUsername());
            finalCandidateSelectionService.selectCandidate(interview.getJobApplicationId(), user.getUsername());
            redirectAttributes.addFlashAttribute("successMessage", "Candidate successfully SELECTED for final hiring decision!");
            return "redirect:/recruiter/interviews/" + id;
        } catch (Exception ex) {
            log.error("Failed to select candidate: ", ex);
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/recruiter/interviews/" + id;
        }
    }

    // 10. Final Candidate Selection (Reject Candidate)
    @PostMapping("/{id}/reject-candidate")
    public String rejectCandidate(@PathVariable("id") Long id,
                                  Authentication authentication,
                                  RedirectAttributes redirectAttributes) {
        try {
            User user = resolveAuthenticatedUser(authentication);
            InterviewDto interview = interviewService.getInterviewById(id, user.getUsername());
            finalCandidateSelectionService.rejectCandidate(interview.getJobApplicationId(), user.getUsername());
            redirectAttributes.addFlashAttribute("successMessage", "Candidate decision set to REJECTED.");
            return "redirect:/recruiter/interviews/" + id;
        } catch (Exception ex) {
            log.error("Failed to reject candidate: ", ex);
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/recruiter/interviews/" + id;
        }
    }
}
