import os
import sys
import pandas as pd

BASE_DIR = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
UNLABELED_CSV = os.path.join(BASE_DIR, "data", "annotations", "unlabeled_pairs.csv")
REVIEWS_CSV = os.path.join(BASE_DIR, "data", "annotations", "annotation_reviews.csv")

REVIEWER_ASSIGNMENTS = {
    "REV_A": [("PAIR_0001", "PAIR_0225")],
    "REV_B": [("PAIR_0226", "PAIR_0450")],
    "REV_C": [("PAIR_0451", "PAIR_0675")]
}

def extract_pair_num(pid):
    try:
        return int(str(pid).replace("PAIR_", ""))
    except Exception:
        return 0

def get_reviewer_pairs(df_pairs, reviewer_id):
    if reviewer_id not in REVIEWER_ASSIGNMENTS:
        return pd.DataFrame()
    ranges = REVIEWER_ASSIGNMENTS[reviewer_id]
    pair_nums = df_pairs["pair_id"].apply(extract_pair_num)
    mask = pd.Series(False, index=df_pairs.index)
    for start_id, end_id in ranges:
        s_num = extract_pair_num(start_id)
        e_num = extract_pair_num(end_id)
        mask = mask | ((pair_nums >= s_num) & (pair_nums <= e_num))
    return df_pairs[mask]

def run_validation():
    if not os.path.exists(UNLABELED_CSV):
        print(f"Error: {UNLABELED_CSV} not found.")
        sys.exit(1)

    df_pairs = pd.read_csv(UNLABELED_CSV)

    rev_a_df = get_reviewer_pairs(df_pairs, "REV_A")
    rev_b_df = get_reviewer_pairs(df_pairs, "REV_B")
    rev_c_df = get_reviewer_pairs(df_pairs, "REV_C")

    rev_a_pairs = set(rev_a_df["pair_id"])
    rev_b_pairs = set(rev_b_df["pair_id"])
    rev_c_pairs = set(rev_c_df["pair_id"])

    all_covered_pairs = rev_a_pairs.union(rev_b_pairs).union(rev_c_pairs)
    total_unique_target = len(all_covered_pairs)

    # Check actual reviews from annotation_reviews.csv
    rev_a_completed = 0
    rev_b_completed = 0
    rev_c_completed = 0
    if os.path.exists(REVIEWS_CSV):
        df_reviews = pd.read_csv(REVIEWS_CSV)
        if not df_reviews.empty:
            rev_a_reviews = df_reviews[df_reviews["reviewer_id"] == "REV_A"]
            rev_b_reviews = df_reviews[df_reviews["reviewer_id"] == "REV_B"]
            rev_c_reviews = df_reviews[df_reviews["reviewer_id"] == "REV_C"]
            rev_a_completed = len(set(rev_a_reviews["pair_id"]).intersection(rev_a_pairs))
            rev_b_completed = len(set(rev_b_reviews["pair_id"]).intersection(rev_b_pairs))
            rev_c_completed = len(set(rev_c_reviews["pair_id"]).intersection(rev_c_pairs))

    print("==================================================")
    print("      REVIEWER BATCH ASSIGNMENT VALIDATION        ")
    print("==================================================")
    print(f"Total Unique Target Pairs      : {total_unique_target} (Expected: 675)")
    print(f"REV_A (Jainil) Assigned Count  : {len(rev_a_pairs)} (Expected: 225)")
    print(f"REV_A (Jainil) Completed Count : {rev_a_completed} (Expected: 225 - COMPLETE)")
    print(f"REV_B (Kashish) Assigned Count : {len(rev_b_pairs)} (Expected: 225)")
    print(f"REV_B (Kashish) Completed Count: {rev_b_completed} (Expected: 0 - PENDING)")
    print(f"REV_C (Khush) Assigned Count   : {len(rev_c_pairs)} (Expected: 225)")
    print(f"REV_C (Khush) Completed Count  : {rev_c_completed} (Expected: 0 - PENDING)")
    print(f"Unique Target Pairs Covered    : {len(all_covered_pairs)} (Expected: 675)")
    print("--------------------------------------------------")

    valid = (
        total_unique_target == 675 and
        len(rev_a_pairs) == 225 and
        len(rev_b_pairs) == 225 and
        len(rev_c_pairs) == 225 and
        rev_a_completed == 225 and
        rev_b_completed == 0 and
        rev_c_completed == 0 and
        len(all_covered_pairs) == 675
    )

    if valid:
        print("RESULT: VALIDATION PASSED - ALL ASSIGNMENT CONSTRAINTS MET!")
    else:
        print("RESULT: VALIDATION FAILED!")
        sys.exit(1)

    print("==================================================")

if __name__ == "__main__":
    run_validation()

