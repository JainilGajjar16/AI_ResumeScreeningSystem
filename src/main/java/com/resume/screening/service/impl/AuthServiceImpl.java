package com.resume.screening.service.impl;

import com.resume.screening.dto.ForgotPasswordDto;
import com.resume.screening.dto.StudentRegisterDto;
import com.resume.screening.entity.StudentProfile;
import com.resume.screening.entity.User;
import com.resume.screening.repository.StudentProfileRepository;
import com.resume.screening.repository.UserRepository;
import com.resume.screening.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(UserRepository userRepository, 
                           StudentProfileRepository studentProfileRepository, 
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void registerStudent(StudentRegisterDto registerDto) {
        if (!registerDto.getPassword().equals(registerDto.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }
        if (userRepository.existsByUsername(registerDto.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmail(registerDto.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = User.builder()
                .username(registerDto.getUsername())
                .password(passwordEncoder.encode(registerDto.getPassword()))
                .email(registerDto.getEmail())
                .role("STUDENT")
                .firstName(registerDto.getFirstName())
                .lastName(registerDto.getLastName())
                .build();
        userRepository.save(user);

        StudentProfile profile = StudentProfile.builder()
                .user(user)
                .phone(registerDto.getPhone())
                .build();
        studentProfileRepository.save(profile);
    }

    @Override
    @Transactional
    public void resetPassword(ForgotPasswordDto resetDto) {
        if (!resetDto.getNewPassword().equals(resetDto.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        User user = userRepository.findByEmail(resetDto.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Email address not found"));

        user.setPassword(passwordEncoder.encode(resetDto.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
}
