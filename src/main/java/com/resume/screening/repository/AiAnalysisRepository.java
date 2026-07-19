package com.resume.screening.repository;

import com.resume.screening.entity.AiAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AiAnalysisRepository extends JpaRepository<AiAnalysis, Long> {
    Optional<AiAnalysis> findByResumeId(Long resumeId);
}
