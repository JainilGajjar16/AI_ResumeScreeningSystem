package com.resume.screening.service;

import com.resume.screening.entity.Recruiter;
import com.resume.screening.entity.StudentProfile;
import com.resume.screening.entity.User;
import org.springframework.security.core.Authentication;

public interface UserResolutionService {

    /**
     * Resolves the authenticated User entity from Spring Security Authentication.
     * Determines whether Authentication#getName() is a username or email and resolves the exact User.
     */
    User resolveAuthenticatedUser(Authentication authentication);

    /**
     * Resolves the User entity from a raw login identifier (username or email).
     */
    User resolveUser(String identifier);

    /**
     * Resolves the StudentProfile for the authenticated user.
     */
    StudentProfile resolveStudentProfile(Authentication authentication);

    /**
     * Resolves the StudentProfile by User entity.
     */
    StudentProfile resolveStudentProfile(User user);

    /**
     * Resolves the StudentProfile by username or email.
     */
    StudentProfile resolveStudentProfile(String identifier);

    /**
     * Resolves the Recruiter entity for the authenticated user.
     */
    Recruiter resolveRecruiter(Authentication authentication);

    /**
     * Resolves the Recruiter entity by User entity.
     */
    Recruiter resolveRecruiter(User user);

    /**
     * Resolves the Recruiter entity by username or email.
     */
    Recruiter resolveRecruiter(String identifier);

    /**
     * Utility method checking whether a string appears to be an email address.
     */
    boolean isEmail(String identifier);
}
