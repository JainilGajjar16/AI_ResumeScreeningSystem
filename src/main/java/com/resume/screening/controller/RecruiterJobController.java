package com.resume.screening.controller;

import com.resume.screening.dto.CandidateRecommendationDto;
import com.resume.screening.dto.JobApplicationDto;
import com.resume.screening.dto.JobPostDto;
import com.resume.screening.dto.ParsedJobRequirementDto;
import com.resume.screening.dto.ResumeAnalysisDto;
import com.resume.screening.dto.ShortlistedCandidateDto;
import com.resume.screening.entity.ParsedResume;
import com.resume.screening.entity.Resume;
import com.resume.screening.repository.ParsedResumeRepository;
import com.resume.screening.service.CandidateRecommendationService;
import com.resume.screening.service.JobApplicationService;
import com.resume.screening.service.JobDescriptionParsingService;
import com.resume.screening.service.JobService;
import com.resume.screening.service.ResumeAnalysisService;
import com.resume.screening.service.ResumeStorageService;
import com.resume.screening.service.ShortlistService;
import com.resume.screening.service.UserResolutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/recruiter")
public class RecruiterJobController {

    private static final Logger log = LoggerFactory.getLogger(RecruiterJobController.class);

    private final JobService jobService;
    private final JobApplicationService jobApplicationService;
    private final ResumeStorageService resumeStorageService;
    private final JobDescriptionParsingService jobDescriptionParsingService;
    private final ResumeAnalysisService resumeAnalysisService;
    private final UserResolutionService userResolutionService;
    private final CandidateRecommendationService candidateRecommendationService;
    private final ShortlistService shortlistService;
    private final ParsedResumeRepository parsedResumeRepository;

    public RecruiterJobController(JobService jobService,
                                  JobApplicationService jobApplicationService,
                                  ResumeStorageService resumeStorageService,
                                  JobDescriptionParsingService jobDescriptionParsingService,
                                  ResumeAnalysisService resumeAnalysisService,
                                  UserResolutionService userResolutionService,
                                  CandidateRecommendationService candidateRecommendationService,
                                  ShortlistService shortlistService,
                                  ParsedResumeRepository parsedResumeRepository) {
        this.jobService = jobService;
        this.jobApplicationService = jobApplicationService;
        this.resumeStorageService = resumeStorageService;
        this.jobDescriptionParsingService = jobDescriptionParsingService;
        this.resumeAnalysisService = resumeAnalysisService;
        this.userResolutionService = userResolutionService;
        this.candidateRecommendationService = candidateRecommendationService;
        this.shortlistService = shortlistService;
        this.parsedResumeRepository = parsedResumeRepository;
    }

    private com.resume.screening.entity.User resolveAuthenticatedUser(org.springframework.security.core.Authentication authentication) {
        return userResolutionService.resolveAuthenticatedUser(authentication);
    }

    // 1. My Jobs List
    @GetMapping("/jobs")
    public String myJobs(Model model, org.springframework.security.core.Authentication authentication) {
        com.resume.screening.entity.User user = resolveAuthenticatedUser(authentication);
        List<JobPostDto> jobs = jobService.getJobsByRecruiter(user.getUsername());
        model.addAttribute("jobs", jobs);
        return "recruiter/jobs";
    }

    // 2. GET Create Job Form
    @GetMapping("/jobs/create")
    public String showCreateForm(Model model, org.springframework.security.core.Authentication authentication) {
        model.addAttribute("job", new JobPostDto());
        return "recruiter/job-create";
    }

    // 3. POST Save New Job
    @PostMapping("/jobs")
    public String saveJob(@ModelAttribute("job") JobPostDto dto,
                          org.springframework.security.core.Authentication authentication,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        try {
            com.resume.screening.entity.User user = resolveAuthenticatedUser(authentication);
            JobPostDto created = jobService.createJob(dto, user.getUsername());
            redirectAttributes.addFlashAttribute("successMessage", "Job posting created successfully with status: " + created.getStatus());
            return "redirect:/recruiter/jobs";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("job", dto);
            return "recruiter/job-create";
        } catch (Exception ex) {
            model.addAttribute("errorMessage", "An error occurred while creating job: " + ex.getMessage());
            model.addAttribute("job", dto);
            return "recruiter/job-create";
        }
    }

    // 4. GET Edit Job Form
    @GetMapping("/jobs/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id,
                               Model model,
                               org.springframework.security.core.Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        try {
            com.resume.screening.entity.User user = resolveAuthenticatedUser(authentication);
            JobPostDto job = jobService.getJobForRecruiter(id, user.getUsername());
            model.addAttribute("job", job);
            return "recruiter/job-edit";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/recruiter/jobs";
        }
    }

    // 5. POST Update Job
    @PostMapping("/jobs/update/{id}")
    public String updateJob(@PathVariable("id") Long id,
                            @ModelAttribute("job") JobPostDto dto,
                            org.springframework.security.core.Authentication authentication,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        try {
            com.resume.screening.entity.User user = resolveAuthenticatedUser(authentication);
            jobService.updateJob(id, dto, user.getUsername());
            redirectAttributes.addFlashAttribute("successMessage", "Job posting updated successfully.");
            return "redirect:/recruiter/jobs";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            dto.setId(id);
            model.addAttribute("job", dto);
            return "recruiter/job-edit";
        } catch (Exception ex) {
            model.addAttribute("errorMessage", "An error occurred while updating job: " + ex.getMessage());
            dto.setId(id);
            model.addAttribute("job", dto);
            return "recruiter/job-edit";
        }
    }

    // 6. POST Publish Job
    @PostMapping("/jobs/publish/{id}")
    public String publishJob(@PathVariable("id") Long id,
                             org.springframework.security.core.Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        try {
            com.resume.screening.entity.User user = resolveAuthenticatedUser(authentication);
            jobService.publishJob(id, user.getUsername());
            redirectAttributes.addFlashAttribute("successMessage", "Job has been published and is now visible to candidates!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/recruiter/jobs";
    }

    // 7. POST Close Job
    @PostMapping("/jobs/close/{id}")
    public String closeJob(@PathVariable("id") Long id,
                           org.springframework.security.core.Authentication authentication,
                           RedirectAttributes redirectAttributes) {
        try {
            com.resume.screening.entity.User user = resolveAuthenticatedUser(authentication);
            jobService.closeJob(id, user.getUsername());
            redirectAttributes.addFlashAttribute("successMessage", "Job posting has been closed. New applications are disabled.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/recruiter/jobs";
    }

    // 8. GET View Job Details (Recruiter View)
    @GetMapping("/jobs/{id}")
    public String viewJobDetails(@PathVariable("id") Long id,
                                Model model,
                                org.springframework.security.core.Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        try {
            com.resume.screening.entity.User user = resolveAuthenticatedUser(authentication);
            JobPostDto job = jobService.getJobForRecruiter(id, user.getUsername());
            List<JobApplicationDto> applicants = jobApplicationService.getApplicantsForJob(id, user.getUsername());
            ParsedJobRequirementDto parsedJob = jobDescriptionParsingService.getParsedJobRequirementByJobId(id);
            
            model.addAttribute("job", job);
            model.addAttribute("applicants", applicants);
            model.addAttribute("parsedJob", parsedJob);
            return "recruiter/job-details";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/recruiter/jobs";
        }
    }

    // 8a. POST Parse Job Requirements
    @PostMapping("/jobs/{id}/parse")
    public String parseJobRequirements(@PathVariable("id") Long id,
                                      org.springframework.security.core.Authentication authentication,
                                      RedirectAttributes redirectAttributes) {
        try {
            com.resume.screening.entity.User user = resolveAuthenticatedUser(authentication);
            // Ownership check
            jobService.getJobForRecruiter(id, user.getUsername());
            jobDescriptionParsingService.parseAndSaveJobRequirement(id);
            redirectAttributes.addFlashAttribute("successMessage", "Job requirements extracted and parsed successfully!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error parsing job requirements: " + ex.getMessage());
        }
        return "redirect:/recruiter/jobs/" + id;
    }

    // 8b. GET Parsed Job Data
    @GetMapping("/jobs/{id}/parsed")
    @ResponseBody
    public ResponseEntity<ParsedJobRequirementDto> getParsedJobData(@PathVariable("id") Long id,
                                                                    org.springframework.security.core.Authentication authentication) {
        com.resume.screening.entity.User user = resolveAuthenticatedUser(authentication);
        // Ownership check
        jobService.getJobForRecruiter(id, user.getUsername());
        ParsedJobRequirementDto parsed = jobDescriptionParsingService.getParsedJobRequirementByJobId(id);
        if (parsed == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(parsed);
    }

    // 9. GET View Job Applicants & Recommendations (Module 7)
    @GetMapping("/jobs/{id}/applicants")
    public String viewJobApplicants(@PathVariable("id") Long id,
                                     Model model,
                                     org.springframework.security.core.Authentication authentication,
                                     RedirectAttributes redirectAttributes) {
        try {
            com.resume.screening.entity.User user = resolveAuthenticatedUser(authentication);
            JobPostDto job = jobService.getJobForRecruiter(id, user.getUsername());
            List<JobApplicationDto> applicants = jobApplicationService.getApplicantsForJob(id, user.getUsername());
            List<CandidateRecommendationDto> recommendations = candidateRecommendationService.getRankedCandidatesForJob(id, user.getUsername());
            
            model.addAttribute("job", job);
            model.addAttribute("applicants", applicants);
            model.addAttribute("recommendations", recommendations);
            return "recruiter/applicants";
        } catch (Exception ex) {
            log.error("Error loading applicants for job ID {}: {}", id, ex.getMessage(), ex);
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/recruiter/jobs";
        }
    }

    // 9a. POST Shortlist Candidate
    @PostMapping("/jobs/{jobId}/shortlist/{studentProfileId}")
    public String shortlistCandidate(@PathVariable("jobId") Long jobId,
                                     @PathVariable("studentProfileId") Long studentProfileId,
                                     org.springframework.security.core.Authentication authentication,
                                     RedirectAttributes redirectAttributes) {
        try {
            com.resume.screening.entity.User user = resolveAuthenticatedUser(authentication);
            shortlistService.shortlistCandidate(jobId, studentProfileId, user.getUsername());
            redirectAttributes.addFlashAttribute("successMessage", "Candidate shortlisted successfully!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/recruiter/jobs/" + jobId + "/applicants";
    }

    // 9b. POST Unshortlist Candidate
    @PostMapping("/jobs/{jobId}/unshortlist/{studentProfileId}")
    public String unshortlistCandidate(@PathVariable("jobId") Long jobId,
                                       @PathVariable("studentProfileId") Long studentProfileId,
                                       org.springframework.security.core.Authentication authentication,
                                       RedirectAttributes redirectAttributes) {
        try {
            com.resume.screening.entity.User user = resolveAuthenticatedUser(authentication);
            shortlistService.removeCandidateFromShortlist(jobId, studentProfileId, user.getUsername());
            redirectAttributes.addFlashAttribute("successMessage", "Candidate removed from shortlist.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/recruiter/jobs/" + jobId + "/applicants";
    }

    // 9c. GET Shortlisted Candidates Page
    @GetMapping("/shortlisted")
    public String viewShortlistedCandidates(Model model,
                                            org.springframework.security.core.Authentication authentication,
                                            RedirectAttributes redirectAttributes) {
        try {
            com.resume.screening.entity.User user = resolveAuthenticatedUser(authentication);
            List<ShortlistedCandidateDto> shortlisted = shortlistService.getShortlistedCandidatesForRecruiter(user.getUsername());
            model.addAttribute("shortlistedCandidates", shortlisted);
            return "recruiter/shortlisted";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/recruiter/dashboard";
        }
    }

    // 9d. POST Remove Shortlist by ID (Shortlisted page action)
    @PostMapping("/shortlisted/remove/{id}")
    public String removeShortlistRecord(@PathVariable("id") Long id,
                                        org.springframework.security.core.Authentication authentication,
                                        RedirectAttributes redirectAttributes) {
        try {
            com.resume.screening.entity.User user = resolveAuthenticatedUser(authentication);
            shortlistService.removeShortlistById(id, user.getUsername());
            redirectAttributes.addFlashAttribute("successMessage", "Candidate removed from shortlist.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/recruiter/shortlisted";
    }

    // 10. GET View Candidate Profile & ATS Details (Recruiter View)
    @GetMapping("/candidate/{studentProfileId}")
    public String viewCandidateProfile(@PathVariable("studentProfileId") Long studentProfileId,
                                       Model model,
                                       org.springframework.security.core.Authentication authentication,
                                       RedirectAttributes redirectAttributes) {
        try {
            com.resume.screening.entity.User user = resolveAuthenticatedUser(authentication);
            JobApplicationDto candidate = jobApplicationService.getApplicantProfileForRecruiter(studentProfileId, user.getUsername());
            
            ResumeAnalysisDto analysis = null;
            if (candidate.getResumeId() != null && candidate.getJobId() != null) {
                try {
                    analysis = resumeAnalysisService.getAnalysisResult(candidate.getResumeId(), candidate.getJobId(), user.getUsername());
                } catch (Exception ignored) {
                }
            }

            ParsedResume parsedResume = null;
            if (candidate.getResumeId() != null) {
                parsedResume = parsedResumeRepository.findByResumeId(candidate.getResumeId()).orElse(null);
            }

            boolean isShortlisted = shortlistService.isCandidateShortlisted(candidate.getJobId(), studentProfileId);

            model.addAttribute("candidate", candidate);
            model.addAttribute("analysis", analysis);
            model.addAttribute("parsedResume", parsedResume);
            model.addAttribute("isShortlisted", isShortlisted);
            return "recruiter/candidate-profile";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/recruiter/jobs";
        }
    }

    // 11. GET View Candidate Resume (Inline)
    @GetMapping("/resume/view/{resumeId}")
    public ResponseEntity<Resource> viewCandidateResume(@PathVariable("resumeId") Long resumeId,
                                                        org.springframework.security.core.Authentication authentication) {
        com.resume.screening.entity.User user = resolveAuthenticatedUser(authentication);
        Resume resume = resumeStorageService.getResumeEntityForRecruiter(user.getUsername(), resumeId);
        Resource resource = resumeStorageService.loadResumeFileForRecruiter(user.getUsername(), resumeId);

        String originalName = resume.getOriginalFileName() != null ? resume.getOriginalFileName() : resume.getFileName();
        String fileType = resume.getFileType() != null ? resume.getFileType().toLowerCase() : "";

        if ("pdf".equals(fileType) || originalName.toLowerCase().endsWith(".pdf")) {
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + originalName + "\"")
                    .body(resource);
        } else {
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + originalName + "\"")
                    .body(resource);
        }
    }

    // 12. GET Download Candidate Resume
    @GetMapping("/resume/download/{resumeId}")
    public ResponseEntity<Resource> downloadCandidateResume(@PathVariable("resumeId") Long resumeId,
                                                            org.springframework.security.core.Authentication authentication) {
        com.resume.screening.entity.User user = resolveAuthenticatedUser(authentication);
        Resume resume = resumeStorageService.getResumeEntityForRecruiter(user.getUsername(), resumeId);
        Resource resource = resumeStorageService.loadResumeFileForRecruiter(user.getUsername(), resumeId);
        String originalName = resume.getOriginalFileName() != null ? resume.getOriginalFileName() : resume.getFileName();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + originalName + "\"")
                .body(resource);
    }

    // 13. POST Analyze Candidate Resume for Job
    @PostMapping("/jobs/{jobId}/analyze/{resumeId}")
    public String analyzeCandidateResume(@PathVariable("jobId") Long jobId,
                                         @PathVariable("resumeId") Long resumeId,
                                         org.springframework.security.core.Authentication authentication,
                                         RedirectAttributes redirectAttributes) {
        try {
            com.resume.screening.entity.User user = resolveAuthenticatedUser(authentication);
            resumeAnalysisService.analyzeResumeForJob(resumeId, jobId, user.getUsername());
            redirectAttributes.addFlashAttribute("successMessage", "ATS Resume Analysis completed successfully!");
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Analysis error: " + ex.getMessage());
        }
        return "redirect:/recruiter/jobs/" + jobId + "/analysis/" + resumeId;
    }

    // 14. GET View Candidate ATS Analysis
    @GetMapping("/jobs/{jobId}/analysis/{resumeId}")
    public String viewCandidateAnalysis(@PathVariable("jobId") Long jobId,
                                        @PathVariable("resumeId") Long resumeId,
                                        Model model,
                                        org.springframework.security.core.Authentication authentication,
                                        RedirectAttributes redirectAttributes) {
        try {
            com.resume.screening.entity.User user = resolveAuthenticatedUser(authentication);
            ResumeAnalysisDto analysis = resumeAnalysisService.getAnalysisResult(resumeId, jobId, user.getUsername());
            if (analysis == null) {
                // Auto-trigger analysis if not existing yet
                analysis = resumeAnalysisService.analyzeResumeForJob(resumeId, jobId, user.getUsername());
            }
            JobPostDto job = jobService.getJobForRecruiter(jobId, user.getUsername());
            model.addAttribute("analysis", analysis);
            model.addAttribute("job", job);
            return "recruiter/analysis";
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/recruiter/jobs/" + jobId + "/applicants";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Could not load analysis: " + ex.getMessage());
            return "redirect:/recruiter/jobs/" + jobId + "/applicants";
        }
    }
}
