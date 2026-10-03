package com.resume.screening.service.impl;

import com.resume.screening.entity.Recruiter;
import com.resume.screening.entity.StudentProfile;
import com.resume.screening.entity.User;
import com.resume.screening.repository.RecruiterRepository;
import com.resume.screening.repository.StudentProfileRepository;
import com.resume.screening.repository.UserRepository;
import com.resume.screening.security.CustomUserDetails;
import com.resume.screening.service.UserResolutionService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserResolutionServiceImpl implements UserResolutionService {

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final RecruiterRepository recruiterRepository;

    public UserResolutionServiceImpl(UserRepository userRepository,
                                    StudentProfileRepository studentProfileRepository,
                                    RecruiterRepository recruiterRepository) {
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.recruiterRepository = recruiterRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public User resolveAuthenticatedUser(Authentication authentication) {
        if (authentication == null) {
            authentication = SecurityContextHolder.getContext().getAuthentication();
        }
        if (authentication == null) {
            throw new IllegalArgumentException("User is not authenticated.");
        }

        // 1. Fast resolution if principal is CustomUserDetails
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails customUserDetails) {
            if (customUserDetails.getUser() != null && customUserDetails.getUser().getId() != null) {
                Optional<User> freshUser = userRepository.findById(customUserDetails.getUser().getId());
                if (freshUser.isPresent()) {
                    return freshUser.get();
                }
                return customUserDetails.getUser();
            }
        }

        // 2. Resolve via authentication name or principal string
        String authName = authentication.getName();
        if (authName == null || authName.trim().isEmpty() || "anonymousUser".equalsIgnoreCase(authName)) {
            if (principal instanceof String && !((String) principal).trim().isEmpty() && !"anonymousUser".equalsIgnoreCase((String) principal)) {
                authName = (String) principal;
            } else {
                throw new IllegalArgumentException("User is not authenticated.");
            }
        }

        return resolveUser(authName);
    }

    @Override
    @Transactional(readOnly = true)
    public User resolveUser(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            throw new IllegalArgumentException("User identifier cannot be empty.");
        }

        String trimmed = identifier.trim();

        if (isEmail(trimmed)) {
            // First search by email, then fallback to username
            return userRepository.findByEmailIgnoreCase(trimmed)
                    .or(() -> userRepository.findByEmail(trimmed))
                    .or(() -> userRepository.findByUsernameOrEmail(trimmed))
                    .or(() -> userRepository.findByUsernameIgnoreCase(trimmed))
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + trimmed));
        } else {
            // Otherwise search by username, then fallback to email
            return userRepository.findByUsernameIgnoreCase(trimmed)
                    .or(() -> userRepository.findByUsername(trimmed))
                    .or(() -> userRepository.findByUsernameOrEmail(trimmed))
                    .or(() -> userRepository.findByEmailIgnoreCase(trimmed))
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + trimmed));
        }
    }

    @Override
    @Transactional
    public StudentProfile resolveStudentProfile(Authentication authentication) {
        User user = resolveAuthenticatedUser(authentication);
        return resolveStudentProfile(user);
    }

    @Override
    @Transactional
    public StudentProfile resolveStudentProfile(User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User cannot be null.");
        }
        return studentProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> studentProfileRepository.save(StudentProfile.builder().user(user).build()));
    }

    @Override
    @Transactional
    public StudentProfile resolveStudentProfile(String identifier) {
        User user = resolveUser(identifier);
        return resolveStudentProfile(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Recruiter resolveRecruiter(Authentication authentication) {
        User user = resolveAuthenticatedUser(authentication);
        return resolveRecruiter(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Recruiter resolveRecruiter(User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User cannot be null.");
        }
        return recruiterRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Recruiter profile not found for user: " + user.getUsername()));
    }

    @Override
    @Transactional(readOnly = true)
    public Recruiter resolveRecruiter(String identifier) {
        User user = resolveUser(identifier);
        return resolveRecruiter(user);
    }

    @Override
    public boolean isEmail(String identifier) {
        if (identifier == null) {
            return false;
        }
        String trimmed = identifier.trim();
        return trimmed.contains("@") && trimmed.indexOf('@') > 0 && trimmed.indexOf('@') < trimmed.length() - 1;
    }
}
