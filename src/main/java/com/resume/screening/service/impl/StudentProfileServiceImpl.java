package com.resume.screening.service.impl;

import com.resume.screening.dto.StudentProfileDto;
import com.resume.screening.entity.Skill;
import com.resume.screening.entity.StudentProfile;
import com.resume.screening.entity.User;
import com.resume.screening.repository.SkillRepository;
import com.resume.screening.repository.StudentProfileRepository;
import com.resume.screening.repository.UserRepository;
import com.resume.screening.service.StudentProfileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class StudentProfileServiceImpl implements StudentProfileService {

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final SkillRepository skillRepository;
    private final com.resume.screening.service.UserResolutionService userResolutionService;

    private static final Pattern GITHUB_PATTERN = Pattern.compile(
            "^(https?://)?(www\\.)?github\\.com/[a-zA-Z0-9_-]+/?$", Pattern.CASE_INSENSITIVE
    );

    private static final Pattern LINKEDIN_PATTERN = Pattern.compile(
            "^(https?://)?(www\\.)?linkedin\\.com/(in|pub|profile)/[a-zA-Z0-9_-]+/?$", Pattern.CASE_INSENSITIVE
    );

    public StudentProfileServiceImpl(UserRepository userRepository, StudentProfileRepository studentProfileRepository) {
        this(userRepository, studentProfileRepository, null, null);
    }

    public StudentProfileServiceImpl(UserRepository userRepository,
                                     StudentProfileRepository studentProfileRepository,
                                     SkillRepository skillRepository) {
        this(userRepository, studentProfileRepository, skillRepository, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public StudentProfileServiceImpl(UserRepository userRepository,
                                     StudentProfileRepository studentProfileRepository,
                                     SkillRepository skillRepository,
                                     com.resume.screening.service.UserResolutionService userResolutionService) {
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.skillRepository = skillRepository;
        this.userResolutionService = userResolutionService != null ? userResolutionService :
                new com.resume.screening.service.impl.UserResolutionServiceImpl(userRepository, studentProfileRepository, null);
    }

    @Override
    @Transactional
    public StudentProfileDto getProfileByUsernameOrEmail(String usernameOrEmail) {
        User user = findUser(usernameOrEmail);
        StudentProfile profile = getOrCreateStudentProfile(user);

        // Retrieve skills from student_skills + skills
        String skillsDisplay;
        if (profile.getSkills() != null && !profile.getSkills().isEmpty()) {
            skillsDisplay = profile.getSkills().stream()
                    .map(Skill::getName)
                    .filter(StringUtils::hasText)
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .collect(Collectors.joining(", "));
            if (!skillsDisplay.equals(profile.getSkillsText())) {
                profile.setSkillsText(skillsDisplay);
                studentProfileRepository.save(profile);
            }
        } else if (StringUtils.hasText(profile.getSkillsText())) {
            // Backward-compatibility: populate student_skills from legacy skills_text
            syncStudentSkills(profile, profile.getSkillsText());
            studentProfileRepository.save(profile);
            skillsDisplay = profile.getSkillsText() != null ? profile.getSkillsText() : "";
        } else {
            skillsDisplay = "";
        }

        int completion = calculateProfileCompletion(user, profile);

        return StudentProfileDto.builder()
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(profile.getPhone())
                .dob(profile.getDob())
                .college(profile.getCollege())
                .degree(profile.getDegree())
                .branch(profile.getBranch())
                .semester(profile.getSemester())
                .cgpa(profile.getCgpa())
                .summary(profile.getSummary())
                .skills(skillsDisplay)
                .githubUrl(profile.getGithubUrl())
                .linkedinUrl(profile.getLinkedinUrl())
                .completionPercentage(completion)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public StudentProfile getStudentProfileEntityByUsernameOrEmail(String usernameOrEmail) {
        User user = findUser(usernameOrEmail);
        return getOrCreateStudentProfile(user);
    }

    @Override
    @Transactional
    public StudentProfileDto updateProfile(String usernameOrEmail, StudentProfileDto dto) {
        User user = findUser(usernameOrEmail);
        StudentProfile profile = getOrCreateStudentProfile(user);

        // Update User info (First Name & Last Name)
        if (StringUtils.hasText(dto.getFirstName())) {
            user.setFirstName(dto.getFirstName().trim());
        }
        if (StringUtils.hasText(dto.getLastName())) {
            user.setLastName(dto.getLastName().trim());
        }
        userRepository.save(user);

        // Validate GitHub & LinkedIn URLs
        String sanitizedGithub = normalizeAndValidateGithubUrl(dto.getGithubUrl());
        String sanitizedLinkedin = normalizeAndValidateLinkedinUrl(dto.getLinkedinUrl());

        // Validate Semester & CGPA range extra check if provided
        if (dto.getSemester() != null && (dto.getSemester() < 1 || dto.getSemester() > 12)) {
            throw new IllegalArgumentException("Semester must be between 1 and 12");
        }
        if (dto.getCgpa() != null && (dto.getCgpa() < 0.0 || dto.getCgpa() > 10.0)) {
            throw new IllegalArgumentException("CGPA must be between 0.00 and 10.00");
        }

        // Update StudentProfile fields
        profile.setPhone(StringUtils.hasText(dto.getPhone()) ? dto.getPhone().trim() : null);
        profile.setDob(dto.getDob());
        profile.setCollege(StringUtils.hasText(dto.getCollege()) ? dto.getCollege().trim() : null);
        profile.setDegree(StringUtils.hasText(dto.getDegree()) ? dto.getDegree().trim() : null);
        profile.setBranch(StringUtils.hasText(dto.getBranch()) ? dto.getBranch().trim() : null);
        profile.setSemester(dto.getSemester());
        profile.setCgpa(dto.getCgpa());
        profile.setSummary(StringUtils.hasText(dto.getSummary()) ? dto.getSummary().trim() : null);
        profile.setGithubUrl(sanitizedGithub);
        profile.setLinkedinUrl(sanitizedLinkedin);

        // Synchronize student skills in student_skills and skills tables
        syncStudentSkills(profile, dto.getSkills());

        StudentProfile savedProfile = studentProfileRepository.save(profile);
        int completion = calculateProfileCompletion(user, savedProfile);

        return StudentProfileDto.builder()
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(savedProfile.getPhone())
                .dob(savedProfile.getDob())
                .college(savedProfile.getCollege())
                .degree(savedProfile.getDegree())
                .branch(savedProfile.getBranch())
                .semester(savedProfile.getSemester())
                .cgpa(savedProfile.getCgpa())
                .summary(savedProfile.getSummary())
                .skills(savedProfile.getSkillsText())
                .githubUrl(savedProfile.getGithubUrl())
                .linkedinUrl(savedProfile.getLinkedinUrl())
                .completionPercentage(completion)
                .build();
    }

    private void syncStudentSkills(StudentProfile profile, String skillsInput) {
        if (profile.getSkills() == null) {
            profile.setSkills(new HashSet<>());
        }

        Set<Skill> targetSkills = new LinkedHashSet<>();
        if (StringUtils.hasText(skillsInput)) {
            String[] tokens = skillsInput.split("[,;\\n\\r]+");
            Set<String> seenLower = new HashSet<>();

            for (String token : tokens) {
                String trimmed = token.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                String lower = trimmed.toLowerCase();
                if (seenLower.contains(lower)) {
                    continue;
                }
                seenLower.add(lower);

                if (skillRepository != null) {
                    // Reuse existing skill case-insensitively, or create new master skill
                    Skill masterSkill = skillRepository.findByNameIgnoreCase(trimmed)
                            .orElseGet(() -> skillRepository.save(
                                    Skill.builder()
                                            .name(trimmed)
                                            .build()
                            ));
                    targetSkills.add(masterSkill);
                }
            }
        }

        profile.getSkills().clear();
        profile.getSkills().addAll(targetSkills);

        // Normalize skills text for display & legacy column
        String normalizedText = targetSkills.stream()
                .map(Skill::getName)
                .collect(Collectors.joining(", "));
        profile.setSkillsText(StringUtils.hasText(normalizedText) ? normalizedText : null);
    }

    @Override
    public int calculateProfileCompletion(User user, StudentProfile profile) {
        int completedCount = 0;
        int totalFields = 12;

        // 1. Full Name (both first and last name present)
        if (user != null && StringUtils.hasText(user.getFirstName()) && StringUtils.hasText(user.getLastName())) {
            completedCount++;
        }
        // 2. Email
        if (user != null && StringUtils.hasText(user.getEmail())) {
            completedCount++;
        }
        if (profile != null) {
            // 3. Mobile
            if (StringUtils.hasText(profile.getPhone())) {
                completedCount++;
            }
            // 4. College
            if (StringUtils.hasText(profile.getCollege())) {
                completedCount++;
            }
            // 5. Degree
            if (StringUtils.hasText(profile.getDegree())) {
                completedCount++;
            }
            // 6. Branch
            if (StringUtils.hasText(profile.getBranch())) {
                completedCount++;
            }
            // 7. Semester
            if (profile.getSemester() != null && profile.getSemester() > 0) {
                completedCount++;
            }
            // 8. CGPA
            if (profile.getCgpa() != null && profile.getCgpa() > 0.0) {
                completedCount++;
            }
            // 9. Professional Summary
            if (StringUtils.hasText(profile.getSummary())) {
                completedCount++;
            }
            // 10. Skills
            boolean hasSkills = (profile.getSkills() != null && !profile.getSkills().isEmpty())
                    || StringUtils.hasText(profile.getSkillsText());
            if (hasSkills) {
                completedCount++;
            }
            // 11. GitHub URL
            if (StringUtils.hasText(profile.getGithubUrl())) {
                completedCount++;
            }
            // 12. LinkedIn URL
            if (StringUtils.hasText(profile.getLinkedinUrl())) {
                completedCount++;
            }
        }

        return (int) Math.round(((double) completedCount / totalFields) * 100);
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
                .orElseThrow(() -> new IllegalArgumentException("User not found with username or email: " + trimmed));
    }

    private StudentProfile getOrCreateStudentProfile(User user) {
        return studentProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> studentProfileRepository.save(StudentProfile.builder().user(user).build()));
    }

    private String normalizeAndValidateGithubUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return null;
        }
        String trimmed = url.trim();
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            trimmed = "https://" + trimmed;
        }
        if (!GITHUB_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("Invalid GitHub profile URL. Must be in the format: https://github.com/username");
        }
        return trimmed;
    }

    private String normalizeAndValidateLinkedinUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return null;
        }
        String trimmed = url.trim();
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            trimmed = "https://" + trimmed;
        }
        if (!LINKEDIN_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("Invalid LinkedIn profile URL. Must be in the format: https://linkedin.com/in/username");
        }
        return trimmed;
    }
}
