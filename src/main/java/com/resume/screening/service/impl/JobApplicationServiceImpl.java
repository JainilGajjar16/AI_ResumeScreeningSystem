package com.resume.screening.service.impl;

import com.resume.screening.dto.JobApplicationDto;
import com.resume.screening.entity.*;
import com.resume.screening.repository.*;
import com.resume.screening.service.JobApplicationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class JobApplicationServiceImpl implements JobApplicationService {

    private final JobApplicationRepository jobApplicationRepository;
    private final JobPostRepository jobPostRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final ResumeRepository resumeRepository;
    private final RecruiterRepository recruiterRepository;
    private final UserRepository userRepository;
    private final com.resume.screening.service.UserResolutionService userResolutionService;

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public JobApplicationServiceImpl(JobApplicationRepository jobApplicationRepository,
                                     JobPostRepository jobPostRepository,
                                     StudentProfileRepository studentProfileRepository,
                                     ResumeRepository resumeRepository,
                                     RecruiterRepository recruiterRepository,
                                     UserRepository userRepository) {
        this(jobApplicationRepository, jobPostRepository, studentProfileRepository, resumeRepository, recruiterRepository, userRepository, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public JobApplicationServiceImpl(JobApplicationRepository jobApplicationRepository,
                                     JobPostRepository jobPostRepository,
                                     StudentProfileRepository studentProfileRepository,
                                     ResumeRepository resumeRepository,
                                     RecruiterRepository recruiterRepository,
                                     UserRepository userRepository,
                                     com.resume.screening.service.UserResolutionService userResolutionService) {
        this.jobApplicationRepository = jobApplicationRepository;
        this.jobPostRepository = jobPostRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.resumeRepository = resumeRepository;
        this.recruiterRepository = recruiterRepository;
        this.userRepository = userRepository;
        this.userResolutionService = userResolutionService != null ? userResolutionService :
                new com.resume.screening.service.impl.UserResolutionServiceImpl(userRepository, studentProfileRepository, recruiterRepository);
    }

    @Override
    @Transactional
    public JobApplicationDto applyForJob(Long jobId, String studentUsername) {
        User user = findUser(studentUsername);
        StudentProfile studentProfile = getOrCreateStudentProfile(user);

        JobPost jobPost = jobPostRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found with ID: " + jobId));

        // Validation 1: Job status must be PUBLISHED
        if (!"PUBLISHED".equalsIgnoreCase(jobPost.getStatus())) {
            throw new IllegalArgumentException("This job posting is not currently accepting applications.");
        }

        // Validation 2: Deadline check
        if (jobPost.getDeadline() != null && jobPost.getDeadline().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("The application deadline for this job has passed.");
        }

        // Validation 3: Duplicate Application Check
        if (jobApplicationRepository.existsByJobPostIdAndStudentProfileId(jobId, studentProfile.getId())) {
            throw new IllegalArgumentException("You have already applied for this job.");
        }

        // Validation 4: Active Resume Check
        Optional<Resume> resumeOpt = resumeRepository.findByStudentProfileIdAndIsCurrentTrue(studentProfile.getId());
        if (resumeOpt.isEmpty()) {
            throw new IllegalArgumentException("Please upload your resume before applying for jobs.");
        }

        JobApplication application = JobApplication.builder()
                .jobPost(jobPost)
                .studentProfile(studentProfile)
                .resume(resumeOpt.get())
                .status("APPLIED")
                .build();

        JobApplication saved = jobApplicationRepository.save(application);
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobApplicationDto> getApplicationsForStudent(String studentUsername) {
        User user = findUser(studentUsername);
        StudentProfile profile = getOrCreateStudentProfile(user);

        List<JobApplication> apps = jobApplicationRepository.findByStudentProfileIdOrderByAppliedAtDesc(profile.getId());
        return apps.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobApplicationDto> getApplicantsForJob(Long jobId, String recruiterUsername) {
        JobPost jobPost = jobPostRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found with ID: " + jobId));

        User recruiterUser = findUser(recruiterUsername);
        Recruiter recruiter = recruiterRepository.findByUserId(recruiterUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Recruiter profile not found."));

        if (!jobPost.getRecruiter().getId().equals(recruiter.getId())) {
            throw new IllegalArgumentException("Unauthorized access: You do not own this job posting.");
        }

        List<JobApplication> apps = jobApplicationRepository.findByJobPostIdOrderByAppliedAtDesc(jobId);
        return apps.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public JobApplicationDto getApplicantProfileForRecruiter(Long studentProfileId, String recruiterUsername) {
        User recruiterUser = findUser(recruiterUsername);
        Recruiter recruiter = recruiterRepository.findByUserId(recruiterUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Recruiter profile not found."));

        if (!studentProfileRepository.existsById(studentProfileId)) {
            throw new IllegalArgumentException("Student profile not found with ID: " + studentProfileId);
        }

        // Ensure recruiter has at least one application from this student
        List<JobApplication> studentApps = jobApplicationRepository.findByStudentProfileId(studentProfileId);
        boolean correspondsToRecruiter = studentApps.stream()
                .anyMatch(app -> app.getJobPost().getRecruiter().getId().equals(recruiter.getId()));

        if (!correspondsToRecruiter) {
            throw new IllegalArgumentException("Unauthorized access to candidate profile.");
        }

        // Return most recent application context
        JobApplication latestApp = studentApps.get(0);
        return mapToDto(latestApp);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasStudentApplied(Long jobId, String studentUsername) {
        User user = findUser(studentUsername);
        Optional<StudentProfile> profileOpt = studentProfileRepository.findByUserId(user.getId());
        if (profileOpt.isEmpty()) {
            return false;
        }
        return jobApplicationRepository.existsByJobPostIdAndStudentProfileId(jobId, profileOpt.get().getId());
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

    private StudentProfile getOrCreateStudentProfile(User user) {
        return studentProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> studentProfileRepository.save(StudentProfile.builder().user(user).build()));
    }

    private JobApplicationDto mapToDto(JobApplication app) {
        StudentProfile student = app.getStudentProfile();
        User studentUser = student != null ? student.getUser() : null;
        JobPost job = app.getJobPost();
        Resume resume = app.getResume();

        String studentName = "";
        String studentEmail = "";
        if (studentUser != null) {
            studentName = (studentUser.getFirstName() != null ? studentUser.getFirstName() : "") + " " +
                          (studentUser.getLastName() != null ? studentUser.getLastName() : "");
            if (!StringUtils.hasText(studentName.trim())) {
                studentName = studentUser.getUsername() != null ? studentUser.getUsername() : "Candidate";
            }
            studentEmail = studentUser.getEmail() != null ? studentUser.getEmail() : "";
        } else if (student != null) {
            studentName = "Student #" + student.getId();
        } else {
            studentName = "Unknown Candidate";
        }

        String company = "";
        if (job != null) {
            company = StringUtils.hasText(job.getCompanyName()) 
                    ? job.getCompanyName() 
                    : (job.getRecruiter() != null ? job.getRecruiter().getCompanyName() : "");
        }

        return JobApplicationDto.builder()
                .id(app.getId())
                .jobId(job != null ? job.getId() : null)
                .jobTitle(job != null ? job.getTitle() : "")
                .companyName(StringUtils.hasText(company) ? company : "Company Not Specified")
                .location(job != null ? job.getLocation() : null)
                .employmentType(job != null ? job.getJobType() : null)
                .jobStatus(job != null ? job.getStatus() : null)
                .studentProfileId(student != null ? student.getId() : null)
                .studentName(studentName.trim())
                .studentEmail(studentEmail)
                .studentPhone(student != null ? student.getPhone() : null)
                .studentCollege(student != null ? student.getCollege() : null)
                .studentDegree(student != null ? student.getDegree() : null)
                .studentBranch(student != null ? student.getBranch() : null)
                .studentCgpa(student != null ? student.getCgpa() : null)
                .studentSkills(student != null ? student.getSkillsText() : null)
                .githubUrl(student != null ? student.getGithubUrl() : null)
                .linkedinUrl(student != null ? student.getLinkedinUrl() : null)
                .education(student != null ? student.getEducation() : null)
                .experience(student != null ? student.getExperience() : null)
                .summary(student != null ? student.getSummary() : null)
                .resumeId(resume != null ? resume.getId() : null)
                .resumeFileName(resume != null ? (resume.getOriginalFileName() != null ? resume.getOriginalFileName() : resume.getFileName()) : null)
                .resumeFileType(resume != null ? resume.getFileType() : null)
                .appliedAt(app.getAppliedAt())
                .formattedAppliedAt(app.getAppliedAt() != null ? app.getAppliedAt().format(DATETIME_FORMATTER) : "")
                .status(app.getStatus() != null ? app.getStatus() : "APPLIED")
                .build();
    }
}
