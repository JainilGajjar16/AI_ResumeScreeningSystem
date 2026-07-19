package com.resume.screening.repository;

import com.resume.screening.entity.JobPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface JobPostRepository extends JpaRepository<JobPost, Long> {
    List<JobPost> findByRecruiterId(Long recruiterId);
    List<JobPost> findByStatus(String status);
}
