package com.resume.screening.repository;

import com.resume.screening.entity.JobPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface JobPostRepository extends JpaRepository<JobPost, Long> {
    List<JobPost> findByRecruiterId(Long recruiterId);
    List<JobPost> findByRecruiterIdOrderByCreatedAtDesc(Long recruiterId);
    List<JobPost> findByStatus(String status);
    List<JobPost> findByStatusOrderByCreatedAtDesc(String status);
    List<JobPost> findByStatusAndDeadlineGreaterThanEqualOrderByCreatedAtDesc(String status, LocalDate date);
    long countByRecruiterId(Long recruiterId);
    long countByRecruiterIdAndStatus(Long recruiterId, String status);
}
