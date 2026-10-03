package com.resume.screening.repository;

import com.resume.screening.entity.StudentProfile;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {
    @EntityGraph(attributePaths = {"skills"})
    Optional<StudentProfile> findByUserId(Long userId);
}
