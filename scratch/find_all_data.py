import os

ws_root = r"C:\Users\jaini\Documents\AI-ResumeScreeningSystem"
data_files = []

for root, dirs, files in os.walk(ws_root):
    if ".git" in root or "target" in root or "node_modules" in root or ".venv" in root:
        continue
    for f in files:
        if f.endswith((".csv", ".json", ".parquet", ".tsv")):
            data_files.append(os.path.relpath(os.path.join(root, f), ws_root))

print("Data files in workspace:")
for f in sorted(data_files):
    print(" -", f)
