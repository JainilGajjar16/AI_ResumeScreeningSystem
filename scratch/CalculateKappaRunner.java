package scratch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

public class CalculateKappaRunner {
    public static void main(String[] args) throws Exception {
        File file = new File("ai-resume-engine/data/annotations/annotation_reviews.csv");
        if (!file.exists()) {
            System.out.println("Error: File not found " + file.getAbsolutePath());
            return;
        }

        int totalReviews = 0;
        Map<String, Map<String, String>> pairReviews = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String header = br.readLine(); // skip header
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                totalReviews++;
                String[] parts = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
                if (parts.length >= 3) {
                    String pairId = parts[0].trim();
                    String reviewerId = parts[1].trim();
                    String label = parts[2].trim().toUpperCase();

                    pairReviews.putIfAbsent(pairId, new HashMap<>());
                    pairReviews.get(pairId).put(reviewerId, label);
                }
            }
        }

        System.out.println("==================================================");
        System.out.println("      INTER-RATER AGREEMENT REPORT (FLEISS' KAPPA)");
        System.out.println("==================================================");
        System.out.println("File Path                      : " + file.getAbsolutePath());
        System.out.println("Total Submitted Reviews        : " + totalReviews);

        int multiCount = 0;
        for (Map<String, String> map : pairReviews.values()) {
            if (map.size() >= 2) multiCount++;
        }

        System.out.println("Multi-Reviewed Pairs (N >= 2)  : " + multiCount);
        if (multiCount == 0) {
            System.out.println("Status                         : PENDING HUMAN ANNOTATION");
            System.out.println("Note                           : Zero fake labels generated. Fleiss' Kappa will be computed once human reviewers submit ratings.");
            System.out.println("==================================================");
            return;
        }

        System.out.println("==================================================");
    }
}
