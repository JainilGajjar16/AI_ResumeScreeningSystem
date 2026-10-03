"""
AI Resume Screening System - Phase 4A Dataset Validation Script
Workspace: ai-resume-engine/
Dataset: data/annotations/annotation_reviews.csv
"""

import os
import sys
import shutil
import pandas as pd
import random

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
DATA_DIR = os.path.join(BASE_DIR, "data", "annotations")
REVIEWS_CSV = os.path.join(DATA_DIR, "annotation_reviews.csv")
BACKUP_CSV = os.path.join(DATA_DIR, "annotation_reviews.csv.bak")
UNLABELED_CSV = os.path.join(DATA_DIR, "unlabeled_pairs.csv")

EXPECTED_TOTAL_PAIRS = 675
EXPECTED_REVIEWERS = {
    "REV_A": ("Jainil", 225, "PAIR_0001", "PAIR_0225"),
    "REV_B": ("Kashish", 225, "PAIR_0226", "PAIR_0450"),
    "REV_C": ("Khush", 225, "PAIR_0451", "PAIR_0675")
}
VALID_LABELS = {"HIGH", "MEDIUM", "LOW"}

def run_validation():
    print("==========================================================================")
    print("          AI RESUME SCREENING SYSTEM - PHASE 4A DATASET VALIDATION        ")
    print("==========================================================================")
    
    validation_passed = True
    failures = []

    # 1. Backup Verification / Creation
    print("\n--- 1. FILE & BACKUP INTEGRITY ---")
    if not os.path.exists(REVIEWS_CSV):
        print(f"[FAIL] Original annotation CSV not found: {REVIEWS_CSV}")
        failures.append("Original annotation CSV missing.")
        return False
    
    print(f"[PASS] Original dataset present: {REVIEWS_CSV}")
    
    if not os.path.exists(BACKUP_CSV):
        shutil.copyfile(REVIEWS_CSV, BACKUP_CSV)
        print(f"[ACTION] Created backup dataset copy: {BACKUP_CSV}")
    else:
        print(f"[PASS] Verified dataset backup exists: {BACKUP_CSV}")

    # Read original CSV
    df_rev = pd.read_csv(REVIEWS_CSV)

    # 2. Row Count & Unique Pair ID Check
    print("\n--- 2. ROW COUNT & UNIQUE PAIR ID VALIDATION ---")
    total_rows = len(df_rev)
    unique_pairs = df_rev['pair_id'].nunique()
    duplicate_pair_count = df_rev['pair_id'].duplicated().sum()

    print(f"Total Row Count         : {total_rows} (Expected: {EXPECTED_TOTAL_PAIRS})")
    print(f"Unique Pair IDs Count   : {unique_pairs} (Expected: {EXPECTED_TOTAL_PAIRS})")
    print(f"Duplicate Pair ID Count : {duplicate_pair_count} (Expected: 0)")

    if total_rows != EXPECTED_TOTAL_PAIRS:
        print(f"[FAIL] Row count mismatch: found {total_rows}, expected {EXPECTED_TOTAL_PAIRS}")
        validation_passed = False
        failures.append(f"Row count mismatch ({total_rows} vs {EXPECTED_TOTAL_PAIRS}).")
    else:
        print("[PASS] Row count matches expected 675 pairs.")

    if duplicate_pair_count > 0:
        print(f"[FAIL] Found {duplicate_pair_count} duplicate pair IDs.")
        validation_passed = False
        failures.append(f"Duplicate pair IDs found ({duplicate_pair_count}).")
    else:
        print("[PASS] Zero duplicate pair IDs found.")

    # 3. Reviewer Partition & Breakdown Check
    print("\n--- 3. REVIEWER COUNTS & PARTITION BREAKDOWN ---")
    reviewer_counts = df_rev['reviewer_id'].value_counts().to_dict()
    
    for rev_id, (rev_name, exp_count, min_id, max_id) in EXPECTED_REVIEWERS.items():
        act_count = reviewer_counts.get(rev_id, 0)
        sub_df = df_rev[df_rev['reviewer_id'] == rev_id]
        actual_min_id = sub_df['pair_id'].min() if not sub_df.empty else "N/A"
        actual_max_id = sub_df['pair_id'].max() if not sub_df.empty else "N/A"
        
        print(f"Reviewer {rev_id} ({rev_name:7s}): {act_count:3d} reviews (Expected: {exp_count:3d}) | Range: {actual_min_id} to {actual_max_id}")
        
        if act_count != exp_count:
            print(f"[FAIL] Reviewer {rev_id} count mismatch: {act_count} vs {exp_count}")
            validation_passed = False
            failures.append(f"Reviewer {rev_id} count mismatch.")
        elif actual_min_id != min_id or actual_max_id != max_id:
            print(f"[FAIL] Reviewer {rev_id} range mismatch: {actual_min_id}-{actual_max_id} vs {min_id}-{max_id}")
            validation_passed = False
            failures.append(f"Reviewer {rev_id} range mismatch.")

    if validation_passed:
        print("[PASS] All 3 reviewers (REV_A, REV_B, REV_C) completed exactly 225 reviews each in expected ranges.")

    # 4. Label Integrity & Distribution Check
    print("\n--- 4. LABEL INTEGRITY & DISTRIBUTION ---")
    labels = df_rev['reviewer_label'].astype(str).str.strip().str.upper()
    invalid_labels = df_rev[~labels.isin(VALID_LABELS)]
    missing_labels = df_rev[df_rev['reviewer_label'].isna() | (df_rev['reviewer_label'].astype(str).str.strip() == '')]
    missing_reasons = df_rev[df_rev['reviewer_reason'].isna() | (df_rev['reviewer_reason'].astype(str).str.strip() == '')]

    print(f"Invalid Labels Count    : {len(invalid_labels)} (Expected: 0)")
    print(f"Missing Labels Count    : {len(missing_labels)} (Expected: 0)")
    print(f"Missing Reasons Count   : {len(missing_reasons)} (Expected: 0)")

    if len(invalid_labels) > 0:
        print(f"[FAIL] Invalid label values found: {invalid_labels['reviewer_label'].unique()}")
        validation_passed = False
        failures.append("Invalid label values found.")
    else:
        print("[PASS] All labels are strictly valid (HIGH, MEDIUM, LOW).")

    if len(missing_labels) > 0:
        print(f"[FAIL] Found {len(missing_labels)} missing labels.")
        validation_passed = False
        failures.append("Missing labels present.")
    else:
        print("[PASS] Zero missing labels.")

    if len(missing_reasons) > 0:
        print(f"[FAIL] Found {len(missing_reasons)} missing reviewer reasons.")
        validation_passed = False
        failures.append("Missing reviewer reasons present.")
    else:
        print("[PASS] Zero missing reviewer reasons.")

    print("\nOverall Label Distribution:")
    label_counts = df_rev['reviewer_label'].value_counts()
    for lbl in ["LOW", "MEDIUM", "HIGH"]:
        cnt = label_counts.get(lbl, 0)
        pct = (cnt / total_rows) * 100
        print(f"  - {lbl:6s} : {cnt:3d} ({pct:6.2f}%)")

    print("\nLabel Breakdown per Reviewer:")
    ct = pd.crosstab(df_rev['reviewer_id'], df_rev['reviewer_label'])
    print(ct)

    # 5. Join Integrity & Source Text Verification
    print("\n--- 5. SOURCE DATA JOIN & TEXT COMPLETENESS ---")
    if not os.path.exists(UNLABELED_CSV):
        print(f"[FAIL] Unlabeled pairs CSV missing: {UNLABELED_CSV}")
        validation_passed = False
        failures.append("Unlabeled pairs dataset missing.")
        merged = df_rev.copy()
    else:
        df_unlabeled = pd.read_csv(UNLABELED_CSV)
        merged = pd.merge(df_rev, df_unlabeled, on='pair_id', how='left')
        
        print(f"Unlabeled dataset size  : {len(df_unlabeled)} pairs")
        print(f"Merged dataset size     : {len(merged)} pairs")
        
        missing_res_id = merged['resume_id'].isna().sum()
        missing_job_id = merged['job_id'].isna().sum()
        
        merged['resume_len'] = merged['resume_raw_text'].astype(str).str.strip().str.len()
        merged['job_len'] = merged['job_raw_text'].astype(str).str.strip().str.len()
        
        empty_res_text = (merged['resume_len'] == 0).sum()
        empty_job_text = (merged['job_len'] == 0).sum()

        print(f"Missing Resume IDs      : {missing_res_id} (Expected: 0)")
        print(f"Missing Job IDs         : {missing_job_id} (Expected: 0)")
        print(f"Empty Resume Text Rows  : {empty_res_text} (Expected: 0)")
        print(f"Empty Job Text Rows     : {empty_job_text} (Expected: 0)")

        print(f"Resume text length range: {merged['resume_len'].min()} to {merged['resume_len'].max()} chars (Avg: {merged['resume_len'].mean():.1f})")
        print(f"Job text length range   : {merged['job_len'].min()} to {merged['job_len'].max()} chars (Avg: {merged['job_len'].mean():.1f})")

        if missing_res_id > 0 or missing_job_id > 0 or empty_res_text > 0 or empty_job_text > 0:
            print("[FAIL] Missing or empty text/id references found during join.")
            validation_passed = False
            failures.append("Missing source data text or IDs during join.")
        else:
            print("[PASS] Successful 100% inner join with source dataset. All candidate/job IDs and raw text fields present.")

    # 6. Data Leakage Analysis & Grouped Split Strategy
    print("\n--- 6. DATA LEAKAGE ANALYSIS & SPLIT PLANNING ---")
    if 'resume_id' in merged.columns:
        n_unique_resumes = merged['resume_id'].nunique()
        n_unique_jobs = merged['job_id'].nunique()
        print(f"Unique Candidate Resumes : {n_unique_resumes}")
        print(f"Unique Job Descriptions   : {n_unique_jobs}")
        print(f"Average pairs per resume  : {len(merged)/n_unique_resumes:.2f}")

        # Simulate candidate-grouped train/val/test split
        random.seed(42)
        unique_resumes = list(merged['resume_id'].unique())
        random.shuffle(unique_resumes)

        n_train = int(len(unique_resumes) * 0.70)
        n_val = int(len(unique_resumes) * 0.15)

        train_res = set(unique_resumes[:n_train])
        val_res = set(unique_resumes[n_train:n_train+n_val])
        test_res = set(unique_resumes[n_train+n_val:])

        train_df = merged[merged['resume_id'].isin(train_res)]
        val_df = merged[merged['resume_id'].isin(val_res)]
        test_df = merged[merged['resume_id'].isin(test_res)]

        print(f"\nGrouped Split Simulation by Candidate Resume ID:")
        print(f"  - Train Set : {len(train_df):3d} pairs ({len(train_df)/len(merged):5.1%}), {len(train_res):3d} resumes")
        print(f"  - Val Set   : {len(val_df):3d} pairs ({len(val_df)/len(merged):5.1%}), {len(val_res):3d} resumes")
        print(f"  - Test Set  : {len(test_df):3d} pairs ({len(test_df)/len(merged):5.1%}), {len(test_res):3d} resumes")

        leakage_tr_va = len(train_res.intersection(val_res))
        leakage_tr_te = len(train_res.intersection(test_res))
        leakage_va_te = len(val_res.intersection(test_res))

        print(f"  - Candidate Resume Leakage across splits: {leakage_tr_va + leakage_tr_te + leakage_va_te}")
        if (leakage_tr_va + leakage_tr_te + leakage_va_te) == 0:
            print("[PASS] Grouped splitting strictly prevents resume leakage across train/validation/test sets.")
        else:
            print("[FAIL] Resume leakage detected.")
            validation_passed = False

    # Summary Result
    print("\n==========================================================================")
    print("                           VALIDATION SUMMARY                             ")
    print("==========================================================================")
    if validation_passed:
        print("RESULT: [PASS] DATASET VALIDATION PASSED SUCCESSFULLY!")
        print("All 675 candidate-job pairs are valid, complete, uncorrupted, and verified.")
        print("==========================================================================")
        return True
    else:
        print("RESULT: [FAIL] DATASET VALIDATION FAILED!")
        print(f"Failure reasons: {failures}")
        print("==========================================================================")
        return False

if __name__ == "__main__":
    success = run_validation()
    sys.exit(0 if success else 1)
