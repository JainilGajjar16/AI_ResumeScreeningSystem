package com.resume.screening.repository;

import com.resume.screening.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Long> {
    Optional<Skill> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
}
