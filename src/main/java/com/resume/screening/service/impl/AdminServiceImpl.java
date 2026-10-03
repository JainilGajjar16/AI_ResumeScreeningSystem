package com.resume.screening.service.impl;

import com.resume.screening.dto.*;
import com.resume.screening.entity.*;
import com.resume.screening.enums.ApplicationDecision;
import com.resume.screening.repository.*;
import com.resume.screening.service.AdminService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final RecruiterRepository recruiterRepository;
    private final JobPostRepository jobPostRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final InterviewRepository interviewRepository;
    private final ShortlistedCandidateRepository shortlistedCandidateRepository;
    private final AtsScoreRepository atsScoreRepository;
    private final ResumeRecommendationRepository resumeRecommendationRepository;
    private final ResumeAnalysisRepository resumeAnalysisRepository;

    public AdminServiceImpl(UserRepository userRepository,
                            StudentProfileRepository studentProfileRepository,
                            RecruiterRepository recruiterRepository,
                            JobPostRepository jobPostRepository,
                            JobApplicationRepository jobApplicationRepository,
                            InterviewRepository interviewRepository,
                            ShortlistedCandidateRepository shortlistedCandidateRepository,
                            AtsScoreRepository atsScoreRepository,
                            ResumeRecommendationRepository resumeRecommendationRepository,
                            ResumeAnalysisRepository resumeAnalysisRepository) {
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.recruiterRepository = recruiterRepository;
        this.jobPostRepository = jobPostRepository;
        this.jobApplicationRepository = jobApplicationRepository;
        this.interviewRepository = interviewRepository;
        this.shortlistedCandidateRepository = shortlistedCandidateRepository;
        this.atsScoreRepository = atsScoreRepository;
        this.resumeRecommendationRepository = resumeRecommendationRepository;
        this.resumeAnalysisRepository = resumeAnalysisRepository;
    }

    @Override
    public AdminDashboardStatsDto getDashboardStats() {
        try {
            long totalUsers = userRepository.count();
            long totalStudents = studentProfileRepository.count();
            long totalRecruiters = recruiterRepository.count();
            long totalJobs = jobPostRepository.count();
            
            List<JobPost> allJobs = jobPostRepository.findAll();
            long activeJobs = allJobs != null ? allJobs.stream().filter(j -> j != null && "PUBLISHED".equalsIgnoreCase(j.getStatus())).count() : 0;

            List<JobApplication> allApps = jobApplicationRepository.findAll();
            long totalApplications = allApps != null ? allApps.size() : 0;

            List<Interview> allInterviews = interviewRepository.findAll();
            long totalInterviews = allInterviews != null ? allInterviews.size() : 0;
            long scheduledInterviews = allInterviews != null ? allInterviews.stream().filter(i -> i != null && i.getStatus() == InterviewStatus.SCHEDULED).count() : 0;
            long completedInterviews = allInterviews != null ? allInterviews.stream().filter(i -> i != null && i.getStatus() == InterviewStatus.COMPLETED).count() : 0;

            long totalShortlistedCandidates = shortlistedCandidateRepository.count();
            long interviewSelectedCandidates = allInterviews != null ? allInterviews.stream().filter(i -> i != null && i.getResult() == InterviewResult.SELECTED).count() : 0;

            long finalSelectedCandidates = allApps != null ? allApps.stream().filter(a -> a != null && a.getFinalDecision() == ApplicationDecision.SELECTED).count() : 0;
            long finalRejectedCandidates = allApps != null ? allApps.stream().filter(a -> a != null && a.getFinalDecision() == ApplicationDecision.REJECTED).count() : 0;
            long pendingFinalDecisions = allApps != null ? allApps.stream().filter(a -> a != null && (a.getFinalDecision() == ApplicationDecision.PENDING || a.getFinalDecision() == null)).count() : 0;

            return AdminDashboardStatsDto.builder()
                    .totalUsers(totalUsers)
                    .totalStudents(totalStudents)
                    .totalRecruiters(totalRecruiters)
                    .totalJobs(totalJobs)
                    .activeJobs(activeJobs)
                    .totalApplications(totalApplications)
                    .totalInterviews(totalInterviews)
                    .scheduledInterviews(scheduledInterviews)
                    .completedInterviews(completedInterviews)
                    .totalShortlistedCandidates(totalShortlistedCandidates)
                    .interviewSelectedCandidates(interviewSelectedCandidates)
                    .finalSelectedCandidates(finalSelectedCandidates)
                    .finalRejectedCandidates(finalRejectedCandidates)
                    .pendingFinalDecisions(pendingFinalDecisions)
                    .build();
        } catch (Exception e) {
            return AdminDashboardStatsDto.builder()
                    .totalUsers(userRepository.count())
                    .totalStudents(studentProfileRepository.count())
                    .totalRecruiters(recruiterRepository.count())
                    .totalJobs(jobPostRepository.count())
                    .activeJobs(0)
                    .totalApplications(jobApplicationRepository.count())
                    .totalInterviews(interviewRepository.count())
                    .scheduledInterviews(0)
                    .completedInterviews(0)
                    .totalShortlistedCandidates(shortlistedCandidateRepository.count())
                    .interviewSelectedCandidates(0)
                    .finalSelectedCandidates(0)
                    .finalRejectedCandidates(0)
                    .pendingFinalDecisions(0)
                    .build();
        }
    }

    @Override
    public List<AdminUserDto> getUsers(String query, String roleFilter) {
        List<User> users = userRepository.findAll();
        List<StudentProfile> students = studentProfileRepository.findAll();
        Map<Long, String> userPhoneMap = new HashMap<>();
        if (students != null) {
            for (StudentProfile sp : students) {
                if (sp != null && sp.getUser() != null && sp.getUser().getId() != null) {
                    userPhoneMap.put(sp.getUser().getId(), sp.getPhone());
                }
            }
        }

        String q = query != null ? query.trim().toLowerCase() : "";
        String r = roleFilter != null ? roleFilter.trim().toUpperCase() : "";

        if (users == null) return Collections.emptyList();

        return users.stream()
                .filter(u -> u != null)
                .filter(u -> {
                    if (!q.isEmpty()) {
                        String fullName = ((u.getFirstName() != null ? u.getFirstName() : "") + " " + 
                                           (u.getLastName() != null ? u.getLastName() : "")).toLowerCase();
                        boolean matchName = fullName.contains(q) || (u.getUsername() != null && u.getUsername().toLowerCase().contains(q));
                        boolean matchEmail = u.getEmail() != null && u.getEmail().toLowerCase().contains(q);
                        if (!matchName && !matchEmail) return false;
                    }
                    if (!r.isEmpty()) {
                        String userRole = u.getRole() != null ? u.getRole().toUpperCase() : "";
                        if (!userRole.contains(r) && !("ROLE_" + userRole).contains(r)) {
                            return false;
                        }
                    }
                    return true;
                })
                .map(u -> {
                    String fullName = (u.getFirstName() != null ? u.getFirstName() : "") + " " + 
                                       (u.getLastName() != null ? u.getLastName() : "");
                    fullName = fullName.trim();
                    if (fullName.isEmpty()) fullName = u.getUsername() != null ? u.getUsername() : "User #" + u.getId();

                    String mobile = userPhoneMap.getOrDefault(u.getId(), "N/A");
                    if (mobile == null || mobile.trim().isEmpty()) mobile = "N/A";

                    return AdminUserDto.builder()
                            .id(u.getId())
                            .username(u.getUsername() != null ? u.getUsername() : "N/A")
                            .fullName(fullName)
                            .email(u.getEmail() != null ? u.getEmail() : "N/A")
                            .mobile(mobile)
                            .role(u.getRole() != null ? u.getRole() : "N/A")
                            .createdAt(u.getCreatedAt())
                            .status("ACTIVE")
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<AdminStudentDto> getStudents(String query) {
        List<StudentProfile> profiles = studentProfileRepository.findAll();
        List<JobApplication> allApplications = jobApplicationRepository.findAll();
        List<Interview> allInterviews = interviewRepository.findAll();

        String q = query != null ? query.trim().toLowerCase() : "";
        if (profiles == null) return Collections.emptyList();

        return profiles.stream()
                .filter(sp -> sp != null)
                .filter(sp -> {
                    if (q.isEmpty()) return true;
                    User u = sp.getUser();
                    String fullName = u != null ? ((u.getFirstName() != null ? u.getFirstName() : "") + " " + 
                                                  (u.getLastName() != null ? u.getLastName() : "")).toLowerCase() : "";
                    String email = u != null && u.getEmail() != null ? u.getEmail().toLowerCase() : "";
                    String college = sp.getCollege() != null ? sp.getCollege().toLowerCase() : "";
                    String skills = sp.getSkillsText() != null ? sp.getSkillsText().toLowerCase() : "";
                    return fullName.contains(q) || email.contains(q) || college.contains(q) || skills.contains(q);
                })
                .map(sp -> {
                    User u = sp.getUser();
                    String fullName = u != null ? ((u.getFirstName() != null ? u.getFirstName() : "") + " " + 
                                                  (u.getLastName() != null ? u.getLastName() : "")).trim() : "N/A";
                    if (fullName.isEmpty() && u != null) fullName = u.getUsername();
                    String email = u != null && u.getEmail() != null ? u.getEmail() : "N/A";

                    boolean hasResume = sp.getResumes() != null && !sp.getResumes().isEmpty();
                    String resumeStatus = hasResume ? "Uploaded" : "Not Uploaded";

                    long appCount = allApplications != null ? allApplications.stream()
                            .filter(a -> a != null && a.getStudentProfile() != null && a.getStudentProfile().getId().equals(sp.getId()))
                            .count() : 0;

                    long intCount = allInterviews != null ? allInterviews.stream()
                            .filter(i -> i != null && i.getStudent() != null && i.getStudent().getId().equals(sp.getId()))
                            .count() : 0;

                    String finalStatus = "PENDING";
                    List<JobApplication> studentApps = allApplications != null ? allApplications.stream()
                            .filter(a -> a != null && a.getStudentProfile() != null && a.getStudentProfile().getId().equals(sp.getId()))
                            .collect(Collectors.toList()) : Collections.emptyList();

                    if (studentApps.stream().anyMatch(a -> a.getFinalDecision() == ApplicationDecision.SELECTED)) {
                        finalStatus = "SELECTED";
                    } else if (studentApps.stream().anyMatch(a -> a.getFinalDecision() == ApplicationDecision.REJECTED)) {
                        finalStatus = "REJECTED";
                    }

                    return AdminStudentDto.builder()
                            .id(sp.getId())
                            .studentName(fullName)
                            .email(email)
                            .phone(sp.getPhone() != null ? sp.getPhone() : "N/A")
                            .college(sp.getCollege() != null ? sp.getCollege() : "N/A")
                            .degree(sp.getDegree() != null ? sp.getDegree() : "N/A")
                            .branch(sp.getBranch() != null ? sp.getBranch() : "N/A")
                            .skills(sp.getSkillsText() != null ? sp.getSkillsText() : "N/A")
                            .githubUrl(sp.getGithubUrl())
                            .linkedinUrl(sp.getLinkedinUrl())
                            .portfolioUrl(sp.getSummary())
                            .hasResume(hasResume)
                            .resumeStatus(resumeStatus)
                            .applicationsCount((int) appCount)
                            .interviewsCount((int) intCount)
                            .finalSelectionStatus(finalStatus)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<AdminRecruiterDto> getRecruiters(String query) {
        List<Recruiter> recruiters = recruiterRepository.findAll();
        List<JobPost> allJobs = jobPostRepository.findAll();
        List<JobApplication> allApplications = jobApplicationRepository.findAll();
        List<Interview> allInterviews = interviewRepository.findAll();

        String q = query != null ? query.trim().toLowerCase() : "";
        if (recruiters == null) return Collections.emptyList();

        return recruiters.stream()
                .filter(r -> r != null)
                .filter(r -> {
                    if (q.isEmpty()) return true;
                    User u = r.getUser();
                    String fullName = u != null ? ((u.getFirstName() != null ? u.getFirstName() : "") + " " + 
                                                  (u.getLastName() != null ? u.getLastName() : "")).toLowerCase() : "";
                    String email = u != null && u.getEmail() != null ? u.getEmail().toLowerCase() : "";
                    String company = r.getCompanyName() != null ? r.getCompanyName().toLowerCase() : "";
                    return fullName.contains(q) || email.contains(q) || company.contains(q);
                })
                .map(r -> {
                    User u = r.getUser();
                    String fullName = u != null ? ((u.getFirstName() != null ? u.getFirstName() : "") + " " + 
                                                  (u.getLastName() != null ? u.getLastName() : "")).trim() : "N/A";
                    if (fullName.isEmpty() && u != null) fullName = u.getUsername();
                    String email = u != null && u.getEmail() != null ? u.getEmail() : "N/A";

                    long jobsCount = allJobs != null ? allJobs.stream()
                            .filter(j -> j != null && j.getRecruiter() != null && j.getRecruiter().getId().equals(r.getId()))
                            .count() : 0;

                    long appsCount = allApplications != null ? allApplications.stream()
                            .filter(a -> a != null && a.getJobPost() != null && a.getJobPost().getRecruiter() != null && 
                                         a.getJobPost().getRecruiter().getId().equals(r.getId()))
                            .count() : 0;

                    long intCount = allInterviews != null ? allInterviews.stream()
                            .filter(i -> i != null && i.getRecruiter() != null && i.getRecruiter().getId().equals(r.getId()))
                            .count() : 0;

                    long selectedCount = allApplications != null ? allApplications.stream()
                            .filter(a -> a != null && a.getJobPost() != null && a.getJobPost().getRecruiter() != null && 
                                         a.getJobPost().getRecruiter().getId().equals(r.getId()) &&
                                         a.getFinalDecision() == ApplicationDecision.SELECTED)
                            .count() : 0;

                    return AdminRecruiterDto.builder()
                            .id(r.getId())
                            .recruiterName(fullName)
                            .email(email)
                            .companyName(r.getCompanyName() != null ? r.getCompanyName() : "N/A")
                            .companyWebsite(r.getCompanyWebsite())
                            .designation(r.getDesignation() != null ? r.getDesignation() : "N/A")
                            .jobsPosted(jobsCount)
                            .applicationsReceived(appsCount)
                            .interviewsConducted(intCount)
                            .selectedCandidates(selectedCount)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<AdminJobDto> getJobs(String query, String statusFilter) {
        List<JobPost> jobs = jobPostRepository.findAll();
        List<JobApplication> allApps = jobApplicationRepository.findAll();
        List<ShortlistedCandidate> allShortlisted = shortlistedCandidateRepository.findAll();
        List<Interview> allInterviews = interviewRepository.findAll();

        String q = query != null ? query.trim().toLowerCase() : "";
        String sf = statusFilter != null ? statusFilter.trim().toUpperCase() : "";

        if (jobs == null) return Collections.emptyList();

        return jobs.stream()
                .filter(j -> j != null)
                .filter(j -> {
                    if (!q.isEmpty()) {
                        String title = j.getTitle() != null ? j.getTitle().toLowerCase() : "";
                        String company = j.getCompanyName() != null ? j.getCompanyName().toLowerCase() : "";
                        String location = j.getLocation() != null ? j.getLocation().toLowerCase() : "";
                        if (!title.contains(q) && !company.contains(q) && !location.contains(q)) return false;
                    }
                    if (!sf.isEmpty() && j.getStatus() != null) {
                        if (!j.getStatus().toUpperCase().equals(sf)) return false;
                    }
                    return true;
                })
                .map(j -> {
                    String recruiterName = "N/A";
                    String recruiterEmail = "N/A";
                    if (j.getRecruiter() != null && j.getRecruiter().getUser() != null) {
                        User u = j.getRecruiter().getUser();
                        recruiterName = ((u.getFirstName() != null ? u.getFirstName() : "") + " " + 
                                         (u.getLastName() != null ? u.getLastName() : "")).trim();
                        if (recruiterName.isEmpty()) recruiterName = u.getUsername();
                        recruiterEmail = u.getEmail() != null ? u.getEmail() : "N/A";
                    }

                    long appsCount = allApps != null ? allApps.stream()
                            .filter(a -> a != null && a.getJobPost() != null && a.getJobPost().getId().equals(j.getId()))
                            .count() : 0;

                    long shortlistedCount = allShortlisted != null ? allShortlisted.stream()
                            .filter(s -> s != null && s.getJobPost() != null && s.getJobPost().getId().equals(j.getId()))
                            .count() : 0;

                    long interviewsCount = allInterviews != null ? allInterviews.stream()
                            .filter(i -> i != null && i.getJobApplication() != null && i.getJobApplication().getJobPost() != null && 
                                         i.getJobApplication().getJobPost().getId().equals(j.getId()))
                            .count() : 0;

                    long selectedCount = allApps != null ? allApps.stream()
                            .filter(a -> a != null && a.getJobPost() != null && a.getJobPost().getId().equals(j.getId()) &&
                                         a.getFinalDecision() == ApplicationDecision.SELECTED)
                            .count() : 0;

                    return AdminJobDto.builder()
                            .id(j.getId())
                            .title(j.getTitle() != null ? j.getTitle() : "N/A")
                            .companyName(j.getCompanyName() != null ? j.getCompanyName() : "N/A")
                            .recruiterName(recruiterName)
                            .recruiterEmail(recruiterEmail)
                            .location(j.getLocation() != null ? j.getLocation() : "N/A")
                            .jobType(j.getJobType() != null ? j.getJobType() : "Full Time")
                            .status(j.getStatus() != null ? j.getStatus() : "DRAFT")
                            .createdAt(j.getCreatedAt())
                            .deadline(j.getDeadline())
                            .applicationsCount(appsCount)
                            .shortlistedCount(shortlistedCount)
                            .interviewsCount(interviewsCount)
                            .selectedCount(selectedCount)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<AdminApplicationDto> getApplications(String query, String statusFilter) {
        List<JobApplication> apps = jobApplicationRepository.findAll();
        List<Interview> allInterviews = interviewRepository.findAll();
        List<ResumeAnalysis> allAnalyses = resumeAnalysisRepository.findAll();

        Map<String, ResumeAnalysis> resumeJobAnalysisMap = new HashMap<>();
        Map<String, ResumeAnalysis> studentJobAnalysisMap = new HashMap<>();
        Map<Long, ResumeAnalysis> studentAnalysisMap = new HashMap<>();

        if (allAnalyses != null) {
            for (ResumeAnalysis ra : allAnalyses) {
                if (ra != null) {
                    if (ra.getResume() != null && ra.getJobPost() != null && ra.getResume().getId() != null && ra.getJobPost().getId() != null) {
                        String key = ra.getResume().getId() + "_" + ra.getJobPost().getId();
                        resumeJobAnalysisMap.put(key, ra);
                    }
                    if (ra.getStudentProfile() != null && ra.getJobPost() != null && ra.getStudentProfile().getId() != null && ra.getJobPost().getId() != null) {
                        String key = ra.getStudentProfile().getId() + "_" + ra.getJobPost().getId();
                        studentJobAnalysisMap.put(key, ra);
                    }
                    if (ra.getStudentProfile() != null && ra.getStudentProfile().getId() != null) {
                        studentAnalysisMap.put(ra.getStudentProfile().getId(), ra);
                    }
                }
            }
        }

        Map<Long, List<Interview>> appInterviewMap = new HashMap<>();
        if (allInterviews != null) {
            for (Interview i : allInterviews) {
                if (i != null && i.getJobApplication() != null && i.getJobApplication().getId() != null) {
                    appInterviewMap.computeIfAbsent(i.getJobApplication().getId(), k -> new ArrayList<>()).add(i);
                }
            }
        }

        String q = query != null ? query.trim().toLowerCase() : "";
        String sf = statusFilter != null ? statusFilter.trim().toUpperCase() : "";

        if (apps == null) return Collections.emptyList();

        return apps.stream()
                .filter(a -> a != null)
                .filter(a -> {
                    if (!q.isEmpty()) {
                        String candidateName = "";
                        String candidateEmail = "";
                        if (a.getStudentProfile() != null && a.getStudentProfile().getUser() != null) {
                            User u = a.getStudentProfile().getUser();
                            candidateName = ((u.getFirstName() != null ? u.getFirstName() : "") + " " + 
                                             (u.getLastName() != null ? u.getLastName() : "")).toLowerCase();
                            candidateEmail = u.getEmail() != null ? u.getEmail().toLowerCase() : "";
                        }
                        String jobTitle = a.getJobPost() != null && a.getJobPost().getTitle() != null ? a.getJobPost().getTitle().toLowerCase() : "";
                        if (!candidateName.contains(q) && !candidateEmail.contains(q) && !jobTitle.contains(q)) {
                            return false;
                        }
                    }
                    if (!sf.isEmpty()) {
                        String appStatus = a.getStatus() != null ? a.getStatus().toUpperCase() : "";
                        String finalDec = a.getFinalDecision() != null ? a.getFinalDecision().name().toUpperCase() : "";
                        if (!appStatus.equals(sf) && !finalDec.equals(sf)) {
                            return false;
                        }
                    }
                    return true;
                })
                .map(a -> {
                    String candidateName = "N/A";
                    String candidateEmail = "N/A";
                    if (a.getStudentProfile() != null && a.getStudentProfile().getUser() != null) {
                        User u = a.getStudentProfile().getUser();
                        candidateName = ((u.getFirstName() != null ? u.getFirstName() : "") + " " + 
                                         (u.getLastName() != null ? u.getLastName() : "")).trim();
                        if (candidateName.isEmpty()) candidateName = u.getUsername();
                        candidateEmail = u.getEmail() != null ? u.getEmail() : "N/A";
                    }

                    String jobTitle = a.getJobPost() != null && a.getJobPost().getTitle() != null ? a.getJobPost().getTitle() : "N/A";
                    String companyName = a.getJobPost() != null && a.getJobPost().getCompanyName() != null ? a.getJobPost().getCompanyName() : "N/A";

                    String recruiterName = "N/A";
                    if (a.getJobPost() != null && a.getJobPost().getRecruiter() != null && a.getJobPost().getRecruiter().getUser() != null) {
                        User rUser = a.getJobPost().getRecruiter().getUser();
                        recruiterName = ((rUser.getFirstName() != null ? rUser.getFirstName() : "") + " " + 
                                         (rUser.getLastName() != null ? rUser.getLastName() : "")).trim();
                        if (recruiterName.isEmpty()) recruiterName = rUser.getUsername();
                    }

                    // Fetch ATS Analysis from ResumeAnalysis entity (primary ATS source across application)
                    ResumeAnalysis analysis = null;
                    if (a.getResume() != null && a.getJobPost() != null && a.getResume().getId() != null && a.getJobPost().getId() != null) {
                        analysis = resumeJobAnalysisMap.get(a.getResume().getId() + "_" + a.getJobPost().getId());
                    }
                    if (analysis == null && a.getStudentProfile() != null && a.getJobPost() != null && a.getStudentProfile().getId() != null && a.getJobPost().getId() != null) {
                        analysis = studentJobAnalysisMap.get(a.getStudentProfile().getId() + "_" + a.getJobPost().getId());
                    }
                    if (analysis == null && a.getStudentProfile() != null && a.getStudentProfile().getId() != null) {
                        analysis = studentAnalysisMap.get(a.getStudentProfile().getId());
                    }

                    Double atsScoreVal = null;
                    String formattedAtsScore = "N/A";
                    if (analysis != null && analysis.getAtsScore() != null) {
                        atsScoreVal = analysis.getAtsScore();
                        formattedAtsScore = String.format(Locale.US, "%.1f%%", atsScoreVal);
                    } else if (a.getAtsScore() != null && a.getAtsScore().getOverallScore() != null) {
                        atsScoreVal = a.getAtsScore().getOverallScore().doubleValue();
                        formattedAtsScore = String.format(Locale.US, "%.1f%%", atsScoreVal);
                    }

                    String recommendation = "Standard Match";
                    if (atsScoreVal != null) {
                        if (atsScoreVal >= 85.0) recommendation = "Strong Match";
                        else if (atsScoreVal >= 70.0) recommendation = "Good Match";
                        else if (atsScoreVal >= 50.0) recommendation = "Moderate Match";
                        else recommendation = "Low Match";
                    }

                    List<Interview> appInterviews = appInterviewMap.getOrDefault(a.getId(), Collections.emptyList());
                    Interview latestInterview = appInterviews.isEmpty() ? null : appInterviews.get(appInterviews.size() - 1);

                    String intStatus = (latestInterview != null && latestInterview.getStatus() != null) ? latestInterview.getStatus().name() : "Not Scheduled";
                    String intResult = (latestInterview != null && latestInterview.getResult() != null) ? latestInterview.getResult().name() : "N/A";

                    return AdminApplicationDto.builder()
                            .id(a.getId())
                            .candidateName(candidateName)
                            .candidateEmail(candidateEmail)
                            .jobTitle(jobTitle)
                            .companyName(companyName)
                            .recruiterName(recruiterName)
                            .atsScore(atsScoreVal)
                            .formattedAtsScore(formattedAtsScore)
                            .recommendation(recommendation)
                            .applicationStatus(a.getStatus() != null ? a.getStatus() : "APPLIED")
                            .interviewStatus(intStatus)
                            .interviewResult(intResult)
                            .finalDecision(a.getFinalDecision() != null ? a.getFinalDecision().name() : "PENDING")
                            .appliedAt(a.getAppliedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public AdminReportDto getReports() {
        List<JobPost> rawJobs = jobPostRepository.findAll();
        List<JobApplication> rawApps = jobApplicationRepository.findAll();
        List<ShortlistedCandidate> rawShortlisted = shortlistedCandidateRepository.findAll();
        List<Interview> rawInterviews = interviewRepository.findAll();
        List<Recruiter> rawRecruiters = recruiterRepository.findAll();

        List<JobPost> jobs = rawJobs != null ? rawJobs : Collections.emptyList();
        List<JobApplication> apps = rawApps != null ? rawApps : Collections.emptyList();
        List<ShortlistedCandidate> shortlisted = rawShortlisted != null ? rawShortlisted : Collections.emptyList();
        List<Interview> interviews = rawInterviews != null ? rawInterviews : Collections.emptyList();
        List<Recruiter> recruiters = rawRecruiters != null ? rawRecruiters : Collections.emptyList();

        long totalJobs = jobs.size();
        long totalApplications = apps.size();
        long totalShortlisted = shortlisted.size();
        long totalInterviews = interviews.size();
        long completedInterviews = interviews.stream().filter(i -> i != null && i.getStatus() == InterviewStatus.COMPLETED).count();
        long selectedInterviewResults = interviews.stream().filter(i -> i != null && i.getResult() == InterviewResult.SELECTED).count();
        long rejectedInterviewResults = interviews.stream().filter(i -> i != null && i.getResult() == InterviewResult.REJECTED).count();

        long finalSelected = apps.stream().filter(a -> a != null && a.getFinalDecision() == ApplicationDecision.SELECTED).count();
        long finalRejected = apps.stream().filter(a -> a != null && a.getFinalDecision() == ApplicationDecision.REJECTED).count();
        long pendingFinalDecisions = apps.stream().filter(a -> a != null && (a.getFinalDecision() == ApplicationDecision.PENDING || a.getFinalDecision() == null)).count();

        AdminReportDto.RecruitmentSummary summary = AdminReportDto.RecruitmentSummary.builder()
                .totalJobs(totalJobs)
                .totalApplications(totalApplications)
                .totalShortlisted(totalShortlisted)
                .totalInterviews(totalInterviews)
                .completedInterviews(completedInterviews)
                .selectedInterviewResults(selectedInterviewResults)
                .rejectedInterviewResults(rejectedInterviewResults)
                .pendingFinalDecisions(pendingFinalDecisions)
                .finalSelected(finalSelected)
                .finalRejected(finalRejected)
                .build();

        long interviewedCount = interviews.stream()
                .filter(i -> i != null && i.getStatus() == InterviewStatus.COMPLETED)
                .map(i -> i.getJobApplication() != null ? i.getJobApplication().getId() : null)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        AdminReportDto.CandidatePipeline pipeline = AdminReportDto.CandidatePipeline.builder()
                .applicationsCount(totalApplications)
                .shortlistedCount(totalShortlisted)
                .interviewedCount(interviewedCount)
                .interviewSelectedCount(selectedInterviewResults)
                .finalSelectedCount(finalSelected)
                .finalRejectedCount(finalRejected)
                .build();

        final List<JobApplication> finalAppsList = apps;
        final List<ShortlistedCandidate> finalShortlistedList = shortlisted;
        final List<Interview> finalInterviewsList = interviews;

        List<AdminReportDto.JobReportSummary> jobSummaries = jobs.stream().filter(j -> j != null).map(j -> {
            String recruiterName = "N/A";
            if (j.getRecruiter() != null && j.getRecruiter().getUser() != null) {
                User u = j.getRecruiter().getUser();
                recruiterName = ((u.getFirstName() != null ? u.getFirstName() : "") + " " + 
                                 (u.getLastName() != null ? u.getLastName() : "")).trim();
                if (recruiterName.isEmpty()) recruiterName = u.getUsername();
            }

            long jApps = finalAppsList.stream().filter(a -> a != null && a.getJobPost() != null && a.getJobPost().getId().equals(j.getId())).count();
            long jShortlisted = finalShortlistedList.stream().filter(s -> s != null && s.getJobPost() != null && s.getJobPost().getId().equals(j.getId())).count();
            long jInterviews = finalInterviewsList.stream().filter(i -> i != null && i.getJobApplication() != null && i.getJobApplication().getJobPost() != null &&
                                                               i.getJobApplication().getJobPost().getId().equals(j.getId())).count();
            long jCompletedInt = finalInterviewsList.stream().filter(i -> i != null && i.getJobApplication() != null && i.getJobApplication().getJobPost() != null &&
                                                                 i.getJobApplication().getJobPost().getId().equals(j.getId()) &&
                                                                 i.getStatus() == InterviewStatus.COMPLETED).count();
            long jIntSelected = finalInterviewsList.stream().filter(i -> i != null && i.getJobApplication() != null && i.getJobApplication().getJobPost() != null &&
                                                                i.getJobApplication().getJobPost().getId().equals(j.getId()) &&
                                                                i.getResult() == InterviewResult.SELECTED).count();
            long jFinalSelected = finalAppsList.stream().filter(a -> a != null && a.getJobPost() != null && a.getJobPost().getId().equals(j.getId()) &&
                                                            a.getFinalDecision() == ApplicationDecision.SELECTED).count();
            long jFinalRejected = finalAppsList.stream().filter(a -> a != null && a.getJobPost() != null && a.getJobPost().getId().equals(j.getId()) &&
                                                            a.getFinalDecision() == ApplicationDecision.REJECTED).count();

            return AdminReportDto.JobReportSummary.builder()
                    .jobId(j.getId())
                    .jobTitle(j.getTitle() != null ? j.getTitle() : "N/A")
                    .recruiterName(recruiterName)
                    .companyName(j.getCompanyName() != null ? j.getCompanyName() : "N/A")
                    .applicationsCount(jApps)
                    .shortlistedCount(jShortlisted)
                    .interviewsCount(jInterviews)
                    .completedInterviewsCount(jCompletedInt)
                    .interviewSelectedCount(jIntSelected)
                    .finalSelectedCount(jFinalSelected)
                    .finalRejectedCount(jFinalRejected)
                    .build();
        }).collect(Collectors.toList());

        List<AdminReportDto.RecruiterReportSummary> recruiterSummaries = recruiters.stream().filter(r -> r != null).map(r -> {
            User u = r.getUser();
            String name = u != null ? ((u.getFirstName() != null ? u.getFirstName() : "") + " " + 
                                       (u.getLastName() != null ? u.getLastName() : "")).trim() : "N/A";
            if (name.isEmpty() && u != null) name = u.getUsername();

            long rJobs = jobs.stream().filter(j -> j != null && j.getRecruiter() != null && j.getRecruiter().getId().equals(r.getId())).count();
            long rApps = finalAppsList.stream().filter(a -> a != null && a.getJobPost() != null && a.getJobPost().getRecruiter() != null &&
                                                   a.getJobPost().getRecruiter().getId().equals(r.getId())).count();
            long rInterviews = finalInterviewsList.stream().filter(i -> i != null && i.getRecruiter() != null && i.getRecruiter().getId().equals(r.getId())).count();
            long rCompletedInt = finalInterviewsList.stream().filter(i -> i != null && i.getRecruiter() != null && i.getRecruiter().getId().equals(r.getId()) &&
                                                                 i.getStatus() == InterviewStatus.COMPLETED).count();
            long rSelected = finalAppsList.stream().filter(a -> a != null && a.getJobPost() != null && a.getJobPost().getRecruiter() != null &&
                                                        a.getJobPost().getRecruiter().getId().equals(r.getId()) &&
                                                        a.getFinalDecision() == ApplicationDecision.SELECTED).count();
            long rRejected = finalAppsList.stream().filter(a -> a != null && a.getJobPost() != null && a.getJobPost().getRecruiter() != null &&
                                                        a.getJobPost().getRecruiter().getId().equals(r.getId()) &&
                                                        a.getFinalDecision() == ApplicationDecision.REJECTED).count();

            return AdminReportDto.RecruiterReportSummary.builder()
                    .recruiterId(r.getId())
                    .recruiterName(name)
                    .companyName(r.getCompanyName() != null ? r.getCompanyName() : "N/A")
                    .jobsCount(rJobs)
                    .applicationsCount(rApps)
                    .interviewsCount(rInterviews)
                    .completedInterviewsCount(rCompletedInt)
                    .finalSelectedCount(rSelected)
                    .finalRejectedCount(rRejected)
                    .build();
        }).collect(Collectors.toList());

        return AdminReportDto.builder()
                .recruitmentSummary(summary)
                .candidatePipeline(pipeline)
                .jobSummaries(jobSummaries)
                .recruiterSummaries(recruiterSummaries)
                .build();
    }
}
