import pandas as pd
import os

base_dir = r"C:\Users\jaini\Documents\AI-ResumeScreeningSystem\ai-resume-engine\data\annotations"
df_rev = pd.read_csv(os.path.join(base_dir, "annotation_reviews.csv"))
df_unlabeled = pd.read_csv(os.path.join(base_dir, "unlabeled_pairs.csv"))

print("=== REVIEWS CSV ===")
print("Columns:", df_rev.columns.tolist())
print(df_rev.head(3))

print("\n=== UNLABELED CSV ===")
print("Columns:", df_unlabeled.columns.tolist())
print(df_unlabeled.head(3))

merged = pd.merge(df_rev, df_unlabeled, on="pair_id", how="left")
print("\n=== MERGED HEAD ===")
print("Merged Columns:", merged.columns.tolist())
for idx, row in merged.head(3).iterrows():
    print(f"\n--- Row {idx} ({row['pair_id']}) ---")
    print("Resume ID:", row['resume_id'])
    print("Job ID   :", row['job_id'])
    print("Label    :", row['reviewer_label'])
    print("Resume Text Snippet:", repr(row['resume_raw_text'][:150]))
    print("Job Text Snippet   :", repr(row['job_raw_text'][:150]))
