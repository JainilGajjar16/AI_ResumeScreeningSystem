package com.resume.screening.config;

import com.resume.screening.entity.Recruiter;
import com.resume.screening.entity.User;
import com.resume.screening.repository.RecruiterRepository;
import com.resume.screening.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RecruiterRepository recruiterRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, 
                           RecruiterRepository recruiterRepository, 
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.recruiterRepository = recruiterRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Seed default Admin if not exists
        if (!userRepository.existsByUsername("admin")) {
            User adminUser = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .email("admin@enterprise.com")
                    .role("ADMIN")
                    .firstName("System")
                    .lastName("Admin")
                    .build();
            userRepository.save(adminUser);
        }

        // Seed default Recruiter if not exists
        if (!userRepository.existsByUsername("recruiter")) {
            User recruiterUser = User.builder()
                    .username("recruiter")
                    .password(passwordEncoder.encode("recruiter123"))
                    .email("recruiter@enterprise.com")
                    .role("RECRUITER")
                    .firstName("Jane")
                    .lastName("Doe")
                    .build();
            userRepository.save(recruiterUser);

            Recruiter recruiterProfile = Recruiter.builder()
                    .user(recruiterUser)
                    .companyName("AI Solutions Inc.")
                    .companyWebsite("https://aisolutions.example.com")
                    .designation("Talent Acquisition Manager")
                    .build();
            recruiterRepository.save(recruiterProfile);
        }
    }
}
