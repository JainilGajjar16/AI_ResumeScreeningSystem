package com.resume.screening.entity;

import com.resume.screening.enums.ApplicationDecision;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "job_applications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"jobPost", "studentProfile", "resume", "atsScore"})
@EqualsAndHashCode(exclude = {"jobPost", "studentProfile", "resume", "atsScore"})
public class JobApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_post_id", nullable = false)
    private JobPost jobPost;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private StudentProfile studentProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", nullable = true)
    private Resume resume;

    @Column(length = 50)
    @Builder.Default
    private String status = "APPLIED"; // e.g., 'APPLIED', 'SCREENED', 'SHORTLISTED', 'REJECTED'

    @Enumerated(EnumType.STRING)
    @Column(name = "final_decision", nullable = false, length = 30)
    @Builder.Default
    private ApplicationDecision finalDecision = ApplicationDecision.PENDING;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @Column(name = "applied_at", nullable = false, updatable = false)
    private LocalDateTime appliedAt;

    @OneToOne(mappedBy = "jobApplication", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private AtsScore atsScore;

    @PrePersist
    protected void onCreate() {
        appliedAt = LocalDateTime.now();
        if (finalDecision == null) {
            finalDecision = ApplicationDecision.PENDING;
        }
    }
}
