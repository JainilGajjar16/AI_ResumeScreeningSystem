import os
import pandas as pd

base_dir = r"C:\Users\jaini\Documents\AI-ResumeScreeningSystem\ai-resume-engine\data"
print("Scanning:", base_dir)

for root, dirs, files in os.walk(base_dir):
    for f in files:
        if f.endswith(".csv"):
            path = os.path.join(root, f)
            try:
                df = pd.read_csv(path)
                rel_path = os.path.relpath(path, base_dir)
                print(f"\n=== File: {rel_path} ===")
                print(f"Shape: {df.shape}")
                print(f"Columns: {list(df.columns)}")
                print("Head:")
                print(df.head(2))
            except Exception as e:
                print(f"Error reading {f}: {e}")
