package com.resume.screening.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "ats_scores")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"jobApplication"})
@EqualsAndHashCode(exclude = {"jobApplication"})
public class AtsScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_application_id", nullable = false, unique = true)
    private JobApplication jobApplication;

    @Column(name = "overall_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal overallScore;

    @Column(name = "skill_match_score", precision = 5, scale = 2)
    private BigDecimal skillMatchScore;

    @Column(name = "experience_match_score", precision = 5, scale = 2)
    private BigDecimal experienceMatchScore;

    @Column(name = "education_match_score", precision = 5, scale = 2)
    private BigDecimal educationMatchScore;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    @Column(name = "scored_at", nullable = false, updatable = false)
    private LocalDateTime scoredAt;

    @PrePersist
    protected void onCreate() {
        scoredAt = LocalDateTime.now();
    }
}
