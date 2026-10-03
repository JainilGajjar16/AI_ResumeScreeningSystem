import os
import pandas as pd
import joblib

base_dir = r"C:\Users\jaini\Documents\AI-ResumeScreeningSystem\ai-resume-engine"
proc_dir = os.path.join(base_dir, "data", "processed")
model_dir = os.path.join(base_dir, "models")

print("=== VERIFYING GENERATED FILES ===")

files_to_check = [
    ("train_split.csv", proc_dir),
    ("val_split.csv", proc_dir),
    ("test_split.csv", proc_dir),
    ("train_features.csv", proc_dir),
    ("val_features.csv", proc_dir),
    ("test_features.csv", proc_dir),
    ("tfidf_vectorizer.joblib", model_dir)
]

for fname, fdir in files_to_check:
    fpath = os.path.join(fdir, fname)
    exists = os.path.exists(fpath)
    if exists:
        if fname.endswith(".csv"):
            df = pd.read_csv(fpath)
            print(f"[OK] {fname:25s} | Exists | Rows: {len(df):3d} | Cols: {len(df.columns):2d}")
        else:
            obj = joblib.load(fpath)
            print(f"[OK] {fname:25s} | Exists | Type: {type(obj).__name__}")
    else:
        print(f"[FAIL] {fname} missing at {fpath}")

# Check Resume Leakage across processed CSVs
tr_df = pd.read_csv(os.path.join(proc_dir, "train_features.csv"))
va_df = pd.read_csv(os.path.join(proc_dir, "val_features.csv"))
te_df = pd.read_csv(os.path.join(proc_dir, "test_features.csv"))

tr_res = set(tr_df["resume_id"])
va_res = set(va_df["resume_id"])
te_res = set(te_df["resume_id"])

print("\n--- RESUME LEAKAGE VERIFICATION ---")
print("Train-Val Resume Overlap :", len(tr_res.intersection(va_res)))
print("Train-Test Resume Overlap:", len(tr_res.intersection(te_res)))
print("Val-Test Resume Overlap  :", len(va_res.intersection(te_res)))

print("\n--- CLASS DISTRIBUTION VERIFICATION ---")
print("Train Label Counts:\n", tr_df["label"].value_counts())
print("\nVal Label Counts:\n", va_df["label"].value_counts())
print("\nTest Label Counts:\n", te_df["label"].value_counts())

print("\n--- SAMPLE FEATURE COLUMNS IN TRAIN_FEATURES.CSV ---")
print(tr_df.head(2).T)
