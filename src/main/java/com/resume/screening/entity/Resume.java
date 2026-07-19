package com.resume.screening.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "resumes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"studentProfile", "versions", "aiAnalysis"})
@EqualsAndHashCode(exclude = {"studentProfile", "versions", "aiAnalysis"})
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private StudentProfile studentProfile;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_type", length = 50)
    private String fileType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder.Default
    @OneToMany(mappedBy = "resume", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<ResumeVersion> versions = new ArrayList<>();

    @OneToOne(mappedBy = "resume", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private AiAnalysis aiAnalysis;

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
