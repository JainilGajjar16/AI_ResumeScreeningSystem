package com.resume.screening.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.recommendation")
@Data
public class CandidateRecommendationConfig {

    private double highThreshold = 80.0;
    private double mediumThreshold = 60.0;

    public String getRecommendationLabel(Double atsScore) {
        if (atsScore == null) {
            return "ATS Analysis Not Available";
        }
        if (atsScore >= highThreshold) {
            return "Highly Compatible";
        } else if (atsScore >= mediumThreshold) {
            return "Compatible";
        } else {
            return "Needs Review";
        }
    }

    public String getBadgeClass(Double atsScore) {
        if (atsScore == null) {
            return "bg-secondary";
        }
        if (atsScore >= highThreshold) {
            return "bg-success";
        } else if (atsScore >= mediumThreshold) {
            return "bg-info text-dark";
        } else {
            return "bg-warning text-dark";
        }
    }
}
