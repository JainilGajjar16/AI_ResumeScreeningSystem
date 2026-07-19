package com.resume.screening.repository;

import com.resume.screening.entity.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {
    List<JobApplication> findByStudentProfileId(Long studentProfileId);
    List<JobApplication> findByJobPostId(Long jobPostId);
    boolean existsByJobPostIdAndStudentProfileId(Long jobPostId, Long studentProfileId);
}
