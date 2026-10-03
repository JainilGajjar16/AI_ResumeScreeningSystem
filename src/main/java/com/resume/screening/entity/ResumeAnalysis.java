package com.resume.screening.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "resume_analyses", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"resume_id", "job_id", "resume_version_number"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"resume", "jobPost", "studentProfile"})
@EqualsAndHashCode(exclude = {"resume", "jobPost", "studentProfile"})
public class ResumeAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", nullable = true)
    private Resume resume;

    @Column(name = "resume_version_number", nullable = false)
    private Integer resumeVersionNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private JobPost jobPost;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private StudentProfile studentProfile;

    @Column(name = "ats_score", nullable = false)
    private Double atsScore;

    @Column(name = "skill_match_percentage")
    private Double skillMatchPercentage;

    @Column(name = "matched_skills", columnDefinition = "TEXT")
    private String matchedSkills;

    @Column(name = "missing_skills", columnDefinition = "TEXT")
    private String missingSkills;

    @Column(name = "qualification_match", length = 30)
    private String qualificationMatch; // MATCH, PARTIAL_MATCH, NO_MATCH, NOT_AVAILABLE

    @Column(name = "experience_match", length = 30)
    private String experienceMatch; // MATCH, PARTIAL_MATCH, NO_MATCH, NOT_AVAILABLE

    @Column(name = "qualification_score")
    private Double qualificationScore;

    @Column(name = "experience_score")
    private Double experienceScore;

    @Column(name = "resume_completeness_score")
    private Double resumeCompletenessScore;

    @Column(name = "keyword_match_score")
    private Double keywordMatchScore;

    @Column(name = "skills_match_score")
    private Double skillsMatchScore;

    @Column(columnDefinition = "TEXT")
    private String strengths;

    @Column(columnDefinition = "TEXT")
    private String suggestions;

    @Column(name = "analyzed_at", nullable = false)
    private LocalDateTime analyzedAt;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        if (analyzedAt == null) {
            analyzedAt = LocalDateTime.now();
        }
    }
}
