package com.resume.screening.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "resume_recommendations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"jobPost", "resume"})
@EqualsAndHashCode(exclude = {"jobPost", "resume"})
public class ResumeRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_post_id", nullable = false)
    private JobPost jobPost;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", nullable = true)
    private Resume resume;

    @Column(name = "match_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal matchPercentage;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "recommended_at", nullable = false, updatable = false)
    private LocalDateTime recommendedAt;

    @PrePersist
    protected void onCreate() {
        recommendedAt = LocalDateTime.now();
    }
}
