package com.resume.screening.repository;

import com.resume.screening.entity.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {
    List<JobApplication> findByStudentProfileId(Long studentProfileId);
    List<JobApplication> findByStudentProfileIdOrderByAppliedAtDesc(Long studentProfileId);
    List<JobApplication> findByJobPostId(Long jobPostId);
    List<JobApplication> findByJobPostIdOrderByAppliedAtDesc(Long jobPostId);
    boolean existsByJobPostIdAndStudentProfileId(Long jobPostId, Long studentProfileId);
    java.util.Optional<JobApplication> findByJobPostIdAndStudentProfileId(Long jobPostId, Long studentProfileId);
    long countByJobPostRecruiterId(Long recruiterId);
    long countByJobPostId(Long jobPostId);
    List<JobApplication> findByResumeId(Long resumeId);
    boolean existsByJobPostRecruiterIdAndResumeId(Long recruiterId, Long resumeId);
}
