import csv
import os
import sys

def summarize_annotations(file_path):
    if not os.path.exists(file_path):
        print(f"Error: Annotation file not found at {file_path}")
        return

    total_pairs = 0
    pending_pairs = 0
    reviewed_pairs = 0
    high_count = 0
    medium_count = 0
    low_count = 0
    adjudication_required_count = 0
    missing_reasons_count = 0

    seen_pair_ids = set()
    duplicate_pair_ids = set()

    with open(file_path, mode='r', encoding='utf-8') as f:
        reader = csv.DictReader(f)
        for row in reader:
            total_pairs += 1
            pair_id = (row.get("pair_id") or "").strip()

            if pair_id in seen_pair_ids:
                duplicate_pair_ids.add(pair_id)
            else:
                seen_pair_ids.add(pair_id)

            status = (row.get("review_status") or "").strip().upper()
            final_label = (row.get("final_label") or "").strip().upper()
            reviewer_label = (row.get("reviewer_label") or "").strip().upper()
            reviewer_reason = (row.get("reviewer_reason") or "").strip()

            label_to_count = final_label if final_label else reviewer_label

            if status == "PENDING" or not label_to_count:
                pending_pairs += 1
            else:
                reviewed_pairs += 1

            if label_to_count == "HIGH":
                high_count += 1
            elif label_to_count == "MEDIUM":
                medium_count += 1
            elif label_to_count == "LOW":
                low_count += 1

            if status == "ADJUDICATION_REQUIRED":
                adjudication_required_count += 1

            if (reviewer_label or final_label) and not reviewer_reason:
                missing_reasons_count += 1

    print("==================================================")
    print("      HUMAN ANNOTATION STATUS SUMMARY REPORT      ")
    print("==================================================")
    print(f"Annotation File Path         : {file_path}")
    print(f"Total Candidate-Job Pairs    : {total_pairs}")
    print(f"Pending Annotation Count     : {pending_pairs}")
    print(f"Reviewed Pairs Count         : {reviewed_pairs}")
    print("--------------------------------------------------")
    print("Label Breakdown:")
    print(f"  - HIGH Match Count         : {high_count}")
    print(f"  - MEDIUM Match Count       : {medium_count}")
    print(f"  - LOW Match Count          : {low_count}")
    print("--------------------------------------------------")
    print(f"Adjudication Required Count  : {adjudication_required_count}")
    print(f"Missing Reviewer Reasons     : {missing_reasons_count}")
    print(f"Duplicate Pair IDs Count     : {len(duplicate_pair_ids)}")
    if duplicate_pair_ids:
        print(f"  Duplicate IDs              : {sorted(list(duplicate_pair_ids))}")
    print("==================================================")

if __name__ == "__main__":
    default_path = os.path.join("ai-resume-engine", "data", "annotations", "human_annotation.csv")
    target_path = sys.argv[1] if len(sys.argv) > 1 else default_path
    summarize_annotations(target_path)
