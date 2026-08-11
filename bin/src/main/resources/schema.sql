-- AI Resume Screening & Smart Candidate Management System DDL Schema
-- Compatible with MySQL (XAMPP / phpMyAdmin)

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS `placement_reports`;
DROP TABLE IF EXISTS `interview_feedback`;
DROP TABLE IF EXISTS `activity_logs`;
DROP TABLE IF EXISTS `notifications`;
DROP TABLE IF EXISTS `shortlisted_candidates`;
DROP TABLE IF EXISTS `resume_recommendations`;
DROP TABLE IF EXISTS `ats_scores`;
DROP TABLE IF EXISTS `ai_analysis`;
DROP TABLE IF EXISTS `job_applications`;
DROP TABLE IF EXISTS `job_posts`;
DROP TABLE IF EXISTS `resume_versions`;
DROP TABLE IF EXISTS `resumes`;
DROP TABLE IF EXISTS `student_skills`;
DROP TABLE IF EXISTS `skills`;
DROP TABLE IF EXISTS `student_profiles`;
DROP TABLE IF EXISTS `recruiters`;
DROP TABLE IF EXISTS `users`;
SET FOREIGN_KEY_CHECKS = 1;

-- 1. users table
CREATE TABLE `users` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL,
    `email` VARCHAR(100) NOT NULL UNIQUE,
    `role` VARCHAR(20) NOT NULL, -- e.g., 'ADMIN', 'RECRUITER', 'STUDENT'
    `first_name` VARCHAR(50) DEFAULT NULL,
    `last_name` VARCHAR(50) DEFAULT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_users_username` (`username`),
    INDEX `idx_users_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. recruiters table
CREATE TABLE `recruiters` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL UNIQUE,
    `company_name` VARCHAR(100) NOT NULL,
    `company_website` VARCHAR(255) DEFAULT NULL,
    `designation` VARCHAR(100) DEFAULT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_recruiters_users` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. student_profiles table
CREATE TABLE `student_profiles` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL UNIQUE,
    `phone` VARCHAR(20) DEFAULT NULL,
    `github_url` VARCHAR(255) DEFAULT NULL,
    `linkedin_url` VARCHAR(255) DEFAULT NULL,
    `summary` TEXT DEFAULT NULL,
    `education` TEXT DEFAULT NULL,
    `experience` TEXT DEFAULT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_students_users` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. skills table
CREATE TABLE `skills` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL UNIQUE,
    INDEX `idx_skills_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. student_skills table (Many-to-Many Join Table)
CREATE TABLE `student_skills` (
    `student_id` BIGINT NOT NULL,
    `skill_id` BIGINT NOT NULL,
    PRIMARY KEY (`student_id`, `skill_id`),
    CONSTRAINT `fk_student_skills_student` FOREIGN KEY (`student_id`) REFERENCES `student_profiles` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_student_skills_skill` FOREIGN KEY (`skill_id`) REFERENCES `skills` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. resumes table
CREATE TABLE `resumes` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `student_id` BIGINT NOT NULL,
    `file_path` VARCHAR(255) NOT NULL,
    `file_name` VARCHAR(255) NOT NULL,
    `file_type` VARCHAR(50) DEFAULT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_resumes_student` FOREIGN KEY (`student_id`) REFERENCES `student_profiles` (`id`) ON DELETE CASCADE,
    INDEX `idx_resumes_student_id` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. resume_versions table
CREATE TABLE `resume_versions` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `resume_id` BIGINT NOT NULL,
    `version_number` INT NOT NULL,
    `file_path` VARCHAR(255) NOT NULL,
    `commit_message` VARCHAR(255) DEFAULT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_resume_versions_resume` FOREIGN KEY (`resume_id`) REFERENCES `resumes` (`id`) ON DELETE CASCADE,
    INDEX `idx_versions_resume_id` (`resume_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. job_posts table
CREATE TABLE `job_posts` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `recruiter_id` BIGINT NOT NULL,
    `title` VARCHAR(150) NOT NULL,
    `description` TEXT NOT NULL,
    `requirements` TEXT DEFAULT NULL,
    `location` VARCHAR(100) DEFAULT NULL,
    `job_type` VARCHAR(50) DEFAULT NULL, -- e.g., 'FULL_TIME', 'PART_TIME', 'INTERNSHIP'
    `status` VARCHAR(20) DEFAULT 'ACTIVE', -- e.g., 'ACTIVE', 'CLOSED'
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_job_posts_recruiter` FOREIGN KEY (`recruiter_id`) REFERENCES `recruiters` (`id`) ON DELETE CASCADE,
    INDEX `idx_job_posts_recruiter` (`recruiter_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. job_applications table
CREATE TABLE `job_applications` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `job_post_id` BIGINT NOT NULL,
    `student_id` BIGINT NOT NULL,
    `resume_id` BIGINT NOT NULL,
    `status` VARCHAR(50) DEFAULT 'APPLIED', -- e.g., 'APPLIED', 'SCREENED', 'SHORTLISTED', 'REJECTED'
    `applied_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_applications_job_post` FOREIGN KEY (`job_post_id`) REFERENCES `job_posts` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_applications_student` FOREIGN KEY (`student_id`) REFERENCES `student_profiles` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_applications_resume` FOREIGN KEY (`resume_id`) REFERENCES `resumes` (`id`) ON DELETE CASCADE,
    INDEX `idx_applications_job_post` (`job_post_id`),
    INDEX `idx_applications_student` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. ai_analysis table
CREATE TABLE `ai_analysis` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `resume_id` BIGINT NOT NULL UNIQUE,
    `parsed_text` LONGTEXT DEFAULT NULL,
    `key_skills_found` TEXT DEFAULT NULL,
    `experience_score` DECIMAL(5, 2) DEFAULT NULL,
    `sentiment_score` DECIMAL(5, 2) DEFAULT NULL,
    `recommendations` TEXT DEFAULT NULL,
    `analyzed_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_ai_analysis_resume` FOREIGN KEY (`resume_id`) REFERENCES `resumes` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 11. ats_scores table
CREATE TABLE `ats_scores` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `job_application_id` BIGINT NOT NULL UNIQUE,
    `overall_score` DECIMAL(5, 2) NOT NULL,
    `skill_match_score` DECIMAL(5, 2) DEFAULT NULL,
    `experience_match_score` DECIMAL(5, 2) DEFAULT NULL,
    `education_match_score` DECIMAL(5, 2) DEFAULT NULL,
    `feedback` TEXT DEFAULT NULL,
    `scored_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_ats_scores_application` FOREIGN KEY (`job_application_id`) REFERENCES `job_applications` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 12. resume_recommendations table
CREATE TABLE `resume_recommendations` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `job_post_id` BIGINT NOT NULL,
    `resume_id` BIGINT NOT NULL,
    `match_percentage` DECIMAL(5, 2) NOT NULL,
    `reason` TEXT DEFAULT NULL,
    `recommended_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_recommendations_job_post` FOREIGN KEY (`job_post_id`) REFERENCES `job_posts` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_recommendations_resume` FOREIGN KEY (`resume_id`) REFERENCES `resumes` (`id`) ON DELETE CASCADE,
    INDEX `idx_rec_job_post` (`job_post_id`),
    INDEX `idx_rec_resume` (`resume_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 13. shortlisted_candidates table
CREATE TABLE `shortlisted_candidates` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `job_post_id` BIGINT NOT NULL,
    `student_id` BIGINT NOT NULL,
    `added_by` BIGINT NOT NULL,
    `status` VARCHAR(50) DEFAULT 'SHORTLISTED', -- e.g., 'SHORTLISTED', 'INTERVIEW_SCHEDULED', 'HIRED', 'REJECTED'
    `added_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_shortlisted_job_post` FOREIGN KEY (`job_post_id`) REFERENCES `job_posts` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_shortlisted_student` FOREIGN KEY (`student_id`) REFERENCES `student_profiles` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_shortlisted_added_by` FOREIGN KEY (`added_by`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    INDEX `idx_shortlist_job_post` (`job_post_id`),
    INDEX `idx_shortlist_student` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 14. notifications table
CREATE TABLE `notifications` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `title` VARCHAR(150) NOT NULL,
    `message` TEXT NOT NULL,
    `is_read` BOOLEAN DEFAULT FALSE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_notifications_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    INDEX `idx_notifications_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 15. activity_logs table
CREATE TABLE `activity_logs` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `action` VARCHAR(255) NOT NULL,
    `ip_address` VARCHAR(45) DEFAULT NULL,
    `performed_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_activity_logs_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    INDEX `idx_activity_logs_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 16. interview_feedback table
CREATE TABLE `interview_feedback` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `job_application_id` BIGINT NOT NULL,
    `interviewer_id` BIGINT NOT NULL,
    `rating` INT NOT NULL, -- Rating out of 5
    `comments` TEXT DEFAULT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_feedback_application` FOREIGN KEY (`job_application_id`) REFERENCES `job_applications` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_feedback_interviewer` FOREIGN KEY (`interviewer_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    INDEX `idx_feedback_application` (`job_application_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 17. placement_reports table
CREATE TABLE `placement_reports` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `academic_year` VARCHAR(20) NOT NULL,
    `total_students` INT NOT NULL,
    `total_placed` INT NOT NULL,
    `average_package` DECIMAL(12, 2) DEFAULT NULL,
    `top_recruiter` VARCHAR(150) DEFAULT NULL,
    `generated_by` BIGINT NOT NULL,
    `generated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_reports_user` FOREIGN KEY (`generated_by`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
