package com.resume.screening.controller;

import com.resume.screening.dto.ResumeDto;
import com.resume.screening.entity.Resume;
import com.resume.screening.security.CustomUserDetails;
import com.resume.screening.service.ResumeStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.resume.screening.dto.ParsedResumeDto;
import com.resume.screening.dto.ResumeAnalysisDto;
import com.resume.screening.dto.JobPostDto;
import com.resume.screening.service.ResumeParsingService;
import com.resume.screening.service.ResumeAnalysisService;
import com.resume.screening.service.JobService;

@Controller
@RequestMapping("/student/resume")
public class StudentResumeController {

    private final ResumeStorageService resumeStorageService;
    private final ResumeParsingService resumeParsingService;
    private final ResumeAnalysisService resumeAnalysisService;
    private final JobService jobService;
    private final com.resume.screening.service.UserResolutionService userResolutionService;

    public StudentResumeController(ResumeStorageService resumeStorageService,
                                  ResumeParsingService resumeParsingService,
                                  ResumeAnalysisService resumeAnalysisService,
                                  JobService jobService,
                                  com.resume.screening.service.UserResolutionService userResolutionService) {
        this.resumeStorageService = resumeStorageService;
        this.resumeParsingService = resumeParsingService;
        this.resumeAnalysisService = resumeAnalysisService;
        this.jobService = jobService;
        this.userResolutionService = userResolutionService;
    }

    private com.resume.screening.entity.User resolveAuthenticatedUser(org.springframework.security.core.Authentication authentication) {
        return userResolutionService.resolveAuthenticatedUser(authentication);
    }

    @GetMapping
    public String viewResumePage(Model model, org.springframework.security.core.Authentication authentication) {
        com.resume.screening.entity.User user = resolveAuthenticatedUser(authentication);
        ResumeDto resume = resumeStorageService.getCurrentResume(user.getUsername());
        model.addAttribute("resume", resume);
        
        if (resume != null) {
            ParsedResumeDto parsedResume = resumeParsingService.getParsedResumeByResumeId(resume.getId());
            model.addAttribute("parsedResume", parsedResume);
        }
        
        return "student/resume";
    }

    @GetMapping("/upload")
    public String showUploadForm(Model model, org.springframework.security.core.Authentication authentication) {
        com.resume.screening.entity.User user = resolveAuthenticatedUser(authentication);
        ResumeDto currentResume = resumeStorageService.getCurrentResume(user.getUsername());
        model.addAttribute("currentResume", currentResume);
        return "student/resume-upload";
    }

    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam("file") MultipartFile file,
                                   org.springframework.security.core.Authentication authentication,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        com.resume.screening.entity.User user = resolveAuthenticatedUser(authentication);
        try {
            resumeStorageService.uploadResume(user.getUsername(), file);
            redirectAttributes.addFlashAttribute("successMessage", "Resume uploaded successfully!");
            return "redirect:/student/resume";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            ResumeDto currentResume = resumeStorageService.getCurrentResume(user.getUsername());
            model.addAttribute("currentResume", currentResume);
            return "student/resume-upload";
        } catch (Exception ex) {
            model.addAttribute("errorMessage", "An error occurred while uploading file: " + ex.getMessage());
            ResumeDto currentResume = resumeStorageService.getCurrentResume(user.getUsername());
            model.addAttribute("currentResume", currentResume);
            return "student/resume-upload";
        }
    }

    @GetMapping("/view/{id}")
    public ResponseEntity<Resource> viewResume(@PathVariable("id") Long id,
                                               org.springframework.security.core.Authentication authentication) {
        com.resume.screening.entity.User user = resolveAuthenticatedUser(authentication);
        Resume resume = resumeStorageService.getResumeEntityWithOwnershipCheck(user.getUsername(), id);
        Resource resource = resumeStorageService.loadResumeFile(user.getUsername(), id);

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

    @GetMapping("/view-version/{versionId}")
    public ResponseEntity<Resource> viewResumeVersion(@PathVariable("versionId") Long versionId,
                                                      org.springframework.security.core.Authentication authentication) {
        com.resume.screening.entity.User user = resolveAuthenticatedUser(authentication);
        Resource resource = resumeStorageService.loadResumeVersionFile(user.getUsername(), versionId);
        String filename = resource.getFilename() != null ? resource.getFilename() : "resume-version";
        
        if (filename.toLowerCase().endsWith(".pdf")) {
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(resource);
        } else {
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(resource);
        }
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadResume(@PathVariable("id") Long id,
                                                    org.springframework.security.core.Authentication authentication) {
        com.resume.screening.entity.User user = resolveAuthenticatedUser(authentication);
        Resume resume = resumeStorageService.getResumeEntityWithOwnershipCheck(user.getUsername(), id);
        Resource resource = resumeStorageService.loadResumeFile(user.getUsername(), id);
        String originalName = resume.getOriginalFileName() != null ? resume.getOriginalFileName() : resume.getFileName();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + originalName + "\"")
                .body(resource);
    }

    @PostMapping("/delete/{id}")
    public String deleteResume(@PathVariable("id") Long id,
                               org.springframework.security.core.Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        try {
            com.resume.screening.entity.User user = resolveAuthenticatedUser(authentication);
            resumeStorageService.deleteResume(user.getUsername(), id);
            redirectAttributes.addFlashAttribute("successMessage", "Resume deleted successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/student/resume";
    }

    @PostMapping("/{id}/parse")
    public String parseResume(@PathVariable("id") Long id,
                              org.springframework.security.core.Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        try {
            com.resume.screening.entity.User user = resolveAuthenticatedUser(authentication);
            // Ownership validation
            resumeStorageService.getResumeEntityWithOwnershipCheck(user.getUsername(), id);
            ParsedResumeDto parsed = resumeParsingService.parseAndSaveResume(id);
            if ("PARSED".equals(parsed.getParsingStatus())) {
                redirectAttributes.addFlashAttribute("successMessage", "Resume text and information extracted successfully!");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Resume parsing failed: " + (parsed.getErrorMessage() != null ? parsed.getErrorMessage() : "Unknown error"));
            }
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error parsing resume: " + ex.getMessage());
        }
        return "redirect:/student/resume";
    }

    @GetMapping("/{id}/parsed")
    @ResponseBody
    public ResponseEntity<ParsedResumeDto> getParsedData(@PathVariable("id") Long id,
                                                         org.springframework.security.core.Authentication authentication) {
        com.resume.screening.entity.User user = resolveAuthenticatedUser(authentication);
        // Ownership validation
        resumeStorageService.getResumeEntityWithOwnershipCheck(user.getUsername(), id);
        ParsedResumeDto parsed = resumeParsingService.getParsedResumeByResumeId(id);
        if (parsed == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(parsed);
    }

    // Student ATS Analysis endpoints
    @PostMapping("/{resumeId}/analyze/{jobId}")
    public String analyzeResumeForJob(@PathVariable("resumeId") Long resumeId,
                                      @PathVariable("jobId") Long jobId,
                                      org.springframework.security.core.Authentication authentication,
                                      RedirectAttributes redirectAttributes) {
        try {
            com.resume.screening.entity.User user = resolveAuthenticatedUser(authentication);
            resumeAnalysisService.analyzeResumeForJob(resumeId, jobId, user.getUsername());
            redirectAttributes.addFlashAttribute("successMessage", "ATS Analysis completed successfully!");
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Could not analyze resume: " + ex.getMessage());
        }
        return "redirect:/student/resume/" + resumeId + "/analysis/" + jobId;
    }

    @GetMapping("/{resumeId}/analysis/{jobId}")
    public String viewResumeAnalysis(@PathVariable("resumeId") Long resumeId,
                                     @PathVariable("jobId") Long jobId,
                                     Model model,
                                     org.springframework.security.core.Authentication authentication,
                                     RedirectAttributes redirectAttributes) {
        try {
            com.resume.screening.entity.User user = resolveAuthenticatedUser(authentication);
            ResumeDto currentResume = resumeStorageService.getCurrentResume(user.getUsername());
            ResumeAnalysisDto analysis = resumeAnalysisService.getAnalysisResult(resumeId, jobId, user.getUsername());
            if (analysis == null) {
                analysis = resumeAnalysisService.analyzeResumeForJob(resumeId, jobId, user.getUsername());
            }
            JobPostDto job = jobService.getJobById(jobId);
            model.addAttribute("hasResume", true);
            model.addAttribute("isParsed", true);
            model.addAttribute("currentResume", currentResume);
            model.addAttribute("analysis", analysis);
            model.addAttribute("job", job);
            return "student/analysis";
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/student/jobs/" + jobId;
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Could not load analysis: " + ex.getMessage());
            return "redirect:/student/jobs/" + jobId;
        }
    }
}
