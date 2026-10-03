package com.resume.screening.service.impl;

import com.resume.screening.dto.CreateInterviewRequestDto;
import com.resume.screening.dto.InterviewDto;
import com.resume.screening.dto.InterviewFeedbackRequestDto;
import com.resume.screening.entity.*;
import com.resume.screening.repository.*;
import com.resume.screening.service.InterviewService;
import com.resume.screening.service.UserResolutionService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class InterviewServiceImpl implements InterviewService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);

    private final InterviewRepository interviewRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final RecruiterRepository recruiterRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final ShortlistedCandidateRepository shortlistedCandidateRepository;
    private final UserResolutionService userResolutionService;

    public InterviewServiceImpl(InterviewRepository interviewRepository,
                                JobApplicationRepository jobApplicationRepository,
                                RecruiterRepository recruiterRepository,
                                StudentProfileRepository studentProfileRepository,
                                ShortlistedCandidateRepository shortlistedCandidateRepository,
                                UserResolutionService userResolutionService) {
        this.interviewRepository = interviewRepository;
        this.jobApplicationRepository = jobApplicationRepository;
        this.recruiterRepository = recruiterRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.shortlistedCandidateRepository = shortlistedCandidateRepository;
        this.userResolutionService = userResolutionService;
    }

    @Override
    @Transactional
    public InterviewDto createInterview(CreateInterviewRequestDto requestDto, String recruiterUsername) {
        if (requestDto == null) {
            throw new IllegalArgumentException("Interview request cannot be null.");
        }
        if (requestDto.getJobApplicationId() == null) {
            throw new IllegalArgumentException("Job application ID is required.");
        }

        User recruiterUser = userResolutionService.resolveUser(recruiterUsername);
        Recruiter recruiter = recruiterRepository.findByUserId(recruiterUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Recruiter profile not found for user: " + recruiterUsername));

        JobApplication jobApplication = jobApplicationRepository.findById(requestDto.getJobApplicationId())
                .orElseThrow(() -> new IllegalArgumentException("Job application not found with ID: " + requestDto.getJobApplicationId()));

        // Security check: Recruiter must own the job posting
        if (!jobApplication.getJobPost().getRecruiter().getId().equals(recruiter.getId())) {
            throw new AccessDeniedException("Unauthorized access: You do not own this job posting.");
        }

        // Rule: Only shortlisted candidates can receive an interview
        boolean isShortlisted = "SHORTLISTED".equalsIgnoreCase(jobApplication.getStatus()) ||
                shortlistedCandidateRepository.existsByJobPostIdAndStudentProfileId(
                        jobApplication.getJobPost().getId(),
                        jobApplication.getStudentProfile().getId()
                );
        if (!isShortlisted) {
            throw new IllegalArgumentException("Cannot schedule interview: Candidate is not shortlisted for this job.");
        }

        // Duplicate active interview check (STEP 5)
        List<InterviewStatus> activeStatuses = List.of(InterviewStatus.SCHEDULED, InterviewStatus.RESCHEDULED);
        boolean activeExists = interviewRepository.existsByJobApplicationIdAndStatusIn(jobApplication.getId(), activeStatuses);
        if (activeExists) {
            throw new IllegalArgumentException("An interview is already scheduled for this candidate.");
        }

        // Required fields validation
        if (!StringUtils.hasText(requestDto.getRoundName())) {
            throw new IllegalArgumentException("Round name is required.");
        }
        if (requestDto.getInterviewDate() == null) {
            throw new IllegalArgumentException("Interview date is required.");
        }
        if (requestDto.getInterviewTime() == null) {
            throw new IllegalArgumentException("Interview time is required.");
        }
        if (requestDto.getInterviewType() == null) {
            throw new IllegalArgumentException("Interview type is required.");
        }

        // Type specific validation
        if (requestDto.getInterviewType() == InterviewType.ONLINE && !StringUtils.hasText(requestDto.getMeetingLink())) {
            throw new IllegalArgumentException("Meeting link is required for ONLINE interviews.");
        }
        if (requestDto.getInterviewType() == InterviewType.OFFLINE && !StringUtils.hasText(requestDto.getLocation())) {
            throw new IllegalArgumentException("Location is required for OFFLINE interviews.");
        }

        InterviewStatus initialStatus = requestDto.getStatus() != null ? requestDto.getStatus() : InterviewStatus.SCHEDULED;

        Interview interview = Interview.builder()
                .jobApplication(jobApplication)
                .recruiter(recruiter)
                .student(jobApplication.getStudentProfile())
                .roundName(requestDto.getRoundName().trim())
                .interviewDate(requestDto.getInterviewDate())
                .interviewTime(requestDto.getInterviewTime())
                .interviewType(requestDto.getInterviewType())
                .meetingLink(requestDto.getMeetingLink())
                .location(requestDto.getLocation())
                .status(initialStatus)
                .notes(requestDto.getNotes())
                .result(InterviewResult.PENDING)
                .build();

        Interview saved = interviewRepository.save(interview);
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public InterviewDto getInterviewById(Long id, String username) {
        Interview interview = interviewRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Interview not found with ID: " + id));

        User user = userResolutionService.resolveUser(username);
        boolean isRecruiterOwner = interview.getRecruiter().getUser().getId().equals(user.getId());
        boolean isStudentOwner = interview.getStudent().getUser().getId().equals(user.getId());
        boolean isAdmin = "ADMIN".equalsIgnoreCase(user.getRole());

        if (!isRecruiterOwner && !isStudentOwner && !isAdmin) {
            throw new AccessDeniedException("Unauthorized access: You cannot view this interview.");
        }

        return mapToDto(interview);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterviewDto> getRecruiterInterviews(String recruiterUsername) {
        List<Interview> interviews = interviewRepository.findByRecruiter_User_Username(recruiterUsername);
        return interviews.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterviewDto> getStudentInterviews(String studentUsername) {
        List<Interview> interviews = interviewRepository.findByStudent_User_Username(studentUsername);
        return interviews.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterviewDto> getInterviewsForApplication(Long applicationId, String recruiterUsername) {
        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Job application not found with ID: " + applicationId));

        User recruiterUser = userResolutionService.resolveUser(recruiterUsername);
        if (!application.getJobPost().getRecruiter().getUser().getId().equals(recruiterUser.getId())) {
            throw new AccessDeniedException("Unauthorized access: You do not own this job posting.");
        }

        List<Interview> interviews = interviewRepository.findByJobApplicationId(applicationId);
        return interviews.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public InterviewDto updateInterview(Long id, CreateInterviewRequestDto requestDto, String recruiterUsername) {
        Interview interview = interviewRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Interview not found with ID: " + id));

        User recruiterUser = userResolutionService.resolveUser(recruiterUsername);
        if (!interview.getRecruiter().getUser().getId().equals(recruiterUser.getId())) {
            throw new AccessDeniedException("Unauthorized access: You do not own this interview.");
        }

        if (requestDto.getRoundName() != null) {
            if (!StringUtils.hasText(requestDto.getRoundName())) {
                throw new IllegalArgumentException("Round name cannot be empty.");
            }
            interview.setRoundName(requestDto.getRoundName().trim());
        }

        if (requestDto.getInterviewDate() != null) {
            interview.setInterviewDate(requestDto.getInterviewDate());
        }
        if (requestDto.getInterviewTime() != null) {
            interview.setInterviewTime(requestDto.getInterviewTime());
        }
        if (requestDto.getInterviewType() != null) {
            interview.setInterviewType(requestDto.getInterviewType());
        }

        InterviewType type = interview.getInterviewType();
        String meetingLink = requestDto.getMeetingLink() != null ? requestDto.getMeetingLink() : interview.getMeetingLink();
        String location = requestDto.getLocation() != null ? requestDto.getLocation() : interview.getLocation();

        if (type == InterviewType.ONLINE && !StringUtils.hasText(meetingLink)) {
            throw new IllegalArgumentException("Meeting link is required for ONLINE interviews.");
        }
        if (type == InterviewType.OFFLINE && !StringUtils.hasText(location)) {
            throw new IllegalArgumentException("Location is required for OFFLINE interviews.");
        }

        interview.setMeetingLink(meetingLink);
        interview.setLocation(location);

        if (requestDto.getStatus() != null) {
            interview.setStatus(requestDto.getStatus());
        }
        if (requestDto.getNotes() != null) {
            interview.setNotes(requestDto.getNotes());
        }

        Interview updated = interviewRepository.save(interview);
        return mapToDto(updated);
    }

    @Override
    @Transactional
    public InterviewDto cancelInterview(Long id, String recruiterUsername) {
        Interview interview = interviewRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Interview not found with ID: " + id));

        User recruiterUser = userResolutionService.resolveUser(recruiterUsername);
        if (!interview.getRecruiter().getUser().getId().equals(recruiterUser.getId())) {
            throw new AccessDeniedException("Unauthorized access: You do not own this interview.");
        }

        interview.setStatus(InterviewStatus.CANCELLED);
        Interview saved = interviewRepository.save(interview);
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public InterviewDto completeInterview(Long id, String recruiterUsername) {
        Interview interview = interviewRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Interview not found with ID: " + id));

        User recruiterUser = userResolutionService.resolveUser(recruiterUsername);
        if (!interview.getRecruiter().getUser().getId().equals(recruiterUser.getId())) {
            throw new AccessDeniedException("Unauthorized access: You do not own this interview.");
        }

        if (interview.getStatus() == InterviewStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot complete a cancelled interview.");
        }

        if (interview.getStatus() == InterviewStatus.COMPLETED) {
            return mapToDto(interview);
        }

        interview.setStatus(InterviewStatus.COMPLETED);
        Interview saved = interviewRepository.save(interview);
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public InterviewDto submitFeedback(Long id, InterviewFeedbackRequestDto feedbackDto, String recruiterUsername) {
        Interview interview = interviewRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Interview not found with ID: " + id));

        User recruiterUser = userResolutionService.resolveUser(recruiterUsername);
        if (!interview.getRecruiter().getUser().getId().equals(recruiterUser.getId())) {
            throw new AccessDeniedException("Unauthorized access: You do not own this interview.");
        }

        if (interview.getStatus() != InterviewStatus.COMPLETED) {
            throw new IllegalArgumentException("Feedback can only be submitted for completed interviews.");
        }

        if (feedbackDto != null) {
            if (feedbackDto.getRating() != null) {
                if (feedbackDto.getRating() < 1 || feedbackDto.getRating() > 5) {
                    throw new IllegalArgumentException("Rating must be between 1 and 5.");
                }
                interview.setRating(feedbackDto.getRating());
            }

            if (feedbackDto.getResult() != null) {
                interview.setResult(feedbackDto.getResult());
            } else if (interview.getResult() == null) {
                interview.setResult(InterviewResult.PENDING);
            }

            if (feedbackDto.getFeedback() != null) {
                interview.setFeedback(feedbackDto.getFeedback().trim());
            }
        }

        Interview saved = interviewRepository.save(interview);
        return mapToDto(saved);
    }

    private InterviewDto mapToDto(Interview interview) {
        if (interview == null) return null;

        String recruiterName = interview.getRecruiter() != null && interview.getRecruiter().getUser() != null ?
                interview.getRecruiter().getUser().getFirstName() + " " + interview.getRecruiter().getUser().getLastName() : "";

        String studentName = interview.getStudent() != null && interview.getStudent().getUser() != null ?
                interview.getStudent().getUser().getFirstName() + " " + interview.getStudent().getUser().getLastName() : "";

        String studentEmail = interview.getStudent() != null && interview.getStudent().getUser() != null ?
                interview.getStudent().getUser().getEmail() : "";

        String formattedDate = interview.getInterviewDate() != null ? interview.getInterviewDate().format(DATE_FORMATTER) : "";
        String formattedTime = interview.getInterviewTime() != null ? interview.getInterviewTime().format(TIME_FORMATTER) : "";

        InterviewResult result = interview.getResult() != null ? interview.getResult() : InterviewResult.PENDING;

        return InterviewDto.builder()
                .id(interview.getId())
                .jobApplicationId(interview.getJobApplication() != null ? interview.getJobApplication().getId() : null)
                .jobPostId(interview.getJobApplication() != null && interview.getJobApplication().getJobPost() != null ?
                        interview.getJobApplication().getJobPost().getId() : null)
                .jobTitle(interview.getJobApplication() != null && interview.getJobApplication().getJobPost() != null ?
                        interview.getJobApplication().getJobPost().getTitle() : "")
                .companyName(interview.getRecruiter() != null ? interview.getRecruiter().getCompanyName() : "")
                .recruiterId(interview.getRecruiter() != null ? interview.getRecruiter().getId() : null)
                .recruiterName(recruiterName.trim())
                .studentProfileId(interview.getStudent() != null ? interview.getStudent().getId() : null)
                .studentName(studentName.trim())
                .studentEmail(studentEmail)
                .roundName(interview.getRoundName())
                .interviewDate(interview.getInterviewDate())
                .interviewTime(interview.getInterviewTime())
                .interviewType(interview.getInterviewType())
                .meetingLink(interview.getMeetingLink())
                .location(interview.getLocation())
                .status(interview.getStatus())
                .notes(interview.getNotes())
                .feedback(interview.getFeedback())
                .rating(interview.getRating())
                .result(result)
                .finalDecision(interview.getJobApplication() != null ? interview.getJobApplication().getFinalDecision() : null)
                .decidedAt(interview.getJobApplication() != null ? interview.getJobApplication().getDecidedAt() : null)
                .createdAt(interview.getCreatedAt())
                .updatedAt(interview.getUpdatedAt())
                .formattedInterviewDate(formattedDate)
                .formattedInterviewTime(formattedTime)
                .build();
    }
}
