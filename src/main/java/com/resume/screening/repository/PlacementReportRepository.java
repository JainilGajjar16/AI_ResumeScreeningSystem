package com.resume.screening.repository;

import com.resume.screening.entity.PlacementReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PlacementReportRepository extends JpaRepository<PlacementReport, Long> {
    List<PlacementReport> findByAcademicYear(String academicYear);
}
