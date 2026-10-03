package com.resume.screening.repository;

import com.resume.screening.entity.Interview;
import com.resume.screening.entity.JobApplication;
import com.resume.screening.entity.Recruiter;
import com.resume.screening.entity.StudentProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InterviewRepository extends JpaRepository<Interview, Long> {

    List<Interview> findByRecruiter(Recruiter recruiter);

    List<Interview> findByRecruiterId(Long recruiterId);

    List<Interview> findByRecruiter_User_Username(String username);

    List<Interview> findByStudent(StudentProfile student);

    List<Interview> findByStudentId(Long studentId);

    List<Interview> findByStudent_User_Username(String username);

    List<Interview> findByJobApplication(JobApplication jobApplication);

    List<Interview> findByJobApplicationId(Long jobApplicationId);

    boolean existsByJobApplicationIdAndStatusIn(Long jobApplicationId, List<com.resume.screening.entity.InterviewStatus> statuses);

    Optional<Interview> findByIdAndRecruiter_User_Username(Long id, String username);

    Optional<Interview> findByIdAndStudent_User_Username(Long id, String username);
}
