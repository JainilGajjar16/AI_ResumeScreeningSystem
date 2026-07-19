package com.resume.screening.repository;

import com.resume.screening.entity.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    List<ActivityLog> findByUserIdOrderByPerformedAtDesc(Long userId);
}
