package com.resume.screening.repository;

import com.resume.screening.entity.ResumeRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ResumeRecommendationRepository extends JpaRepository<ResumeRecommendation, Long> {
    List<ResumeRecommendation> findByJobPostIdOrderByMatchPercentageDesc(Long jobPostId);
}
