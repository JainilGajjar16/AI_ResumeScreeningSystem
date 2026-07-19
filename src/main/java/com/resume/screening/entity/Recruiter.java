package com.resume.screening.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "recruiters")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"user", "jobPosts"})
@EqualsAndHashCode(exclude = {"user", "jobPosts"})
public class Recruiter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "company_name", nullable = false, length = 100)
    private String companyName;

    @Column(name = "company_website")
    private String companyWebsite;

    @Column(length = 100)
    private String designation;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder.Default
    @OneToMany(mappedBy = "recruiter", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<JobPost> jobPosts = new ArrayList<>();

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
