package com.resume.screening.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "parsed_job_requirements")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"jobPost"})
@EqualsAndHashCode(exclude = {"jobPost"})
public class ParsedJobRequirement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false, unique = true)
    private JobPost jobPost;

    @Column(name = "required_skills", columnDefinition = "TEXT")
    private String requiredSkills;

    @Column(columnDefinition = "TEXT")
    private String qualification;

    @Column(columnDefinition = "TEXT")
    private String experience;

    @Column(columnDefinition = "TEXT")
    private String responsibilities;

    @Column(columnDefinition = "TEXT")
    private String keywords;

    @Column(name = "raw_text", columnDefinition = "LONGTEXT")
    private String rawText;

    @Column(name = "parsing_status", length = 30, nullable = false)
    @Builder.Default
    private String parsingStatus = "PARSED"; // PARSED, FAILED

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "parsed_at")
    private LocalDateTime parsedAt;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        if (parsedAt == null) {
            parsedAt = LocalDateTime.now();
        }
    }
}
