package com.resume.screening.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "job_posts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"recruiter", "applications"})
@EqualsAndHashCode(exclude = {"recruiter", "applications"})
public class JobPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruiter_id", nullable = false)
    private Recruiter recruiter;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(columnDefinition = "TEXT")
    private String requirements;

    @Column(length = 100)
    private String location;

    @Column(name = "job_type", length = 50)
    private String jobType; // e.g., 'FULL_TIME', 'PART_TIME', 'INTERNSHIP'

    @Column(length = 20)
    @Builder.Default
    private String status = "ACTIVE"; // e.g., 'ACTIVE', 'CLOSED'

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder.Default
    @OneToMany(mappedBy = "jobPost", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<JobApplication> applications = new ArrayList<>();

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
