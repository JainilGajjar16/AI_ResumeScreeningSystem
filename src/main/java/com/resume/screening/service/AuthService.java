package com.resume.screening.service;

import com.resume.screening.dto.ForgotPasswordDto;
import com.resume.screening.dto.StudentRegisterDto;

public interface AuthService {
    void registerStudent(StudentRegisterDto registerDto);
    void resetPassword(ForgotPasswordDto resetDto);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
