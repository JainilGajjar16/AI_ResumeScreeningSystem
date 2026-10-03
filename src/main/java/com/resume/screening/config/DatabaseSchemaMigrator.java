package com.resume.screening.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseSchemaMigrator {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseSchemaMigrator.class);

    private final JdbcTemplate jdbcTemplate;

    public DatabaseSchemaMigrator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void migrateSchema() {
        try {
            logger.info("Executing database schema migration for nullable resume_id columns...");

            // 1. Modify job_applications.resume_id to allow NULL values
            try {
                jdbcTemplate.execute("ALTER TABLE job_applications MODIFY COLUMN resume_id BIGINT NULL");
                logger.info("Successfully altered job_applications.resume_id to NULL.");
            } catch (Exception ex) {
                logger.warn("Could not alter job_applications.resume_id: {}", ex.getMessage());
            }

            // 2. Modify resume_analyses.resume_id to allow NULL values
            try {
                jdbcTemplate.execute("ALTER TABLE resume_analyses MODIFY COLUMN resume_id BIGINT NULL");
                logger.info("Successfully altered resume_analyses.resume_id to NULL.");
            } catch (Exception ex) {
                logger.warn("Could not alter resume_analyses.resume_id: {}", ex.getMessage());
            }

            // 3. Modify resume_recommendations.resume_id to allow NULL values if table exists
            try {
                jdbcTemplate.execute("ALTER TABLE resume_recommendations MODIFY COLUMN resume_id BIGINT NULL");
                logger.info("Successfully altered resume_recommendations.resume_id to NULL.");
            } catch (Exception ex) {
                logger.warn("Could not alter resume_recommendations.resume_id: {}", ex.getMessage());
            }

        } catch (Exception ex) {
            logger.error("Error during database schema migration: {}", ex.getMessage());
        }
    }
}
