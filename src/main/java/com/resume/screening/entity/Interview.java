package com.resume.screening.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "interviews")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"jobApplication", "recruiter", "student"})
@EqualsAndHashCode(exclude = {"jobApplication", "recruiter", "student"})
public class Interview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_application_id", nullable = false)
    private JobApplication jobApplication;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruiter_id", nullable = false)
    private Recruiter recruiter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private StudentProfile student;

    @Column(name = "round_name", nullable = false, length = 100)
    private String roundName;

    @Column(name = "interview_date", nullable = false)
    private LocalDate interviewDate;

    @Column(name = "interview_time", nullable = false)
    private LocalTime interviewTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "interview_type", nullable = false, length = 20)
    private InterviewType interviewType;

    @Column(name = "meeting_link", length = 500)
    private String meetingLink;

    @Column(name = "location", length = 255)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private InterviewStatus status = InterviewStatus.SCHEDULED;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    @Column(name = "rating")
    private Integer rating;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, length = 30)
    @Builder.Default
    private InterviewResult result = InterviewResult.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
