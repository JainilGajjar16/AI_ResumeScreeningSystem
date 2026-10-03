package com.resume.screening.repository;

import com.resume.screening.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

import java.util.Optional;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, Long> {
    List<Resume> findByStudentProfileId(Long studentProfileId);
    Optional<Resume> findByStudentProfileIdAndIsCurrentTrue(Long studentProfileId);
    List<Resume> findByStudentProfileIdOrderByCreatedAtDesc(Long studentProfileId);
    Optional<Resume> findByStudentProfileUserUsernameAndIsCurrentTrue(String username);
    Optional<Resume> findByStudentProfileUserEmailAndIsCurrentTrue(String email);
}
