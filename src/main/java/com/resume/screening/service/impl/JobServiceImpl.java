package com.resume.screening.service.impl;

import com.resume.screening.dto.JobPostDto;
import com.resume.screening.dto.RecruiterDashboardDto;
import com.resume.screening.entity.JobPost;
import com.resume.screening.entity.Recruiter;
import com.resume.screening.entity.User;
import com.resume.screening.repository.JobApplicationRepository;
import com.resume.screening.repository.JobPostRepository;
import com.resume.screening.repository.RecruiterRepository;
import com.resume.screening.repository.UserRepository;
import com.resume.screening.service.JobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class JobServiceImpl implements JobService {

    private static final Logger log = LoggerFactory.getLogger(JobServiceImpl.class);

    private final JobPostRepository jobPostRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final RecruiterRepository recruiterRepository;
    private final UserRepository userRepository;
    private final com.resume.screening.service.UserResolutionService userResolutionService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public JobServiceImpl(JobPostRepository jobPostRepository,
                          JobApplicationRepository jobApplicationRepository,
                          RecruiterRepository recruiterRepository,
                          UserRepository userRepository) {
        this(jobPostRepository, jobApplicationRepository, recruiterRepository, userRepository, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public JobServiceImpl(JobPostRepository jobPostRepository,
                          JobApplicationRepository jobApplicationRepository,
                          RecruiterRepository recruiterRepository,
                          UserRepository userRepository,
                          com.resume.screening.service.UserResolutionService userResolutionService) {
        this.jobPostRepository = jobPostRepository;
        this.jobApplicationRepository = jobApplicationRepository;
        this.recruiterRepository = recruiterRepository;
        this.userRepository = userRepository;
        this.userResolutionService = userResolutionService != null ? userResolutionService :
                new com.resume.screening.service.impl.UserResolutionServiceImpl(userRepository, null, recruiterRepository);
    }

    @Override
    @Transactional
    public JobPostDto createJob(JobPostDto dto, String recruiterUsername) {
        Recruiter recruiter = getRecruiterByUsername(recruiterUsername);
        validateJobDto(dto);

        String companyName = StringUtils.hasText(dto.getCompanyName()) 
                ? dto.getCompanyName().trim() 
                : recruiter.getCompanyName();

        String status = StringUtils.hasText(dto.getStatus()) ? dto.getStatus().toUpperCase() : "DRAFT";
        if (!"DRAFT".equals(status) && !"PUBLISHED".equals(status) && !"CLOSED".equals(status)) {
            status = "DRAFT";
        }

        JobPost jobPost = JobPost.builder()
                .recruiter(recruiter)
                .companyName(companyName)
                .title(dto.getTitle().trim())
                .description(dto.getDescription().trim())
                .requiredSkills(dto.getRequiredSkills().trim())
                .requirements(dto.getRequiredSkills().trim())
                .qualification(dto.getQualification().trim())
                .experienceRequired(StringUtils.hasText(dto.getExperienceRequired()) ? dto.getExperienceRequired().trim() : "Not specified")
                .location(dto.getLocation().trim())
                .jobType(dto.getJobType().trim())
                .salaryRange(StringUtils.hasText(dto.getSalaryRange()) ? dto.getSalaryRange().trim() : "Not disclosed")
                .deadline(dto.getDeadline())
                .status(status)
                .build();

        JobPost saved = jobPostRepository.save(jobPost);
        log.info("Created job ID {} with status {}", saved.getId(), saved.getStatus());

        // Also update recruiter's company name if not set
        if (!StringUtils.hasText(recruiter.getCompanyName()) && StringUtils.hasText(companyName)) {
            recruiter.setCompanyName(companyName);
            recruiterRepository.save(recruiter);
        }

        return mapToDto(saved);
    }

    @Override
    @Transactional
    public JobPostDto updateJob(Long jobId, JobPostDto dto, String recruiterUsername) {
        JobPost jobPost = getJobEntityWithOwnershipCheck(jobId, recruiterUsername);
        validateJobDto(dto);

        jobPost.setTitle(dto.getTitle().trim());
        if (StringUtils.hasText(dto.getCompanyName())) {
            jobPost.setCompanyName(dto.getCompanyName().trim());
        }
        jobPost.setDescription(dto.getDescription().trim());
        jobPost.setRequiredSkills(dto.getRequiredSkills().trim());
        jobPost.setRequirements(dto.getRequiredSkills().trim());
        jobPost.setQualification(dto.getQualification().trim());
        jobPost.setExperienceRequired(StringUtils.hasText(dto.getExperienceRequired()) ? dto.getExperienceRequired().trim() : "Not specified");
        jobPost.setLocation(dto.getLocation().trim());
        jobPost.setJobType(dto.getJobType().trim());
        jobPost.setSalaryRange(StringUtils.hasText(dto.getSalaryRange()) ? dto.getSalaryRange().trim() : "Not disclosed");
        jobPost.setDeadline(dto.getDeadline());

        if (StringUtils.hasText(dto.getStatus())) {
            String newStatus = dto.getStatus().toUpperCase();
            if ("DRAFT".equals(newStatus) || "PUBLISHED".equals(newStatus) || "CLOSED".equals(newStatus)) {
                jobPost.setStatus(newStatus);
            }
        }

        JobPost updated = jobPostRepository.save(jobPost);
        log.info("Updated job ID {} status to {}", updated.getId(), updated.getStatus());
        return mapToDto(updated);
    }

    @Override
    @Transactional
    public void publishJob(Long jobId, String recruiterUsername) {
        JobPost jobPost = getJobEntityWithOwnershipCheck(jobId, recruiterUsername);
        jobPost.setStatus("PUBLISHED");
        jobPostRepository.save(jobPost);
        log.info("Job ID {} published by recruiter {}", jobId, recruiterUsername);
    }

    @Override
    @Transactional
    public void closeJob(Long jobId, String recruiterUsername) {
        JobPost jobPost = getJobEntityWithOwnershipCheck(jobId, recruiterUsername);
        jobPost.setStatus("CLOSED");
        jobPostRepository.save(jobPost);
        log.info("Job ID {} closed by recruiter {}", jobId, recruiterUsername);
    }

    @Override
    @Transactional(readOnly = true)
    public JobPostDto getJobById(Long jobId) {
        JobPost jobPost = jobPostRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found with ID: " + jobId));
        return mapToDto(jobPost);
    }

    @Override
    @Transactional(readOnly = true)
    public JobPostDto getJobForRecruiter(Long jobId, String recruiterUsername) {
        JobPost jobPost = getJobEntityWithOwnershipCheck(jobId, recruiterUsername);
        return mapToDto(jobPost);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobPostDto> getJobsByRecruiter(String recruiterUsername) {
        Recruiter recruiter = getRecruiterByUsername(recruiterUsername);
        List<JobPost> jobs = jobPostRepository.findByRecruiterIdOrderByCreatedAtDesc(recruiter.getId());
        return jobs.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobPostDto> getPublishedJobsForStudents() {
        log.info("Available jobs request started");
        List<JobPost> allJobs = jobPostRepository.findAll();
        log.info("Total jobs fetched: {}", allJobs.size());

        List<JobPost> publishedJobs = allJobs.stream()
                .filter(job -> job.getStatus() != null && 
                        ("PUBLISHED".equalsIgnoreCase(job.getStatus()) || "ACTIVE".equalsIgnoreCase(job.getStatus())))
                .sorted((j1, j2) -> {
                    if (j1.getCreatedAt() == null || j2.getCreatedAt() == null) return 0;
                    return j2.getCreatedAt().compareTo(j1.getCreatedAt());
                })
                .collect(Collectors.toList());

        log.info("Published jobs found: {}", publishedJobs.size());
        for (JobPost job : publishedJobs) {
            log.info("Job ID: {}, Title: {}, Status: {}, Deadline: {}", job.getId(), job.getTitle(), job.getStatus(), job.getDeadline());
        }

        return publishedJobs.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public RecruiterDashboardDto getRecruiterDashboardStats(String recruiterUsername) {
        User user = findUser(recruiterUsername);
        Recruiter recruiter = getRecruiterByUsername(recruiterUsername);

        String fullName = (user.getFirstName() != null ? user.getFirstName() : "") + " " +
                          (user.getLastName() != null ? user.getLastName() : "");
        if (!StringUtils.hasText(fullName.trim())) {
            fullName = user.getUsername();
        }

        long totalJobs = jobPostRepository.countByRecruiterId(recruiter.getId());
        long activeJobs = jobPostRepository.countByRecruiterIdAndStatus(recruiter.getId(), "PUBLISHED");
        long draftJobs = jobPostRepository.countByRecruiterIdAndStatus(recruiter.getId(), "DRAFT");
        long closedJobs = jobPostRepository.countByRecruiterIdAndStatus(recruiter.getId(), "CLOSED");
        long totalApplications = jobApplicationRepository.countByJobPostRecruiterId(recruiter.getId());

        List<JobPost> recent = jobPostRepository.findByRecruiterIdOrderByCreatedAtDesc(recruiter.getId());
        List<JobPostDto> recentDtos = recent.stream().limit(5).map(this::mapToDto).collect(Collectors.toList());

        return RecruiterDashboardDto.builder()
                .recruiterName(fullName.trim())
                .companyName(StringUtils.hasText(recruiter.getCompanyName()) ? recruiter.getCompanyName() : "Not specified")
                .email(user.getEmail())
                .designation(StringUtils.hasText(recruiter.getDesignation()) ? recruiter.getDesignation() : "Recruiter")
                .totalJobs(totalJobs)
                .activeJobs(activeJobs)
                .draftJobs(draftJobs)
                .closedJobs(closedJobs)
                .totalApplications(totalApplications)
                .recentJobs(recentDtos)
                .build();
    }

    private void validateJobDto(JobPostDto dto) {
        if (!StringUtils.hasText(dto.getTitle())) {
            throw new IllegalArgumentException("Job Title is required.");
        }
        if (!StringUtils.hasText(dto.getCompanyName())) {
            throw new IllegalArgumentException("Company Name is required.");
        }
        if (!StringUtils.hasText(dto.getDescription())) {
            throw new IllegalArgumentException("Job Description is required.");
        }
        if (!StringUtils.hasText(dto.getRequiredSkills())) {
            throw new IllegalArgumentException("Required Skills are required.");
        }
        if (!StringUtils.hasText(dto.getQualification())) {
            throw new IllegalArgumentException("Qualification is required.");
        }
        if (!StringUtils.hasText(dto.getLocation())) {
            throw new IllegalArgumentException("Location is required.");
        }
        if (!StringUtils.hasText(dto.getJobType())) {
            throw new IllegalArgumentException("Employment Type is required.");
        }
        if (dto.getDeadline() == null) {
            throw new IllegalArgumentException("Valid Application Deadline is required.");
        }
    }

    private JobPost getJobEntityWithOwnershipCheck(Long jobId, String recruiterUsername) {
        JobPost jobPost = jobPostRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found with ID: " + jobId));

        Recruiter recruiter = getRecruiterByUsername(recruiterUsername);
        if (!jobPost.getRecruiter().getId().equals(recruiter.getId())) {
            throw new IllegalArgumentException("Unauthorized access: You do not own this job posting.");
        }
        return jobPost;
    }

    private Recruiter getRecruiterByUsername(String username) {
        User user = findUser(username);
        return recruiterRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Recruiter newRecruiter = Recruiter.builder()
                            .user(user)
                            .companyName("Default Company")
                            .build();
                    return recruiterRepository.save(newRecruiter);
                });
    }

    private User findUser(String usernameOrEmail) {
        if (userResolutionService != null) {
            return userResolutionService.resolveUser(usernameOrEmail);
        }
        if (usernameOrEmail == null || usernameOrEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("User identifier cannot be empty.");
        }
        String trimmed = usernameOrEmail.trim();
        return userRepository.findByUsernameIgnoreCase(trimmed)
                .or(() -> userRepository.findByEmailIgnoreCase(trimmed))
                .or(() -> userRepository.findByUsernameOrEmail(trimmed))
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + trimmed));
    }

    private JobPostDto mapToDto(JobPost jobPost) {
        long appCount = 0;
        try {
            appCount = jobApplicationRepository.countByJobPostId(jobPost.getId());
        } catch (Exception ex) {
            log.warn("Could not count applications for job ID {}: {}", jobPost.getId(), ex.getMessage());
        }

        String recruiterName = "Recruiter";
        if (jobPost.getRecruiter() != null && jobPost.getRecruiter().getUser() != null) {
            User user = jobPost.getRecruiter().getUser();
            String name = (user.getFirstName() != null ? user.getFirstName() : "") + " " +
                          (user.getLastName() != null ? user.getLastName() : "");
            recruiterName = StringUtils.hasText(name.trim()) ? name.trim() : user.getUsername();
        }

        String company = jobPost.getCompanyName();
        if (!StringUtils.hasText(company) && jobPost.getRecruiter() != null) {
            company = jobPost.getRecruiter().getCompanyName();
        }
        if (!StringUtils.hasText(company)) {
            company = "Company Not Specified";
        }

        String reqSkills = jobPost.getRequiredSkills();
        if (!StringUtils.hasText(reqSkills)) {
            reqSkills = jobPost.getRequirements();
        }
        if (!StringUtils.hasText(reqSkills)) {
            reqSkills = "";
        }

        return JobPostDto.builder()
                .id(jobPost.getId())
                .recruiterId(jobPost.getRecruiter() != null ? jobPost.getRecruiter().getId() : null)
                .recruiterName(recruiterName)
                .companyName(company)
                .title(jobPost.getTitle() != null ? jobPost.getTitle() : "Untitled Job")
                .description(jobPost.getDescription() != null ? jobPost.getDescription() : "")
                .requiredSkills(reqSkills)
                .requirements(jobPost.getRequirements() != null ? jobPost.getRequirements() : "")
                .qualification(jobPost.getQualification() != null ? jobPost.getQualification() : "Not specified")
                .experienceRequired(jobPost.getExperienceRequired() != null ? jobPost.getExperienceRequired() : "Not specified")
                .location(jobPost.getLocation() != null ? jobPost.getLocation() : "Not specified")
                .jobType(jobPost.getJobType() != null ? jobPost.getJobType() : "Full Time")
                .salaryRange(jobPost.getSalaryRange() != null ? jobPost.getSalaryRange() : "Not disclosed")
                .deadline(jobPost.getDeadline())
                .formattedDeadline(jobPost.getDeadline() != null ? jobPost.getDeadline().format(DATE_FORMATTER) : "N/A")
                .status(jobPost.getStatus() != null ? jobPost.getStatus() : "DRAFT")
                .applicationCount(appCount)
                .createdAt(jobPost.getCreatedAt())
                .formattedCreatedAt(jobPost.getCreatedAt() != null ? jobPost.getCreatedAt().format(DATETIME_FORMATTER) : "")
                .build();
    }
}
