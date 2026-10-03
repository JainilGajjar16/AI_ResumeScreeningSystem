package com.resume.screening.repository;

import com.resume.screening.entity.ShortlistedCandidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShortlistedCandidateRepository extends JpaRepository<ShortlistedCandidate, Long> {
    List<ShortlistedCandidate> findByJobPostId(Long jobPostId);
    List<ShortlistedCandidate> findByStudentProfileId(Long studentProfileId);
    Optional<ShortlistedCandidate> findByJobPostIdAndStudentProfileId(Long jobPostId, Long studentProfileId);
    boolean existsByJobPostIdAndStudentProfileId(Long jobPostId, Long studentProfileId);
    void deleteByJobPostIdAndStudentProfileId(Long jobPostId, Long studentProfileId);
    List<ShortlistedCandidate> findByAddedByIdOrderByAddedAtDesc(Long recruiterUserId);
    List<ShortlistedCandidate> findByJobPostRecruiterUserIdOrderByAddedAtDesc(Long recruiterUserId);
}

