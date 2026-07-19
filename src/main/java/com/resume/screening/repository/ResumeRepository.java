package com.resume.screening.repository;

import com.resume.screening.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, Long> {
    List<Resume> findByStudentProfileId(Long studentProfileId);
}
