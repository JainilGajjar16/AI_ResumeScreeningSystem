package scratch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Set;

public class AnnotationSummaryRunner {
    public static void main(String[] args) throws Exception {
        File file = new File("ai-resume-engine/data/annotations/human_annotation.csv");
        if (!file.exists()) {
            System.out.println("Error: File not found " + file.getAbsolutePath());
            return;
        }

        int totalPairs = 0;
        int pendingPairs = 0;
        int reviewedPairs = 0;
        int highCount = 0;
        int mediumCount = 0;
        int lowCount = 0;
        int adjudicationCount = 0;
        int missingReasons = 0;

        Set<String> seenIds = new HashSet<>();
        Set<String> duplicates = new HashSet<>();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String header = br.readLine(); // skip header
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                totalPairs++;

                // parse CSV line
                String[] parts = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
                if (parts.length >= 1) {
                    String pairId = parts[0].trim();
                    if (seenIds.contains(pairId)) {
                        duplicates.add(pairId);
                    } else {
                        seenIds.add(pairId);
                    }
                }

                String reviewerId = parts.length > 5 ? parts[5].trim() : "";
                String reviewerLabel = parts.length > 6 ? parts[6].trim().toUpperCase() : "";
                String reviewerReason = parts.length > 7 ? parts[7].trim() : "";
                String finalLabel = parts.length > 8 ? parts[8].trim().toUpperCase() : "";
                String status = parts.length > 9 ? parts[9].trim().toUpperCase() : "PENDING";

                String activeLabel = !finalLabel.isEmpty() ? finalLabel : reviewerLabel;

                if ("PENDING".equals(status) || activeLabel.isEmpty()) {
                    pendingPairs++;
                } else {
                    reviewedPairs++;
                }

                if ("HIGH".equals(activeLabel)) highCount++;
                else if ("MEDIUM".equals(activeLabel)) mediumCount++;
                else if ("LOW".equals(activeLabel)) lowCount++;

                if ("ADJUDICATION_REQUIRED".equals(status)) adjudicationCount++;
                if (!activeLabel.isEmpty() && reviewerReason.isEmpty()) missingReasons++;
            }
        }

        System.out.println("==================================================");
        System.out.println("      HUMAN ANNOTATION STATUS SUMMARY REPORT      ");
        System.out.println("==================================================");
        System.out.println("Annotation File Path         : " + file.getAbsolutePath());
        System.out.println("Total Candidate-Job Pairs    : " + totalPairs);
        System.out.println("Pending Annotation Count     : " + pendingPairs);
        System.out.println("Reviewed Pairs Count         : " + reviewedPairs);
        System.out.println("--------------------------------------------------");
        System.out.println("Label Breakdown:");
        System.out.println("  - HIGH Match Count         : " + highCount);
        System.out.println("  - MEDIUM Match Count       : " + mediumCount);
        System.out.println("  - LOW Match Count          : " + lowCount);
        System.out.println("--------------------------------------------------");
        System.out.println("Adjudication Required Count  : " + adjudicationCount);
        System.out.println("Missing Reviewer Reasons     : " + missingReasons);
        System.out.println("Duplicate Pair IDs Count     : " + duplicates.size());
        System.out.println("==================================================");
    }
}
