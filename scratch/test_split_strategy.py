import pandas as pd
import numpy as np
import os
import random

base_dir = r"C:\Users\jaini\Documents\AI-ResumeScreeningSystem\ai-resume-engine\data\annotations"
df_rev = pd.read_csv(os.path.join(base_dir, "annotation_reviews.csv"))
df_unlabeled = pd.read_csv(os.path.join(base_dir, "unlabeled_pairs.csv"))

merged = pd.merge(df_rev, df_unlabeled, on="pair_id", how="left")

print("Total merged rows:", len(merged))
print("Unique resume_ids:", merged['resume_id'].nunique())
print("Unique job_ids:", merged['job_id'].nunique())

# Let's test multiple seeds to find a split that balances class distribution as well as 70/15/15 ratio
best_seed = None
best_diff = 999

results = []

for seed in range(100):
    random.seed(seed)
    resumes = list(merged['resume_id'].unique())
    random.shuffle(resumes)
    
    n_resumes = len(resumes)
    n_train = int(n_resumes * 0.70)
    n_val = int(n_resumes * 0.15)
    
    train_res = set(resumes[:n_train])
    val_res = set(resumes[n_train:n_train+n_val])
    test_res = set(resumes[n_train+n_val:])
    
    df_tr = merged[merged['resume_id'].isin(train_res)]
    df_va = merged[merged['resume_id'].isin(val_res)]
    df_te = merged[merged['resume_id'].isin(test_res)]
    
    tr_p = len(df_tr) / len(merged)
    va_p = len(df_va) / len(merged)
    te_p = len(df_te) / len(merged)
    
    # Calculate label distribution difference from overall
    overall_dist = merged['reviewer_label'].value_counts(normalize=True)
    tr_dist = df_tr['reviewer_label'].value_counts(normalize=True)
    va_dist = df_va['reviewer_label'].value_counts(normalize=True)
    te_dist = df_te['reviewer_label'].value_counts(normalize=True)
    
    dist_diff = (
        abs(tr_dist.get('HIGH', 0) - overall_dist.get('HIGH', 0)) +
        abs(va_dist.get('HIGH', 0) - overall_dist.get('HIGH', 0)) +
        abs(te_dist.get('HIGH', 0) - overall_dist.get('HIGH', 0)) +
        abs(tr_p - 0.70) + abs(va_p - 0.15) + abs(te_p - 0.15)
    )
    
    results.append((dist_diff, seed, tr_p, va_p, te_p, len(df_tr), len(df_va), len(df_te)))

results.sort(key=lambda x: x[0])
print("\nTop 5 Seeds for Stratified Group Split by resume_id:")
for diff, seed, tr_p, va_p, te_p, n_tr, n_va, n_te in results[:5]:
    print(f"Seed {seed:2d}: Diff={diff:.4f} | Train={n_tr} ({tr_p:.1%}) | Val={n_va} ({va_p:.1%}) | Test={n_te} ({te_p:.1%})")

# Let's inspect the best seed details
best_seed = results[0][1]
print(f"\n--- Best Seed Details (Seed {best_seed}) ---")
random.seed(best_seed)
resumes = list(merged['resume_id'].unique())
random.shuffle(resumes)

n_train = int(len(resumes) * 0.70)
n_val = int(len(resumes) * 0.15)

train_res = set(resumes[:n_train])
val_res = set(resumes[n_train:n_train+n_val])
test_res = set(resumes[n_train+n_val:])

df_tr = merged[merged['resume_id'].isin(train_res)]
df_va = merged[merged['resume_id'].isin(val_res)]
df_te = merged[merged['resume_id'].isin(test_res)]

print("Overall label counts:\n", merged['reviewer_label'].value_counts())
print("\nTrain label counts (Seed", best_seed, "):\n", df_tr['reviewer_label'].value_counts())
print("\nVal label counts (Seed", best_seed, "):\n", df_va['reviewer_label'].value_counts())
print("\nTest label counts (Seed", best_seed, "):\n", df_te['reviewer_label'].value_counts())

print("\nOverlap checks:")
print("Train-Val resume overlap :", len(train_res.intersection(val_res)))
print("Train-Test resume overlap:", len(train_res.intersection(test_res)))
print("Val-Test resume overlap  :", len(val_res.intersection(test_res)))
