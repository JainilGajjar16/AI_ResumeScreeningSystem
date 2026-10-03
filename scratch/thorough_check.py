import pandas as pd
import numpy as np
import os
import random

base_path = r"C:\Users\jaini\Documents\AI-ResumeScreeningSystem\ai-resume-engine\data"
reviews_path = os.path.join(base_path, "annotations", "annotation_reviews.csv")
unlabeled_path = os.path.join(base_path, "annotations", "unlabeled_pairs.csv")

df_rev = pd.read_csv(reviews_path)
df_unlabeled = pd.read_csv(unlabeled_path)

merged = pd.merge(df_rev, df_unlabeled, on='pair_id', how='left')

print("=== DEEP VALIDATION CHECKS ===")
print("1. Text completeness:")
merged['resume_len'] = merged['resume_raw_text'].astype(str).str.strip().str.len()
merged['job_len'] = merged['job_raw_text'].astype(str).str.strip().str.len()

print("Min resume text len:", merged['resume_len'].min())
print("Max resume text len:", merged['resume_len'].max())
print("Avg resume text len:", merged['resume_len'].mean())
print("Min job text len:", merged['job_len'].min())
print("Max job text len:", merged['job_len'].max())
print("Avg job text len:", merged['job_len'].mean())

empty_resumes = (merged['resume_len'] == 0).sum()
empty_jobs = (merged['job_len'] == 0).sum()
print("Empty resume texts:", empty_resumes)
print("Empty job texts:", empty_jobs)

print("\n2. Candidate-Job Pair uniqueness check:")
pair_tuples = list(zip(merged['resume_id'], merged['job_id']))
print("Total candidate-job pairs:", len(pair_tuples))
print("Unique (resume_id, job_id) combinations:", len(set(pair_tuples)))

print("\n3. Grouped Train/Val/Test Split Simulation by Resume ID:")
# Pure python/pandas Grouped Split implementation
def grouped_split_by_resume(df, train_ratio=0.70, val_ratio=0.15, seed=42):
    random.seed(seed)
    unique_resumes = list(df['resume_id'].unique())
    random.shuffle(unique_resumes)
    
    n_resumes = len(unique_resumes)
    n_train = int(n_resumes * train_ratio)
    n_val = int(n_resumes * val_ratio)
    
    train_resumes = set(unique_resumes[:n_train])
    val_resumes = set(unique_resumes[n_train:n_train + n_val])
    test_resumes = set(unique_resumes[n_train + n_val:])
    
    df_train = df[df['resume_id'].isin(train_resumes)]
    df_val = df[df['resume_id'].isin(val_resumes)]
    df_test = df[df['resume_id'].isin(test_resumes)]
    
    return df_train, df_val, df_test, train_resumes, val_resumes, test_resumes

df_tr, df_va, df_te, tr_r, va_r, te_r = grouped_split_by_resume(merged)

print(f"Train set: {len(df_tr)} pairs ({len(df_tr)/len(merged):.1%}), {len(tr_r)} unique resumes")
print(f"Val set  : {len(df_va)} pairs ({len(df_va)/len(merged):.1%}), {len(va_r)} unique resumes")
print(f"Test set : {len(df_te)} pairs ({len(df_te)/len(merged):.1%}), {len(te_r)} unique resumes")

print("\nData Leakage Check:")
print("Resume overlap Train-Val :", len(tr_r.intersection(va_r)))
print("Resume overlap Train-Test:", len(tr_r.intersection(te_r)))
print("Resume overlap Val-Test  :", len(va_r.intersection(te_r)))

print("\nLabel Distribution across splits:")
print("Overall:\n", merged['reviewer_label'].value_counts(normalize=True).round(3))
print("Train  :\n", df_tr['reviewer_label'].value_counts(normalize=True).round(3))
print("Val    :\n", df_va['reviewer_label'].value_counts(normalize=True).round(3))
print("Test   :\n", df_te['reviewer_label'].value_counts(normalize=True).round(3))
