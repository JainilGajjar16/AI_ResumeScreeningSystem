package com.resume.screening.repository;

import com.resume.screening.entity.InterviewFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface InterviewFeedbackRepository extends JpaRepository<InterviewFeedback, Long> {
    List<InterviewFeedback> findByJobApplicationId(Long jobApplicationId);
    List<InterviewFeedback> findByInterviewerId(Long interviewerId);
}
