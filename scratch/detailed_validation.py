import pandas as pd
import os
import json

base_path = r"C:\Users\jaini\Documents\AI-ResumeScreeningSystem\ai-resume-engine\data"
reviews_path = os.path.join(base_path, "annotations", "annotation_reviews.csv")
unlabeled_path = os.path.join(base_path, "annotations", "unlabeled_pairs.csv")
human_ann_path = os.path.join(base_path, "annotations", "human_annotation.csv")
final_ann_path = os.path.join(base_path, "annotations", "final_annotations.csv")

print("--- 1. ANNOTATION_REVIEWS.CSV CHECK ---")
df_rev = pd.read_csv(reviews_path)
print("Total rows:", len(df_rev))
print("Columns:", df_rev.columns.tolist())
print("Unique pair_ids:", df_rev['pair_id'].nunique())
print("Duplicate pair_id count:", df_rev['pair_id'].duplicated().sum())

print("\nReviewer value counts:")
print(df_rev['reviewer_id'].value_counts(dropna=False))

print("\nReviewer label counts:")
print(df_rev['reviewer_label'].value_counts(dropna=False))

print("\nCross-tab reviewer_id vs reviewer_label:")
print(pd.crosstab(df_rev['reviewer_id'], df_rev['reviewer_label'], dropna=False))

print("\nMissing labels count:", df_rev['reviewer_label'].isna().sum() + (df_rev['reviewer_label'].astype(str).str.strip() == '').sum())
print("Missing reasons count:", df_rev['reviewer_reason'].isna().sum() + (df_rev['reviewer_reason'].astype(str).str.strip() == '').sum())
print("Invalid labels count:", (~df_rev['reviewer_label'].isin(['HIGH', 'MEDIUM', 'LOW'])).sum())
if (~df_rev['reviewer_label'].isin(['HIGH', 'MEDIUM', 'LOW'])).sum() > 0:
    print("Invalid label values:", df_rev[~df_rev['reviewer_label'].isin(['HIGH', 'MEDIUM', 'LOW'])]['reviewer_label'].tolist())

print("\nPair range per reviewer:")
for rev in df_rev['reviewer_id'].unique():
    sub = df_rev[df_rev['reviewer_id'] == rev]
    print(f"  {rev}: min pair_id = {sub['pair_id'].min()}, max pair_id = {sub['pair_id'].max()}, total = {len(sub)}")

print("\n--- 2. JOIN WITH UNLABELED_PAIRS.CSV ---")
df_unlabeled = pd.read_csv(unlabeled_path)
print("Unlabeled pairs shape:", df_unlabeled.shape)
print("Unlabeled columns:", df_unlabeled.columns.tolist())
print("Unlabeled unique pair_id:", df_unlabeled['pair_id'].nunique())

merged = pd.merge(df_rev, df_unlabeled, on='pair_id', how='left')
print("Merged shape:", merged.shape)
print("Merged missing resume_id:", merged['resume_id'].isna().sum())
print("Merged missing job_id:", merged['job_id'].isna().sum())
print("Merged missing resume_raw_text:", merged['resume_raw_text'].isna().sum() + (merged['resume_raw_text'].astype(str).str.strip() == '').sum())
print("Merged missing job_raw_text:", merged['job_raw_text'].isna().sum() + (merged['job_raw_text'].astype(str).str.strip() == '').sum())

print("\n--- 3. SOURCE DATA JOIN / RESUME & JOB ID DISTRIBUTION ---")
print("Unique resume_ids in 675 reviews:", merged['resume_id'].nunique())
print("Unique job_ids in 675 reviews:", merged['job_id'].nunique())

print("\nTop resume_ids by count in reviews:")
print(merged['resume_id'].value_counts().head(10))

print("\nTop job_ids by count in reviews:")
print(merged['job_id'].value_counts().head(10))

print("\nChecking raw/processed dataset files in data/raw or data/processed or anywhere else:")
for root, dirs, files in os.walk(base_path):
    for f in files:
        if f != 'annotation_reviews.csv' and f.endswith('.csv'):
            print("  Found file:", os.path.relpath(os.path.join(root, f), base_path))

