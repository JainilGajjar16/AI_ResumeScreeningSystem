package com.resume.screening.service.impl;

import com.resume.screening.dto.ShortlistedCandidateDto;
import com.resume.screening.entity.*;
import com.resume.screening.enums.ApplicationDecision;
import com.resume.screening.repository.*;
import com.resume.screening.service.ShortlistService;
import com.resume.screening.service.UserResolutionService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ShortlistServiceImpl implements ShortlistService {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ShortlistedCandidateRepository shortlistedCandidateRepository;
    private final JobPostRepository jobPostRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final RecruiterRepository recruiterRepository;
    private final ResumeAnalysisRepository resumeAnalysisRepository;
    private final UserResolutionService userResolutionService;

    public ShortlistServiceImpl(ShortlistedCandidateRepository shortlistedCandidateRepository,
                                JobPostRepository jobPostRepository,
                                JobApplicationRepository jobApplicationRepository,
                                StudentProfileRepository studentProfileRepository,
                                RecruiterRepository recruiterRepository,
                                ResumeAnalysisRepository resumeAnalysisRepository,
                                UserResolutionService userResolutionService) {
        this.shortlistedCandidateRepository = shortlistedCandidateRepository;
        this.jobPostRepository = jobPostRepository;
        this.jobApplicationRepository = jobApplicationRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.recruiterRepository = recruiterRepository;
        this.resumeAnalysisRepository = resumeAnalysisRepository;
        this.userResolutionService = userResolutionService;
    }

    @Override
    @Transactional
    public void shortlistCandidate(Long jobId, Long studentProfileId, String recruiterUsername) {
        User recruiterUser = resolveUser(recruiterUsername);
        Recruiter recruiter = recruiterRepository.findByUserId(recruiterUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Recruiter profile not found."));

        JobPost jobPost = jobPostRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job post not found with ID: " + jobId));

        if (!jobPost.getRecruiter().getId().equals(recruiter.getId())) {
            throw new AccessDeniedException("Unauthorized access: You do not own this job posting.");
        }

        StudentProfile student = studentProfileRepository.findById(studentProfileId)
                .orElseThrow(() -> new IllegalArgumentException("Student profile not found with ID: " + studentProfileId));

        // Check if candidate applied
        List<JobApplication> apps = jobApplicationRepository.findByJobPostId(jobId);
        JobApplication app = apps.stream()
                .filter(a -> a.getStudentProfile().getId().equals(studentProfileId))
                .findFirst()
                .orElse(null);

        // Update JobApplication status to SHORTLISTED if application exists
        if (app != null) {
            app.setStatus("SHORTLISTED");
            jobApplicationRepository.save(app);
        }

        // Check if already in shortlisted_candidates table
        if (!shortlistedCandidateRepository.existsByJobPostIdAndStudentProfileId(jobId, studentProfileId)) {
            ShortlistedCandidate candidate = ShortlistedCandidate.builder()
                    .jobPost(jobPost)
                    .studentProfile(student)
                    .addedBy(recruiterUser)
                    .status("SHORTLISTED")
                    .build();
            shortlistedCandidateRepository.save(candidate);
        }
    }

    @Override
    @Transactional
    public void removeCandidateFromShortlist(Long jobId, Long studentProfileId, String recruiterUsername) {
        User recruiterUser = resolveUser(recruiterUsername);
        Recruiter recruiter = recruiterRepository.findByUserId(recruiterUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Recruiter profile not found."));

        JobPost jobPost = jobPostRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job post not found with ID: " + jobId));

        if (!jobPost.getRecruiter().getId().equals(recruiter.getId())) {
            throw new AccessDeniedException("Unauthorized access: You do not own this job posting.");
        }

        shortlistedCandidateRepository.deleteByJobPostIdAndStudentProfileId(jobId, studentProfileId);

        // Update application status back to APPLIED without deleting JobApplication
        List<JobApplication> apps = jobApplicationRepository.findByJobPostId(jobId);
        apps.stream()
                .filter(a -> a.getStudentProfile().getId().equals(studentProfileId))
                .forEach(a -> {
                    if ("SHORTLISTED".equalsIgnoreCase(a.getStatus())) {
                        a.setStatus("APPLIED");
                        jobApplicationRepository.save(a);
                    }
                });
    }

    @Override
    @Transactional
    public void removeShortlistById(Long shortlistId, String recruiterUsername) {
        User recruiterUser = resolveUser(recruiterUsername);
        Recruiter recruiter = recruiterRepository.findByUserId(recruiterUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Recruiter profile not found."));

        ShortlistedCandidate candidate = shortlistedCandidateRepository.findById(shortlistId)
                .orElseThrow(() -> new IllegalArgumentException("Shortlist record not found with ID: " + shortlistId));

        if (!candidate.getJobPost().getRecruiter().getId().equals(recruiter.getId()) &&
            !candidate.getAddedBy().getId().equals(recruiterUser.getId())) {
            throw new AccessDeniedException("Unauthorized access to shortlist record.");
        }

        Long jobId = candidate.getJobPost().getId();
        Long studentId = candidate.getStudentProfile().getId();

        shortlistedCandidateRepository.delete(candidate);

        // Revert application status if needed
        List<JobApplication> apps = jobApplicationRepository.findByJobPostId(jobId);
        apps.stream()
                .filter(a -> a.getStudentProfile().getId().equals(studentId))
                .forEach(a -> {
                    if ("SHORTLISTED".equalsIgnoreCase(a.getStatus())) {
                        a.setStatus("APPLIED");
                        jobApplicationRepository.save(a);
                    }
                });
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShortlistedCandidateDto> getShortlistedCandidatesForRecruiter(String recruiterUsername) {
        User recruiterUser = resolveUser(recruiterUsername);
        Recruiter recruiter = recruiterRepository.findByUserId(recruiterUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Recruiter profile not found."));

        List<ShortlistedCandidate> list = shortlistedCandidateRepository.findByJobPostRecruiterUserIdOrderByAddedAtDesc(recruiterUser.getId());
        if (list.isEmpty()) {
            list = shortlistedCandidateRepository.findByAddedByIdOrderByAddedAtDesc(recruiterUser.getId());
        }

        return list.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isCandidateShortlisted(Long jobId, Long studentProfileId) {
        return shortlistedCandidateRepository.existsByJobPostIdAndStudentProfileId(jobId, studentProfileId);
    }

    private User resolveUser(String usernameOrEmail) {
        return userResolutionService.resolveUser(usernameOrEmail);
    }

    private ShortlistedCandidateDto mapToDto(ShortlistedCandidate entity) {
        JobPost job = entity.getJobPost();
        StudentProfile student = entity.getStudentProfile();
        User studentUser = student != null ? student.getUser() : null;

        String studentName = "Student";
        String studentEmail = "";
        if (studentUser != null) {
            studentName = (studentUser.getFirstName() != null ? studentUser.getFirstName() : "") + " " +
                          (studentUser.getLastName() != null ? studentUser.getLastName() : "");
            studentName = studentName.trim();
            if (studentName.isEmpty()) {
                studentName = studentUser.getUsername();
            }
            studentEmail = studentUser.getEmail();
        }

        String company = job != null ? (StringUtils.hasText(job.getCompanyName()) ? job.getCompanyName() : (job.getRecruiter() != null ? job.getRecruiter().getCompanyName() : "")) : "";

        ResumeAnalysis analysis = null;
        Long resumeId = null;
        Long jobApplicationId = null;
        String appStatus = entity.getStatus() != null ? entity.getStatus() : "SHORTLISTED";

        ApplicationDecision finalDecision = null;

        if (job != null && student != null) {
            Optional<JobApplication> appOpt = jobApplicationRepository.findByJobPostIdAndStudentProfileId(job.getId(), student.getId());

            if (appOpt.isPresent()) {
                JobApplication app = appOpt.get();
                jobApplicationId = app.getId();
                if (app.getStatus() != null) {
                    appStatus = app.getStatus();
                }
                if (app.getResume() != null) {
                    resumeId = app.getResume().getId();
                }
                finalDecision = app.getFinalDecision();
            }

            List<ResumeAnalysis> analyses = resumeAnalysisRepository.findByStudentProfileId(student.getId());
            if (analyses != null && !analyses.isEmpty()) {
                analysis = analyses.stream()
                        .filter(a -> a.getJobPost() != null && job.getId().equals(a.getJobPost().getId()))
                        .max(Comparator.comparing(ResumeAnalysis::getAnalyzedAt, Comparator.nullsFirst(Comparator.naturalOrder()))
                                .thenComparing(ResumeAnalysis::getId, Comparator.nullsFirst(Comparator.naturalOrder())))
                        .orElse(null);
            }
        }

        Double atsScore = analysis != null ? analysis.getAtsScore() : null;
        String formattedAts = atsScore != null ? String.format("%.1f%%", atsScore) : "ATS Analysis Not Available";

        Double skillMatchPct = analysis != null ? (analysis.getSkillMatchPercentage() != null ? analysis.getSkillMatchPercentage() : analysis.getSkillsMatchScore()) : null;

        return ShortlistedCandidateDto.builder()
                .id(entity.getId())
                .jobId(job != null ? job.getId() : null)
                .jobTitle(job != null ? job.getTitle() : "")
                .companyName(company)
                .jobApplicationId(jobApplicationId)
                .studentProfileId(student != null ? student.getId() : null)
                .studentName(studentName)
                .studentEmail(studentEmail)
                .studentDegree(student != null ? student.getDegree() : null)
                .studentCollege(student != null ? student.getCollege() : null)
                .resumeId(resumeId)
                .atsScore(atsScore)
                .formattedAtsScore(formattedAts)
                .skillMatchPercentage(skillMatchPct)
                .qualificationMatch(analysis != null && StringUtils.hasText(analysis.getQualificationMatch()) ? analysis.getQualificationMatch() : "N/A")
                .experienceMatch(analysis != null && StringUtils.hasText(analysis.getExperienceMatch()) ? analysis.getExperienceMatch() : "N/A")
                .shortlistedAt(entity.getAddedAt())
                .formattedShortlistedAt(entity.getAddedAt() != null ? entity.getAddedAt().format(DATETIME_FORMATTER) : "")
                .applicationStatus(appStatus)
                .finalDecision(finalDecision)
                .build();
    }
}
