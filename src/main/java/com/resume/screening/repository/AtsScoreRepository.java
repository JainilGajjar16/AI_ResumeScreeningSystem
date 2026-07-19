package com.resume.screening.repository;

import com.resume.screening.entity.AtsScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AtsScoreRepository extends JpaRepository<AtsScore, Long> {
    Optional<AtsScore> findByJobApplicationId(Long jobApplicationId);
}
