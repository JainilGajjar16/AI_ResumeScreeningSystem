import os
import sys
import pandas as pd
import numpy as np

def calculate_fleiss_kappa(file_path):
    if not os.path.exists(file_path):
        print(f"Error: Reviews file not found at {file_path}")
        return

    df_reviews = pd.read_csv(file_path)

    if df_reviews.empty:
        print("==================================================")
        print("      INTER-RATER AGREEMENT REPORT (FLEISS' KAPPA)")
        print("==================================================")
        print(f"File Path                      : {file_path}")
        print("Total Submitted Reviews        : 0")
        print("Overlap Batch Reviewed Pairs   : 0")
        print("Status                         : PENDING HUMAN ANNOTATION")
        print("Note                           : Zero fake labels generated. Fleiss' Kappa will be calculated once human reviewers submit ratings.")
        print("==================================================")
        return

    # Pivot to pair_id x reviewer_id table
    pivot = df_reviews.pivot(index="pair_id", columns="reviewer_id", values="reviewer_label")

    # Filter pairs with at least 2 reviewer ratings
    multi_reviewed = pivot.dropna(thresh=2)

    if multi_reviewed.empty:
        print("==================================================")
        print("      INTER-RATER AGREEMENT REPORT (FLEISS' KAPPA)")
        print("==================================================")
        print(f"File Path                      : {file_path}")
        print(f"Total Submitted Reviews        : {len(df_reviews)}")
        print("Multi-Reviewed Overlap Pairs   : 0 (Need >= 2 reviewers per pair)")
        print("Status                         : AWAITING SECONDARY REVIEWER SUBMISSIONS")
        print("==================================================")
        return

    categories = ["HIGH", "MEDIUM", "LOW"]
    N = len(multi_reviewed)
    n = multi_reviewed.count(axis=1).min() # min number of reviewers per pair

    # Build matrix of ratings per category per pair
    matrix = np.zeros((N, len(categories)))

    for i, (_, row) in enumerate(multi_reviewed.iterrows()):
        for j, cat in enumerate(categories):
            matrix[i, j] = (row == cat).sum()

    n_rv = matrix.sum(axis=1)

    # Compute P_i
    P_i = (np.sum(matrix**2, axis=1) - n_rv) / (n_rv * (n_rv - 1))
    P_mean = np.mean(P_i)

    # Compute P_j
    P_j = np.sum(matrix, axis=0) / np.sum(n_rv)
    P_e = np.sum(P_j**2)

    if P_e == 1.0:
        kappa = 1.0
    else:
        kappa = (P_mean - P_e) / (1.0 - P_e)

    print("==================================================")
    print("      INTER-RATER AGREEMENT REPORT (FLEISS' KAPPA)")
    print("==================================================")
    print(f"File Path                      : {file_path}")
    print(f"Total Submitted Reviews        : {len(df_reviews)}")
    print(f"Evaluated Overlap Pairs (N)    : {N}")
    print(f"Mean Rating Agreement (P_bar)  : {P_mean:.4f}")
    print(f"Expected Chance Agreement (P_e): {P_e:.4f}")
    print(f"Fleiss' Kappa Score (k)        : {kappa:.4f}")
    print("--------------------------------------------------")

    if kappa >= 0.81:
        qual = "Almost Perfect Agreement"
    elif kappa >= 0.61:
        qual = "Substantial Agreement"
    elif kappa >= 0.41:
        qual = "Moderate Agreement"
    elif kappa >= 0.21:
        qual = "Fair Agreement"
    else:
        qual = "Slight / Poor Agreement"

    print(f"Qualitative Interpretation     : {qual}")
    print("==================================================")

if __name__ == "__main__":
    base_dir = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
    default_csv = os.path.join(base_dir, "data", "annotations", "annotation_reviews.csv")
    target_csv = sys.argv[1] if len(sys.argv) > 1 else default_csv
    calculate_fleiss_kappa(target_csv)
