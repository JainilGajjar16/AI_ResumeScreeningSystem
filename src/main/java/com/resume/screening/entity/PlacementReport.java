package com.resume.screening.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "placement_reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"generatedBy"})
@EqualsAndHashCode(exclude = {"generatedBy"})
public class PlacementReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "academic_year", nullable = false, length = 20)
    private String academicYear;

    @Column(name = "total_students", nullable = false)
    private Integer totalStudents;

    @Column(name = "total_placed", nullable = false)
    private Integer totalPlaced;

    @Column(name = "average_package", precision = 12, scale = 2)
    private BigDecimal averagePackage;

    @Column(name = "top_recruiter", length = 150)
    private String topRecruiter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_by", nullable = false)
    private User generatedBy;

    @Column(name = "generated_at", nullable = false, updatable = false)
    private LocalDateTime generatedAt;

    @PrePersist
    protected void onCreate() {
        generatedAt = LocalDateTime.now();
    }
}
