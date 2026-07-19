package com.resume.screening.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "ai_analysis")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"resume"})
@EqualsAndHashCode(exclude = {"resume"})
public class AiAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", nullable = false, unique = true)
    private Resume resume;

    @Column(name = "parsed_text", columnDefinition = "LONGTEXT")
    private String parsedText;

    @Column(name = "key_skills_found", columnDefinition = "TEXT")
    private String keySkillsFound;

    @Column(name = "experience_score", precision = 5, scale = 2)
    private BigDecimal experienceScore;

    @Column(name = "sentiment_score", precision = 5, scale = 2)
    private BigDecimal sentimentScore;

    @Column(columnDefinition = "TEXT")
    private String recommendations;

    @Column(name = "analyzed_at", nullable = false, updatable = false)
    private LocalDateTime analyzedAt;

    @PrePersist
    protected void onCreate() {
        analyzedAt = LocalDateTime.now();
    }
}
