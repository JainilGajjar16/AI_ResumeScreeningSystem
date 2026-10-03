package com.resume.screening.service.impl;

import com.resume.screening.dto.FinalCandidateDecisionDto;
import com.resume.screening.entity.*;
import com.resume.screening.enums.ApplicationDecision;
import com.resume.screening.repository.InterviewRepository;
import com.resume.screening.repository.JobApplicationRepository;
import com.resume.screening.service.FinalCandidateSelectionService;
import com.resume.screening.service.UserResolutionService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class FinalCandidateSelectionServiceImpl implements FinalCandidateSelectionService {

    private final JobApplicationRepository jobApplicationRepository;
    private final InterviewRepository interviewRepository;
    private final UserResolutionService userResolutionService;

    public FinalCandidateSelectionServiceImpl(JobApplicationRepository jobApplicationRepository,
                                                InterviewRepository interviewRepository,
                                                UserResolutionService userResolutionService) {
        this.jobApplicationRepository = jobApplicationRepository;
        this.interviewRepository = interviewRepository;
        this.userResolutionService = userResolutionService;
    }

    @Override
    public FinalCandidateDecisionDto selectCandidate(Long applicationId, String recruiterUsername) {
        JobApplication application = validateAndGetApplication(applicationId, recruiterUsername);
        Interview interview = validateInterviewCompletionAndResult(applicationId);

        if (interview.getResult() != InterviewResult.SELECTED) {
            throw new IllegalStateException("Candidate interview result must be SELECTED before final selection.");
        }

        application.setFinalDecision(ApplicationDecision.SELECTED);
        application.setDecidedAt(LocalDateTime.now());
        JobApplication saved = jobApplicationRepository.save(application);

        return mapToDto(saved, interview);
    }

    @Override
    public FinalCandidateDecisionDto rejectCandidate(Long applicationId, String recruiterUsername) {
        JobApplication application = validateAndGetApplication(applicationId, recruiterUsername);
        Interview interview = validateInterviewCompletionAndResult(applicationId);

        application.setFinalDecision(ApplicationDecision.REJECTED);
        application.setDecidedAt(LocalDateTime.now());
        JobApplication saved = jobApplicationRepository.save(application);

        return mapToDto(saved, interview);
    }

    @Override
    @Transactional(readOnly = true)
    public FinalCandidateDecisionDto getDecision(Long applicationId, String username) {
        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Job application not found with ID: " + applicationId));

        User user = userResolutionService.resolveUser(username);
        boolean isRecruiterOwner = application.getJobPost().getRecruiter() != null &&
                application.getJobPost().getRecruiter().getUser().getId().equals(user.getId());
        boolean isStudentOwner = application.getStudentProfile() != null &&
                application.getStudentProfile().getUser().getId().equals(user.getId());

        boolean isAdmin = user.getRole() != null && (user.getRole().equalsIgnoreCase("ROLE_ADMIN") || user.getRole().equalsIgnoreCase("ADMIN"));

        if (!isRecruiterOwner && !isStudentOwner && !isAdmin) {
            throw new AccessDeniedException("You are not authorized to view the decision for this application.");
        }

        List<Interview> interviews = interviewRepository.findByJobApplicationId(applicationId);
        Interview latestInterview = interviews.isEmpty() ? null : interviews.get(interviews.size() - 1);

        return mapToDto(application, latestInterview);
    }

    private JobApplication validateAndGetApplication(Long applicationId, String recruiterUsername) {
        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Job application not found with ID: " + applicationId));

        User recruiterUser = userResolutionService.resolveUser(recruiterUsername);
        String role = recruiterUser.getRole() != null ? recruiterUser.getRole() : "";
        boolean isRecruiterOrAdmin = role.equalsIgnoreCase("ROLE_RECRUITER") || role.equalsIgnoreCase("RECRUITER") ||
                                     role.equalsIgnoreCase("ROLE_ADMIN") || role.equalsIgnoreCase("ADMIN");

        if (!isRecruiterOrAdmin) {
            throw new AccessDeniedException("Only recruiters are authorized to make final hiring decisions.");
        }

        if (application.getJobPost().getRecruiter() == null ||
                !application.getJobPost().getRecruiter().getUser().getId().equals(recruiterUser.getId())) {
            throw new AccessDeniedException("You are not authorized to make a decision for this application.");
        }

        return application;
    }

    private Interview validateInterviewCompletionAndResult(Long applicationId) {
        List<Interview> interviews = interviewRepository.findByJobApplicationId(applicationId);

        if (interviews.isEmpty()) {
            throw new IllegalStateException("Candidate interview must be completed before final selection.");
        }

        // Find the completed interview
        Interview completedInterview = null;
        for (Interview interview : interviews) {
            if (interview.getStatus() == InterviewStatus.COMPLETED) {
                completedInterview = interview;
                break;
            }
        }

        if (completedInterview == null) {
            throw new IllegalStateException("Candidate interview must be completed before final selection.");
        }

        return completedInterview;
    }

    private FinalCandidateDecisionDto mapToDto(JobApplication application, Interview interview) {
        String candidateName = "Candidate";
        String candidateEmail = "";
        if (application.getStudentProfile() != null && application.getStudentProfile().getUser() != null) {
            User sUser = application.getStudentProfile().getUser();
            candidateName = (sUser.getFirstName() != null ? sUser.getFirstName() : "") + " " +
                            (sUser.getLastName() != null ? sUser.getLastName() : "");
            candidateName = candidateName.trim();
            if (candidateName.isEmpty()) candidateName = sUser.getUsername();
            candidateEmail = sUser.getEmail();
        }

        Double atsScoreVal = null;
        String formattedAtsScoreVal = null;
        if (application.getAtsScore() != null && application.getAtsScore().getOverallScore() != null) {
            atsScoreVal = application.getAtsScore().getOverallScore().doubleValue();
            formattedAtsScoreVal = String.format("%.1f%%", atsScoreVal);
        }

        return FinalCandidateDecisionDto.builder()
                .applicationId(application.getId())
                .studentProfileId(application.getStudentProfile() != null ? application.getStudentProfile().getId() : null)
                .candidateName(candidateName)
                .candidateEmail(candidateEmail)
                .jobPostId(application.getJobPost() != null ? application.getJobPost().getId() : null)
                .jobTitle(application.getJobPost() != null ? application.getJobPost().getTitle() : null)
                .companyName(application.getJobPost() != null ? application.getJobPost().getCompanyName() : null)
                .atsScore(atsScoreVal)
                .formattedAtsScore(formattedAtsScoreVal)
                .interviewStatus(interview != null ? interview.getStatus() : null)
                .interviewResult(interview != null ? interview.getResult() : null)
                .finalDecision(application.getFinalDecision() != null ? application.getFinalDecision() : ApplicationDecision.PENDING)
                .decidedAt(application.getDecidedAt())
                .build();
    }
}
