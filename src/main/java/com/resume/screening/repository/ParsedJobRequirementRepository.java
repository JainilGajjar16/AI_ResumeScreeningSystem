package com.resume.screening.repository;

import com.resume.screening.entity.ParsedJobRequirement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ParsedJobRequirementRepository extends JpaRepository<ParsedJobRequirement, Long> {
    Optional<ParsedJobRequirement> findByJobPostId(Long jobId);
    void deleteByJobPostId(Long jobId);
}
