package com.resume.screening.repository;

import com.resume.screening.entity.ShortlistedCandidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ShortlistedCandidateRepository extends JpaRepository<ShortlistedCandidate, Long> {
    List<ShortlistedCandidate> findByJobPostId(Long jobPostId);
    List<ShortlistedCandidate> findByStudentProfileId(Long studentProfileId);
}
