package com.resume.screening.repository;

import com.resume.screening.entity.ParsedResume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ParsedResumeRepository extends JpaRepository<ParsedResume, Long> {
    Optional<ParsedResume> findByResumeId(Long resumeId);
    Optional<ParsedResume> findByStudentProfileId(Long studentProfileId);
    void deleteByResumeId(Long resumeId);
}
