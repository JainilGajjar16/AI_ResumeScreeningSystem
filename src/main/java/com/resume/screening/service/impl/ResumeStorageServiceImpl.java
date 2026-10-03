package com.resume.screening.service.impl;

import com.resume.screening.dto.ResumeDto;
import com.resume.screening.dto.ResumeVersionDto;
import com.resume.screening.entity.Resume;
import com.resume.screening.entity.ResumeVersion;
import com.resume.screening.entity.StudentProfile;
import com.resume.screening.entity.User;
import com.resume.screening.entity.Recruiter;
import com.resume.screening.repository.JobApplicationRepository;
import com.resume.screening.repository.RecruiterRepository;
import com.resume.screening.repository.ResumeRepository;
import com.resume.screening.repository.ResumeVersionRepository;
import com.resume.screening.repository.StudentProfileRepository;
import com.resume.screening.repository.UserRepository;
import com.resume.screening.service.ResumeStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ResumeStorageServiceImpl implements ResumeStorageService {

    private final Path fileStorageLocation;
    private final ResumeRepository resumeRepository;
    private final ResumeVersionRepository resumeVersionRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final UserRepository userRepository;
    private final RecruiterRepository recruiterRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final com.resume.screening.repository.ResumeAnalysisRepository resumeAnalysisRepository;
    private final com.resume.screening.service.ResumeParsingService resumeParsingService;
    private final com.resume.screening.service.UserResolutionService userResolutionService;

    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public ResumeStorageServiceImpl(@Value("${app.file.upload-dir:uploads/resumes}") String uploadDir,
                                    ResumeRepository resumeRepository,
                                    ResumeVersionRepository resumeVersionRepository,
                                    StudentProfileRepository studentProfileRepository,
                                    UserRepository userRepository,
                                    RecruiterRepository recruiterRepository,
                                    JobApplicationRepository jobApplicationRepository,
                                    com.resume.screening.repository.ResumeAnalysisRepository resumeAnalysisRepository,
                                    com.resume.screening.service.ResumeParsingService resumeParsingService) {
        this(uploadDir, resumeRepository, resumeVersionRepository, studentProfileRepository, userRepository,
                recruiterRepository, jobApplicationRepository, resumeAnalysisRepository, resumeParsingService, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public ResumeStorageServiceImpl(@Value("${app.file.upload-dir:uploads/resumes}") String uploadDir,
                                    ResumeRepository resumeRepository,
                                    ResumeVersionRepository resumeVersionRepository,
                                    StudentProfileRepository studentProfileRepository,
                                    UserRepository userRepository,
                                    RecruiterRepository recruiterRepository,
                                    JobApplicationRepository jobApplicationRepository,
                                    com.resume.screening.repository.ResumeAnalysisRepository resumeAnalysisRepository,
                                    com.resume.screening.service.ResumeParsingService resumeParsingService,
                                    com.resume.screening.service.UserResolutionService userResolutionService) {
        this.resumeRepository = resumeRepository;
        this.resumeVersionRepository = resumeVersionRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.userRepository = userRepository;
        this.recruiterRepository = recruiterRepository;
        this.jobApplicationRepository = jobApplicationRepository;
        this.resumeAnalysisRepository = resumeAnalysisRepository;
        this.resumeParsingService = resumeParsingService;
        this.userResolutionService = userResolutionService != null ? userResolutionService :
                new com.resume.screening.service.impl.UserResolutionServiceImpl(userRepository, studentProfileRepository, recruiterRepository);

        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create directory for uploaded resumes: " + uploadDir, ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResumeDto getCurrentResume(String usernameOrEmail) {
        User user = findUser(usernameOrEmail);
        Optional<StudentProfile> profileOpt = studentProfileRepository.findByUserId(user.getId());
        if (profileOpt.isEmpty()) {
            return null;
        }

        Optional<Resume> resumeOpt = resumeRepository.findByStudentProfileIdAndIsCurrentTrue(profileOpt.get().getId());
        if (resumeOpt.isEmpty()) {
            return null;
        }

        return mapToDto(resumeOpt.get());
    }

    @Override
    @Transactional
    public ResumeDto uploadResume(String usernameOrEmail, MultipartFile file) {
        User user = findUser(usernameOrEmail);
        StudentProfile profile = getOrCreateStudentProfile(user);

        // Validation 1: Null or Empty File
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please select a file to upload.");
        }

        // Validation 2: File Size <= 10 MB
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("Maximum file size is 10 MB.");
        }

        // Validation 3: Extension & Mime Type (PDF and DOCX only)
        String rawFileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        if (rawFileName.contains("..")) {
            throw new IllegalArgumentException("Filename contains invalid path sequence: " + rawFileName);
        }

        String extension = getFileExtension(rawFileName);
        if (!extension.equals("pdf") && !extension.equals("docx")) {
            throw new IllegalArgumentException("Only PDF and DOCX files are allowed.");
        }

        String contentType = file.getContentType();
        if (contentType != null) {
            String lowerType = contentType.toLowerCase();
            boolean isPdfMime = lowerType.contains("pdf");
            boolean isDocxMime = lowerType.contains("wordprocessingml") || lowerType.contains("msword") || lowerType.contains("docx");
            if (!isPdfMime && !isDocxMime && !lowerType.equals("application/octet-stream")) {
                throw new IllegalArgumentException("Only PDF and DOCX files are allowed.");
            }
        }

        // Store file safely
        String storedFileName = UUID.randomUUID().toString() + "_" + rawFileName;
        Path targetLocation = this.fileStorageLocation.resolve(storedFileName);

        try {
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + rawFileName + ". Please try again!", ex);
        }

        // Database Entity & Version Handling
        Optional<Resume> existingResumeOpt = resumeRepository.findByStudentProfileIdAndIsCurrentTrue(profile.getId());
        Resume resume;
        int versionNumber;

        if (existingResumeOpt.isPresent()) {
            resume = existingResumeOpt.get();
            versionNumber = (resume.getCurrentVersion() != null ? resume.getCurrentVersion() : 1) + 1;

            resume.setFileName(rawFileName);
            resume.setOriginalFileName(rawFileName);
            resume.setStoredFileName(storedFileName);
            resume.setFilePath(targetLocation.toString());
            resume.setFileType(extension.toUpperCase());
            resume.setFileSize(file.getSize());
            resume.setCurrentVersion(versionNumber);
            resume.setStatus("Uploaded");
            resume.setIsCurrent(true);
        } else {
            versionNumber = 1;
            resume = Resume.builder()
                    .studentProfile(profile)
                    .fileName(rawFileName)
                    .originalFileName(rawFileName)
                    .storedFileName(storedFileName)
                    .filePath(targetLocation.toString())
                    .fileType(extension.toUpperCase())
                    .fileSize(file.getSize())
                    .currentVersion(1)
                    .status("Uploaded")
                    .isCurrent(true)
                    .build();
        }

        Resume savedResume = resumeRepository.save(resume);

        // Save ResumeVersion record for history
        ResumeVersion version = ResumeVersion.builder()
                .resume(savedResume)
                .versionNumber(versionNumber)
                .filePath(targetLocation.toString())
                .originalFileName(rawFileName)
                .storedFileName(storedFileName)
                .fileType(extension.toUpperCase())
                .fileSize(file.getSize())
                .commitMessage(versionNumber == 1 ? "Initial resume upload" : "Replaced with version " + versionNumber)
                .build();

        resumeVersionRepository.save(version);

        // Auto-parse resume safely without crashing upload
        try {
            resumeParsingService.parseAndSaveResume(savedResume.getId());
        } catch (Exception ex) {
            System.err.println("Auto parsing resume failed for resume ID " + savedResume.getId() + ": " + ex.getMessage());
        }

        return mapToDto(savedResume);
    }

    @Override
    @Transactional(readOnly = true)
    public Resource loadResumeFile(String usernameOrEmail, Long resumeId) {
        Resume resume = getResumeEntityWithOwnershipCheck(usernameOrEmail, resumeId);
        return loadResourceFromPath(resume.getFilePath());
    }

    @Override
    @Transactional(readOnly = true)
    public Resource loadResumeVersionFile(String usernameOrEmail, Long versionId) {
        ResumeVersion version = resumeVersionRepository.findById(versionId)
                .orElseThrow(() -> new IllegalArgumentException("Resume version not found with ID: " + versionId));

        // Ownership Check via parent Resume
        User user = findUser(usernameOrEmail);
        if (!version.getResume().getStudentProfile().getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Unauthorized access to resume version.");
        }

        return loadResourceFromPath(version.getFilePath());
    }

    @Override
    @Transactional(readOnly = true)
    public Resume getResumeEntityWithOwnershipCheck(String usernameOrEmail, Long resumeId) {
        User user = findUser(usernameOrEmail);
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new IllegalArgumentException("Resume not found with ID: " + resumeId));

        if (!resume.getStudentProfile().getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Unauthorized access to resume.");
        }

        return resume;
    }

    @Override
    @Transactional(readOnly = true)
    public Resource loadResumeFileForRecruiter(String recruiterUsername, Long resumeId) {
        Resume resume = getResumeEntityForRecruiter(recruiterUsername, resumeId);
        return loadResourceFromPath(resume.getFilePath());
    }

    @Override
    @Transactional(readOnly = true)
    public Resume getResumeEntityForRecruiter(String recruiterUsername, Long resumeId) {
        User user = findUser(recruiterUsername);
        Recruiter recruiter = recruiterRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Recruiter profile not found."));

        boolean hasApplication = jobApplicationRepository.existsByJobPostRecruiterIdAndResumeId(recruiter.getId(), resumeId);
        if (!hasApplication) {
            throw new IllegalArgumentException("Unauthorized access to applicant resume.");
        }

        return resumeRepository.findById(resumeId)
                .orElseThrow(() -> new IllegalArgumentException("Resume not found with ID: " + resumeId));
    }

    @Override
    @Transactional
    public void deleteResume(String usernameOrEmail, Long resumeId) {
        Resume resume = getResumeEntityWithOwnershipCheck(usernameOrEmail, resumeId);

        // 1. Preserve job applications history and disassociate resume_id
        List<com.resume.screening.entity.JobApplication> applications = jobApplicationRepository.findByResumeId(resumeId);
        if (applications != null && !applications.isEmpty()) {
            for (com.resume.screening.entity.JobApplication app : applications) {
                app.setResume(null);
            }
            jobApplicationRepository.saveAll(applications);
        }

        // 2. Preserve or disassociate resume_id on ATS analyses
        List<com.resume.screening.entity.ResumeAnalysis> analyses = resumeAnalysisRepository.findByResumeId(resumeId);
        if (analyses != null && !analyses.isEmpty()) {
            for (com.resume.screening.entity.ResumeAnalysis analysis : analyses) {
                analysis.setResume(null);
            }
            resumeAnalysisRepository.saveAll(analyses);
        }

        // 3. Try deleting main physical file
        try {
            Path mainPath = Paths.get(resume.getFilePath());
            Files.deleteIfExists(mainPath);
        } catch (IOException ignored) {}

        // 4. Delete version physical files safely
        if (resume.getVersions() != null) {
            for (ResumeVersion ver : resume.getVersions()) {
                try {
                    Path verPath = Paths.get(ver.getFilePath());
                    Files.deleteIfExists(verPath);
                } catch (IOException ignored) {}
            }
        }

        // 5. Delete resume entity safely
        resumeRepository.delete(resume);
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

    private StudentProfile getStudentProfile(User user) {
        return studentProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Student profile not found for user: " + user.getUsername()));
    }

    private StudentProfile getOrCreateStudentProfile(User user) {
        return studentProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> studentProfileRepository.save(StudentProfile.builder().user(user).build()));
    }

    private String getFileExtension(String filename) {
        int lastIndex = filename.lastIndexOf('.');
        if (lastIndex > 0) {
            return filename.substring(lastIndex + 1).toLowerCase();
        }
        return "";
    }

    private Resource loadResourceFromPath(String filePathStr) {
        try {
            Path filePath = Paths.get(filePathStr);
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new IllegalArgumentException("Resume file could not be read or does not exist on disk.");
            }
        } catch (MalformedURLException ex) {
            throw new IllegalArgumentException("File path is invalid: " + filePathStr, ex);
        }
    }

    private ResumeDto mapToDto(Resume resume) {
        List<ResumeVersion> versionEntities = resumeVersionRepository.findByResumeIdOrderByVersionNumberDesc(resume.getId());

        List<ResumeVersionDto> versionDtos = versionEntities.stream().map(ver -> ResumeVersionDto.builder()
                .id(ver.getId())
                .versionNumber(ver.getVersionNumber())
                .originalFileName(ver.getOriginalFileName() != null ? ver.getOriginalFileName() : resume.getOriginalFileName())
                .storedFileName(ver.getStoredFileName())
                .filePath(ver.getFilePath())
                .fileType(ver.getFileType() != null ? ver.getFileType() : resume.getFileType())
                .fileSize(ver.getFileSize())
                .fileSizeFormatted(formatFileSize(ver.getFileSize()))
                .commitMessage(ver.getCommitMessage())
                .createdAt(ver.getCreatedAt())
                .formattedUploadDate(ver.getCreatedAt() != null ? ver.getCreatedAt().format(DATE_FORMATTER) : "")
                .build()
        ).collect(Collectors.toList());

        boolean isPdf = "PDF".equalsIgnoreCase(resume.getFileType()) ||
                (resume.getOriginalFileName() != null && resume.getOriginalFileName().toLowerCase().endsWith(".pdf"));

        return ResumeDto.builder()
                .id(resume.getId())
                .studentProfileId(resume.getStudentProfile().getId())
                .originalFileName(resume.getOriginalFileName() != null ? resume.getOriginalFileName() : resume.getFileName())
                .storedFileName(resume.getStoredFileName() != null ? resume.getStoredFileName() : resume.getFileName())
                .filePath(resume.getFilePath())
                .fileType(resume.getFileType())
                .fileSize(resume.getFileSize())
                .fileSizeFormatted(formatFileSize(resume.getFileSize()))
                .currentVersion(resume.getCurrentVersion() != null ? resume.getCurrentVersion() : 1)
                .status(resume.getStatus() != null ? resume.getStatus() : "Uploaded")
                .isCurrent(resume.getIsCurrent() != null ? resume.getIsCurrent() : true)
                .createdAt(resume.getCreatedAt())
                .updatedAt(resume.getUpdatedAt())
                .formattedUploadDate(resume.getCreatedAt() != null ? resume.getCreatedAt().format(DATE_FORMATTER) : "")
                .pdf(isPdf)
                .versions(versionDtos)
                .build();
    }

    private String formatFileSize(Long bytes) {
        if (bytes == null || bytes == 0) return "0 KB";
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format(Locale.US, "%.1f %cB", bytes / Math.pow(1024, exp), pre);
    }
}
