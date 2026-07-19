package com.resume.screening.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "shortlisted_candidates")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"jobPost", "studentProfile", "addedBy"})
@EqualsAndHashCode(exclude = {"jobPost", "studentProfile", "addedBy"})
public class ShortlistedCandidate {

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
    @JoinColumn(name = "added_by", nullable = false)
    private User addedBy;

    @Column(length = 50)
    @Builder.Default
    private String status = "SHORTLISTED"; // e.g., 'SHORTLISTED', 'INTERVIEW_SCHEDULED', 'HIRED', 'REJECTED'

    @Column(name = "added_at", nullable = false, updatable = false)
    private LocalDateTime addedAt;

    @PrePersist
    protected void onCreate() {
        addedAt = LocalDateTime.now();
    }
}
