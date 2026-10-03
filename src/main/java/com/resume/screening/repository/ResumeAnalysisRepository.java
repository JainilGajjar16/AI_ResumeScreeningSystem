package com.resume.screening.repository;

import com.resume.screening.entity.ResumeAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResumeAnalysisRepository extends JpaRepository<ResumeAnalysis, Long> {

    Optional<ResumeAnalysis> findByResumeIdAndJobPostIdAndResumeVersionNumber(Long resumeId, Long jobId, Integer resumeVersionNumber);

    List<ResumeAnalysis> findByResumeIdAndJobPostId(Long resumeId, Long jobId);

    List<ResumeAnalysis> findByResumeId(Long resumeId);

    List<ResumeAnalysis> findByJobPostId(Long jobId);

    List<ResumeAnalysis> findByStudentProfileId(Long studentId);
}
