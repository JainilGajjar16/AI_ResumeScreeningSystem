package com.resume.screening.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "resume_versions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"resume"})
@EqualsAndHashCode(exclude = {"resume"})
public class ResumeVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "commit_message")
    private String commitMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
