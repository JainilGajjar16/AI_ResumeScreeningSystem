package com.resume.screening.config;

import com.resume.screening.entity.Recruiter;
import com.resume.screening.entity.User;
import com.resume.screening.repository.RecruiterRepository;
import com.resume.screening.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.resume.screening.entity.StudentProfile;
import com.resume.screening.repository.StudentProfileRepository;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RecruiterRepository recruiterRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final com.resume.screening.repository.SkillRepository skillRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, 
                           RecruiterRepository recruiterRepository, 
                           StudentProfileRepository studentProfileRepository,
                           com.resume.screening.repository.SkillRepository skillRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.recruiterRepository = recruiterRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.skillRepository = skillRepository;
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

        // Seed default Student if not exists
        if (!userRepository.existsByUsername("student")) {
            User studentUser = User.builder()
                    .username("student")
                    .password(passwordEncoder.encode("student123"))
                    .email("student@example.com")
                    .role("STUDENT")
                    .firstName("John")
                    .lastName("Student")
                    .build();
            userRepository.save(studentUser);

            StudentProfile studentProfile = StudentProfile.builder()
                    .user(studentUser)
                    .phone("9876543210")
                    .build();
            studentProfileRepository.save(studentProfile);
        }

        // Seed default Skills if table is empty
        if (skillRepository.count() == 0) {
            String[] initialSkills = {
                "Java", "Python", "JavaScript", "HTML", "CSS", "Bootstrap", "Spring Boot",
                "Spring", "MySQL", "SQL", "MongoDB", "Git", "GitHub", "React", "Angular",
                "Flutter", "Dart", "PHP", "C", "C++", "C#", ".NET", "REST API", "AWS",
                "Docker", "Linux", "Machine Learning", "Data Science"
            };
            for (String skillName : initialSkills) {
                if (!skillRepository.existsByNameIgnoreCase(skillName)) {
                    skillRepository.save(com.resume.screening.entity.Skill.builder().name(skillName).build());
                }
            }
        }
    }
}
