package com.resume.screening.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "job_posts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"recruiter", "applications", "parsedJobRequirement"})
@EqualsAndHashCode(exclude = {"recruiter", "applications", "parsedJobRequirement"})
public class JobPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruiter_id", nullable = false)
    private Recruiter recruiter;

    @Column(name = "company_name", length = 100)
    private String companyName;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "required_skills", columnDefinition = "TEXT")
    private String requiredSkills;

    @Column(columnDefinition = "TEXT")
    private String requirements;

    @Column(columnDefinition = "TEXT")
    private String qualification;

    @Column(name = "experience_required", length = 100)
    private String experienceRequired;

    @Column(length = 100)
    private String location;

    @Column(name = "job_type", length = 50)
    private String jobType; // e.g., 'Full Time', 'Part Time', 'Internship', 'Contract'

    @Column(name = "salary_range", length = 100)
    private String salaryRange;

    @Column(name = "deadline")
    private LocalDate deadline;

    @Column(length = 20)
    @Builder.Default
    private String status = "DRAFT"; // e.g., 'DRAFT', 'PUBLISHED', 'CLOSED'

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder.Default
    @OneToMany(mappedBy = "jobPost", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<JobApplication> applications = new ArrayList<>();

    @OneToOne(mappedBy = "jobPost", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private ParsedJobRequirement parsedJobRequirement;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
