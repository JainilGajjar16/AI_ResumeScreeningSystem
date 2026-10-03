package com.resume.screening.service.impl;

import com.resume.screening.config.CandidateRecommendationConfig;
import com.resume.screening.dto.CandidateRecommendationDto;
import com.resume.screening.entity.*;
import com.resume.screening.enums.ApplicationDecision;
import com.resume.screening.repository.*;
import com.resume.screening.service.CandidateRecommendationService;
import com.resume.screening.service.UserResolutionService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class CandidateRecommendationServiceImpl implements CandidateRecommendationService {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final JobPostRepository jobPostRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final ResumeAnalysisRepository resumeAnalysisRepository;
    private final ShortlistedCandidateRepository shortlistedCandidateRepository;
    private final RecruiterRepository recruiterRepository;
    private final UserRepository userRepository;
    private final UserResolutionService userResolutionService;
    private final CandidateRecommendationConfig recommendationConfig;

    public CandidateRecommendationServiceImpl(JobPostRepository jobPostRepository,
                                               JobApplicationRepository jobApplicationRepository,
                                               ResumeAnalysisRepository resumeAnalysisRepository,
                                               ShortlistedCandidateRepository shortlistedCandidateRepository,
                                               RecruiterRepository recruiterRepository,
                                               UserRepository userRepository,
                                               UserResolutionService userResolutionService,
                                               CandidateRecommendationConfig recommendationConfig) {
        this.jobPostRepository = jobPostRepository;
        this.jobApplicationRepository = jobApplicationRepository;
        this.resumeAnalysisRepository = resumeAnalysisRepository;
        this.shortlistedCandidateRepository = shortlistedCandidateRepository;
        this.recruiterRepository = recruiterRepository;
        this.userRepository = userRepository;
        this.userResolutionService = userResolutionService;
        this.recommendationConfig = recommendationConfig != null ? recommendationConfig : new CandidateRecommendationConfig();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CandidateRecommendationDto> getRankedCandidatesForJob(Long jobId, String recruiterUsername) {
        JobPost jobPost = validateAndGetJobPost(jobId, recruiterUsername);

        List<JobApplication> applications = jobApplicationRepository.findByJobPostId(jobId);
        List<ShortlistedCandidate> shortlistedCandidates = shortlistedCandidateRepository.findByJobPostId(jobId);
        
        Map<Long, ShortlistedCandidate> shortlistMap = new HashMap<>();
        if (shortlistedCandidates != null) {
            for (ShortlistedCandidate sc : shortlistedCandidates) {
                if (sc != null && sc.getStudentProfile() != null && sc.getStudentProfile().getId() != null) {
                    shortlistMap.put(sc.getStudentProfile().getId(), sc);
                }
            }
        }

        List<CandidateRecommendationDto> list = new ArrayList<>();
        if (applications != null) {
            for (JobApplication app : applications) {
                if (app != null) {
                    Long studentId = (app.getStudentProfile() != null) ? app.getStudentProfile().getId() : null;
                    ShortlistedCandidate scRecord = (studentId != null) ? shortlistMap.get(studentId) : null;
                    list.add(mapToRecommendationDto(app, jobPost, scRecord));
                }
            }
        }

        // Rank candidates safely:
        // Candidates WITH ATS scores sorted DESCENDING by atsScore (highest first).
        // Candidates WITHOUT ATS score sorted AFTER, ordered by application time descending.
        list.sort((c1, c2) -> {
            boolean c1Valid = c1.isHasAtsAnalysis() && c1.getAtsScore() != null;
            boolean c2Valid = c2.isHasAtsAnalysis() && c2.getAtsScore() != null;

            if (c1Valid && c2Valid) {
                return Double.compare(c2.getAtsScore(), c1.getAtsScore());
            } else if (c1Valid) {
                return -1; // c1 comes first
            } else if (c2Valid) {
                return 1;  // c2 comes first
            } else {
                if (c1.getAppliedAt() != null && c2.getAppliedAt() != null) {
                    return c2.getAppliedAt().compareTo(c1.getAppliedAt());
                }
                return 0;
            }
        });

        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public CandidateRecommendationDto getCandidateRecommendationForJob(Long jobId, Long studentProfileId, String recruiterUsername) {
        JobPost jobPost = validateAndGetJobPost(jobId, recruiterUsername);

        List<JobApplication> apps = jobApplicationRepository.findByJobPostId(jobId);
        JobApplication targetApp = apps.stream()
                .filter(app -> app != null && app.getStudentProfile() != null && studentProfileId.equals(app.getStudentProfile().getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Candidate application not found for job ID: " + jobId));

        ShortlistedCandidate shortlistRecord = shortlistedCandidateRepository
                .findByJobPostIdAndStudentProfileId(jobId, studentProfileId)
                .orElse(null);

        return mapToRecommendationDto(targetApp, jobPost, shortlistRecord);
    }

    private JobPost validateAndGetJobPost(Long jobId, String recruiterUsername) {
        User recruiterUser = userResolutionService.resolveUser(recruiterUsername);
        if (recruiterUser == null) {
            throw new IllegalArgumentException("User not found: " + recruiterUsername);
        }

        Recruiter recruiter = recruiterRepository.findByUserId(recruiterUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Recruiter profile not found."));

        JobPost jobPost = jobPostRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job post not found with ID: " + jobId));

        if (!jobPost.getRecruiter().getId().equals(recruiter.getId())) {
            throw new AccessDeniedException("Unauthorized access: You do not own this job posting.");
        }

        return jobPost;
    }

    private CandidateRecommendationDto mapToRecommendationDto(JobApplication app, JobPost jobPost, ShortlistedCandidate shortlistRecord) {
        StudentProfile student = app.getStudentProfile();
        User studentUser = student != null ? student.getUser() : null;
        Resume resume = app.getResume();

        String studentName = "Student";
        String studentEmail = "";
        if (studentUser != null) {
            studentName = (studentUser.getFirstName() != null ? studentUser.getFirstName() : "") + " " +
                          (studentUser.getLastName() != null ? studentUser.getLastName() : "");
            studentName = studentName.trim();
            if (studentName.isEmpty()) {
                studentName = studentUser.getUsername() != null ? studentUser.getUsername() : "Candidate";
            }
            studentEmail = studentUser.getEmail() != null ? studentUser.getEmail() : "";
        } else if (student != null) {
            studentName = "Candidate #" + student.getId();
        }

        // Search for existing ResumeAnalysis
        ResumeAnalysis analysis = null;
        if (resume != null && jobPost != null) {
            Integer versionNum = resume.getCurrentVersion() != null ? resume.getCurrentVersion() : 1;
            analysis = resumeAnalysisRepository
                    .findByResumeIdAndJobPostIdAndResumeVersionNumber(resume.getId(), jobPost.getId(), versionNum)
                    .orElse(null);

            if (analysis == null) {
                List<ResumeAnalysis> list = resumeAnalysisRepository.findByResumeIdAndJobPostId(resume.getId(), jobPost.getId());
                if (list != null && !list.isEmpty()) {
                    analysis = list.get(list.size() - 1);
                }
            }
        }

        if (analysis == null && student != null && jobPost != null) {
            List<ResumeAnalysis> studentAnalyses = resumeAnalysisRepository.findByStudentProfileId(student.getId());
            if (studentAnalyses != null && !studentAnalyses.isEmpty()) {
                analysis = studentAnalyses.stream()
                        .filter(a -> a != null && a.getJobPost() != null && jobPost.getId().equals(a.getJobPost().getId()))
                        .max(Comparator.comparing(ResumeAnalysis::getAnalyzedAt, Comparator.nullsFirst(Comparator.naturalOrder()))
                                .thenComparing(ResumeAnalysis::getId, Comparator.nullsFirst(Comparator.naturalOrder())))
                        .orElse(null);
            }
        }

        boolean hasAnalysis = (analysis != null && analysis.getAtsScore() != null);
        Double atsScore = hasAnalysis ? analysis.getAtsScore() : null;
        String formattedAts = hasAnalysis && atsScore != null ? String.format("%.1f%%", atsScore) : "ATS Analysis Not Available";

        Double skillMatchPct = analysis != null ? (analysis.getSkillMatchPercentage() != null ? analysis.getSkillMatchPercentage() : analysis.getSkillsMatchScore()) : null;
        String formattedSkillMatch = skillMatchPct != null ? String.format("%.1f%%", skillMatchPct) : "N/A";

        List<String> matchedSkills = (analysis != null && StringUtils.hasText(analysis.getMatchedSkills()))
                ? Arrays.asList(analysis.getMatchedSkills().split("[,;]\\s*"))
                : Collections.emptyList();

        List<String> missingSkills = (analysis != null && StringUtils.hasText(analysis.getMissingSkills()))
                ? Arrays.asList(analysis.getMissingSkills().split("[,;]\\s*"))
                : Collections.emptyList();

        CandidateRecommendationConfig config = recommendationConfig != null ? recommendationConfig : new CandidateRecommendationConfig();
        String label = config.getRecommendationLabel(atsScore);
        String badgeClass = config.getBadgeClass(atsScore);

        boolean isShortlisted = (shortlistRecord != null) || "SHORTLISTED".equalsIgnoreCase(app.getStatus());

        return CandidateRecommendationDto.builder()
                .applicationId(app.getId())
                .jobId(jobPost != null ? jobPost.getId() : null)
                .jobTitle(jobPost != null ? jobPost.getTitle() : "")
                .companyName(jobPost != null ? jobPost.getCompanyName() : "")
                .studentProfileId(student != null ? student.getId() : null)
                .studentName(studentName)
                .studentEmail(studentEmail)
                .studentPhone(student != null ? student.getPhone() : null)
                .studentCollege(student != null ? student.getCollege() : null)
                .studentDegree(student != null ? student.getDegree() : null)
                .studentBranch(student != null ? student.getBranch() : null)
                .studentSkills(student != null ? student.getSkillsText() : null)
                .githubUrl(student != null ? student.getGithubUrl() : null)
                .linkedinUrl(student != null ? student.getLinkedinUrl() : null)
                .appliedAt(app.getAppliedAt())
                .formattedAppliedAt(app.getAppliedAt() != null ? app.getAppliedAt().format(DATETIME_FORMATTER) : "")
                .applicationStatus(app.getStatus() != null ? app.getStatus() : "APPLIED")
                .finalDecision(app.getFinalDecision())
                .resumeId(resume != null ? resume.getId() : null)
                .resumeFileName(resume != null ? (resume.getOriginalFileName() != null ? resume.getOriginalFileName() : resume.getFileName()) : null)
                .hasAtsAnalysis(hasAnalysis)
                .atsScore(atsScore)
                .formattedAtsScore(formattedAts)
                .skillMatchPercentage(skillMatchPct)
                .formattedSkillMatch(formattedSkillMatch)
                .qualificationMatch(analysis != null && StringUtils.hasText(analysis.getQualificationMatch()) ? analysis.getQualificationMatch() : "N/A")
                .experienceMatch(analysis != null && StringUtils.hasText(analysis.getExperienceMatch()) ? analysis.getExperienceMatch() : "N/A")
                .matchedSkills(matchedSkills)
                .missingSkills(missingSkills)
                .recommendationLabel(label)
                .recommendationBadgeClass(badgeClass)
                .isShortlisted(isShortlisted)
                .shortlistedCandidateId(shortlistRecord != null ? shortlistRecord.getId() : null)
                .build();
    }
}
