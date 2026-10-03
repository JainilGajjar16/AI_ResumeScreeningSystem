package com.resume.screening.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "parsed_resumes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"resume", "studentProfile"})
@EqualsAndHashCode(exclude = {"resume", "studentProfile"})
public class ParsedResume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", nullable = false, unique = true)
    private Resume resume;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private StudentProfile studentProfile;

    @Column(name = "raw_text", columnDefinition = "LONGTEXT")
    private String rawText;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String skills;

    @Column(columnDefinition = "TEXT")
    private String education;

    @Column(columnDefinition = "TEXT")
    private String experience;

    @Column(columnDefinition = "TEXT")
    private String projects;

    @Column(columnDefinition = "TEXT")
    private String certifications;

    @Column(name = "parsing_status", length = 30, nullable = false)
    @Builder.Default
    private String parsingStatus = "NOT_PARSED"; // NOT_PARSED, PROCESSING, PARSED, FAILED

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "parser_version", length = 20)
    @Builder.Default
    private String parserVersion = "v1.0";

    @Column(name = "parsed_at")
    private LocalDateTime parsedAt;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        if (parsedAt == null && "PARSED".equals(parsingStatus)) {
            parsedAt = LocalDateTime.now();
        }
    }
}
