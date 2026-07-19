package com.resume.screening.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "interview_feedback")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"jobApplication", "interviewer"})
@EqualsAndHashCode(exclude = {"jobApplication", "interviewer"})
public class InterviewFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_application_id", nullable = false)
    private JobApplication jobApplication;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interviewer_id", nullable = false)
    private User interviewer;

    @Column(nullable = false)
    private Integer rating; // Rating out of 5

    @Column(columnDefinition = "TEXT")
    private String comments;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
