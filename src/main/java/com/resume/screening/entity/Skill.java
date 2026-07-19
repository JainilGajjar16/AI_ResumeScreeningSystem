package com.resume.screening.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.Set;
import java.util.HashSet;

@Entity
@Table(name = "skills")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"students"})
@EqualsAndHashCode(exclude = {"students"})
public class Skill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Builder.Default
    @ManyToMany(mappedBy = "skills", fetch = FetchType.LAZY)
    private Set<StudentProfile> students = new HashSet<>();
}
