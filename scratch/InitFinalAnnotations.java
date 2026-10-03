package scratch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;

public class InitFinalAnnotations {
    public static void main(String[] args) throws Exception {
        File unlabeledFile = new File("ai-resume-engine/data/annotations/unlabeled_pairs.csv");
        File finalFile = new File("ai-resume-engine/data/annotations/final_annotations.csv");

        if (!unlabeledFile.exists()) {
            System.out.println("Error: unlabeled_pairs.csv missing.");
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(unlabeledFile));
             PrintWriter pw = new PrintWriter(new FileWriter(finalFile))) {

            pw.println("pair_id,final_label,final_reason,review_status,adjudicator_id");

            String line = br.readLine(); // skip header
            int count = 0;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
                if (parts.length > 0) {
                    String pairId = parts[0].trim();
                    pw.println(pairId + ",,,PENDING,");
                    count++;
                }
            }
            System.out.println("Initialized final_annotations.csv with " + count + " pending records.");
        }
    }
}
