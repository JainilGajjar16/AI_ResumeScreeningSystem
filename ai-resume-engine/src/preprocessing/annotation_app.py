import os
import csv
import datetime
import pandas as pd
import streamlit as st

# Configure Streamlit Page
st.set_page_config(
    page_title="AI Resume Screening - Human Annotation Tool",
    page_icon="📝",
    layout="wide",
    initial_sidebar_state="expanded"
)

# File Paths
BASE_DIR = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
UNLABELED_CSV = os.path.join(BASE_DIR, "data", "annotations", "unlabeled_pairs.csv")
REVIEWS_CSV = os.path.join(BASE_DIR, "data", "annotations", "annotation_reviews.csv")
FINAL_CSV = os.path.join(BASE_DIR, "data", "annotations", "final_annotations.csv")

# Phase 3C Reviewer Assignments (225 pairs per reviewer, 675 unique pairs total)
REVIEWER_PROFILES = {
    "REV_A": {
        "name": "Jainil",
        "display": "REV_A (Jainil)",
        "ranges": [("PAIR_0001", "PAIR_0225")],
        "total_assigned": 225
    },
    "REV_B": {
        "name": "Kashish",
        "display": "REV_B (Kashish)",
        "ranges": [("PAIR_0226", "PAIR_0450")],
        "total_assigned": 225
    },
    "REV_C": {
        "name": "Khush",
        "display": "REV_C (Khush)",
        "ranges": [("PAIR_0451", "PAIR_0675")],
        "total_assigned": 225
    }
}

def extract_pair_num(pid):
    try:
        return int(str(pid).replace("PAIR_", ""))
    except Exception:
        return 0

def get_reviewer_assigned_pairs(df_pairs, reviewer_id):

    if reviewer_id not in REVIEWER_PROFILES:
        return df_pairs

    ranges = REVIEWER_PROFILES[reviewer_id]["ranges"]
    pair_nums = df_pairs["pair_id"].apply(extract_pair_num)
    mask = pd.Series(False, index=df_pairs.index)

    for start_id, end_id in ranges:
        start_num = extract_pair_num(start_id)
        end_num = extract_pair_num(end_id)
        mask = mask | ((pair_nums >= start_num) & (pair_nums <= end_num))

    return df_pairs[mask].reset_index(drop=True)

def ensure_files_exist():
    if not os.path.exists(REVIEWS_CSV):
        with open(REVIEWS_CSV, 'w', newline='', encoding='utf-8') as f:
            writer = csv.writer(f)
            writer.writerow(["pair_id", "reviewer_id", "reviewer_label", "reviewer_reason", "review_timestamp"])

    if not os.path.exists(FINAL_CSV) and os.path.exists(UNLABELED_CSV):
        df_unlabeled = pd.read_csv(UNLABELED_CSV)
        df_final = pd.DataFrame({
            "pair_id": df_unlabeled["pair_id"],
            "final_label": "",
            "final_reason": "",
            "review_status": "PENDING",
            "adjudicator_id": ""
        })
        df_final.to_csv(FINAL_CSV, index=False)

ensure_files_exist()

@st.cache_data(ttl=2)
def load_data():
    df_pairs = pd.read_csv(UNLABELED_CSV)
    df_reviews = pd.read_csv(REVIEWS_CSV) if os.path.exists(REVIEWS_CSV) else pd.DataFrame(columns=["pair_id", "reviewer_id", "reviewer_label", "reviewer_reason", "review_timestamp"])
    df_final = pd.read_csv(FINAL_CSV) if os.path.exists(FINAL_CSV) else pd.DataFrame(columns=["pair_id", "final_label", "final_reason", "review_status", "adjudicator_id"])
    return df_pairs, df_reviews, df_final

def save_review(pair_id, reviewer_id, label, reason):
    df_reviews = pd.read_csv(REVIEWS_CSV)
    now_str = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")

    # Duplicate check: Check if pair_id + reviewer_id exists
    mask = (df_reviews["pair_id"] == pair_id) & (df_reviews["reviewer_id"] == reviewer_id)

    if mask.any():
        df_reviews.loc[mask, "reviewer_label"] = label
        df_reviews.loc[mask, "reviewer_reason"] = reason
        df_reviews.loc[mask, "review_timestamp"] = now_str
        st.info(f"Updated existing review for {reviewer_id} on {pair_id}.")
    else:
        new_row = pd.DataFrame([{
            "pair_id": pair_id,
            "reviewer_id": reviewer_id,
            "reviewer_label": label,
            "reviewer_reason": reason,
            "review_timestamp": now_str
        }])
        df_reviews = pd.concat([df_reviews, new_row], ignore_index=True)
        st.success(f"Saved new review for {reviewer_id} on {pair_id}.")

    df_reviews.to_csv(REVIEWS_CSV, index=False)
    st.cache_data.clear()

def validate_assignments_internal(df_pairs):

    rev_a = get_reviewer_assigned_pairs(df_pairs, "REV_A")
    rev_b = get_reviewer_assigned_pairs(df_pairs, "REV_B")
    rev_c = get_reviewer_assigned_pairs(df_pairs, "REV_C")

    set_a = set(rev_a["pair_id"])
    set_b = set(rev_b["pair_id"])
    set_c = set(rev_c["pair_id"])

    coverage = set_a.union(set_b).union(set_c)

    return {
        "total_pairs": 675,
        "rev_a_count": len(set_a),
        "rev_b_count": len(set_b),
        "rev_c_count": len(set_c),
        "coverage_count": len(coverage)
    }

def main():
    st.title("📝 AI Resume Screening — Human Annotation & Adjudication Tool")

    df_pairs, df_reviews, df_final = load_data()
    total_pairs = len(df_pairs)

    # Sidebar Navigation & Reviewer Selection
    st.sidebar.header("👤 Annotator Configuration")
    app_mode = st.sidebar.radio("Select Tool Mode", ["Human Reviewer Interface", "Adjudication Admin Dashboard"])

    if app_mode == "Human Reviewer Interface":
        selected_display = st.sidebar.selectbox(
            "Select Assigned Reviewer",
            ["REV_A (Jainil)", "REV_B (Kashish)", "REV_C (Khush)"]
        )

        reviewer_id = selected_display.split(" ")[0]

        # Filter dataset to show ONLY current reviewer's assigned batch
        df_assigned = get_reviewer_assigned_pairs(df_pairs, reviewer_id)
        assigned_total = len(df_assigned)

        # Track reviewer change in session state
        if "current_reviewer" not in st.session_state or st.session_state.current_reviewer != reviewer_id:
            st.session_state.current_reviewer = reviewer_id
            st.session_state.pair_index = 0

        # Compute reviewer-specific stats
        rev_reviews = df_reviews[df_reviews["reviewer_id"] == reviewer_id]
        rev_done_ids = set(rev_reviews["pair_id"].unique())
        assigned_done_ids = set(df_assigned["pair_id"]).intersection(rev_done_ids)
        rev_count = len(assigned_done_ids)
        pending_count = assigned_total - rev_count
        progress_pct = (rev_count / assigned_total * 100) if assigned_total > 0 else 0.0

        st.sidebar.markdown("---")
        st.sidebar.subheader(f"📊 {reviewer_id} Progress Statistics")
        st.sidebar.markdown(f"**Assigned:** `{assigned_total}`")
        st.sidebar.markdown(f"**Reviewed:** `{rev_count}`")
        st.sidebar.markdown(f"**Pending:** `{pending_count}`")
        st.sidebar.markdown(f"**Progress:** `{rev_count}/{assigned_total}` (`{progress_pct:.1f}%`) ")
        st.sidebar.progress(progress_pct / 100.0)

        label_counts = rev_reviews["reviewer_label"].value_counts().to_dict()
        st.sidebar.markdown("**Label Breakdown (Your Entries):**")
        col_s1, col_s2, col_s3 = st.sidebar.columns(3)
        col_s1.metric("HIGH", label_counts.get("HIGH", 0))
        col_s2.metric("MED", label_counts.get("MEDIUM", 0))
        col_s3.metric("LOW", label_counts.get("LOW", 0))

        st.sidebar.markdown("---")

        # Resume First Pending Pair (Requirement 6: Searches ONLY within assigned set)
        if st.sidebar.button("📌 Resume First Pending Pair"):
            first_pending_idx = None
            for idx, p_id in enumerate(df_assigned["pair_id"]):
                if p_id not in rev_done_ids:
                    first_pending_idx = idx
                    break
            if first_pending_idx is not None:
                st.session_state.pair_index = first_pending_idx
                st.info(f"Resumed at first pending pair: {df_assigned.iloc[first_pending_idx]['pair_id']}")
            else:
                st.success(f"{reviewer_id} assignment completed.")
                st.session_state.pair_index = max(0, assigned_total - 1)
            st.rerun()

        # Pair Index Selector bounded by assigned batch
        curr_idx = st.session_state.pair_index
        if curr_idx >= assigned_total:
            curr_idx = max(0, assigned_total - 1)
            st.session_state.pair_index = curr_idx

        curr_idx = st.sidebar.number_input(
            f"Jump to Assigned Index (0-{assigned_total - 1})",
            min_value=0,
            max_value=max(0, assigned_total - 1),
            value=curr_idx
        )
        st.session_state.pair_index = curr_idx

        # Main Review Area
        pair = df_assigned.iloc[curr_idx]
        p_id = pair["pair_id"]
        r_id = pair["resume_id"]
        j_id = pair["job_id"]

        st.markdown(f"### Candidate-Job Pair: **{p_id}** (Assigned Item `{curr_idx + 1}` of `{assigned_total}`)")

        if rev_count == assigned_total:
            st.success(f"🎉 **{reviewer_id} assignment completed.** All {assigned_total} assigned pairs have been reviewed!")

        # Display Qualitative Guidance Card
        with st.expander("📖 Qualitative Annotation Guidelines (Click to Expand)", expanded=False):
            st.markdown("""
            * **HIGH MATCH**: Candidate shows strong overall suitability for the specific job description (core skills, experience quality, relevant projects, education).
            * **MEDIUM MATCH**: Meaningful overall alignment but noticeable gaps (e.g. missing secondary framework, minor experience deficit offset by relevant projects).
            * **LOW MATCH**: Substantial mismatch (missing core technical language, unrelated domain, major deficit).
            * *Rule*: Ratings MUST be qualitative human assessments. Do NOT use fixed skill percentages or hardcoded formulas.
            """)

        # Display Side-by-Side Resume vs Job Description
        col_resume, col_job = st.columns(2)

        with col_resume:
            st.subheader(f"📄 Candidate Resume (`{r_id}`)")
            st.info(pair["resume_raw_text"])

        with col_job:
            st.subheader(f"💼 Job Description (`{j_id}`)")
            st.success(pair["job_raw_text"])

        st.markdown("---")

        # PRIVACY (Requirement 8): Look ONLY at current reviewer's own prior review
        existing_row = rev_reviews[rev_reviews["pair_id"] == p_id]
        default_label = None
        default_reason = ""
        if not existing_row.empty:
            default_label = existing_row.iloc[0]["reviewer_label"]
            default_reason = existing_row.iloc[0]["reviewer_reason"]
            st.warning(f"⚠️ Existing review recorded by **{reviewer_id}** on {existing_row.iloc[0]['review_timestamp']}: Label = **{default_label}**")

        # Reviewer Input Form (Requirement 1 & 11: Human must manually select label)
        st.subheader("✍️ Reviewer Assessment Input")

        col_lbl, col_btn = st.columns([1, 2])
        with col_lbl:
            label_choice = st.radio(
                "Match Rating (Required - Select manually)",
                options=["HIGH", "MEDIUM", "LOW"],
                index=["HIGH", "MEDIUM", "LOW"].index(default_label) if default_label in ["HIGH", "MEDIUM", "LOW"] else None,
                key=f"label_{p_id}_{reviewer_id}"
            )

        reason_input = st.text_area(
            "Reviewer Rationale (Required - Brief 1-2 sentence justification)",
            value=default_reason,
            placeholder="Example: Candidate has strong Java and Spring Boot experience, but lacks requested AWS deployment background.",
            key=f"reason_{p_id}_{reviewer_id}"
        )

        # Navigation & Submit Buttons
        col_b1, col_b2, col_b3, col_b4 = st.columns(4)

        with col_b1:
            if st.button("⬅️ Previous Pair", disabled=(curr_idx == 0)):
                st.session_state.pair_index = max(0, curr_idx - 1)
                st.rerun()

        with col_b2:
            if st.button("💾 Save & Next Pair", type="primary"):
                if label_choice is None:
                    st.error("❌ Match Rating is required! Please select HIGH, MEDIUM, or LOW.")
                elif not reason_input.strip():
                    st.error("❌ Rationale is required before saving! Please enter a brief justification.")
                else:
                    save_review(p_id, reviewer_id, label_choice, reason_input.strip())
                    rev_done_ids.add(p_id)

                    # Automatic Next-Pair Navigation (Requirement 5)
                    next_unreviewed_idx = None
                    for idx in range(curr_idx + 1, assigned_total):
                        if df_assigned.iloc[idx]["pair_id"] not in rev_done_ids:
                            next_unreviewed_idx = idx
                            break
                    if next_unreviewed_idx is None:
                        for idx in range(0, curr_idx):
                            if df_assigned.iloc[idx]["pair_id"] not in rev_done_ids:
                                next_unreviewed_idx = idx
                                break

                    if next_unreviewed_idx is not None:
                        st.session_state.pair_index = next_unreviewed_idx
                    else:
                        st.session_state.pair_index = min(curr_idx + 1, assigned_total - 1)
                        st.success(f"{reviewer_id} assignment completed.")
                    st.rerun()

        with col_b3:
            if st.button("⏭️ Skip Pair"):
                st.session_state.pair_index = min(assigned_total - 1, curr_idx + 1)
                st.rerun()

        with col_b4:
            if st.button("➡️ Next Pair", disabled=(curr_idx == assigned_total - 1)):
                st.session_state.pair_index = min(assigned_total - 1, curr_idx + 1)
                st.rerun()

    else:
        # Adjudication Admin Dashboard
        st.subheader("⚖️ Adjudication Admin Dashboard")

        # Sidebar Validation Check Tool
        if st.sidebar.button("🔍 Run Assignment Validation Check"):
            metrics = validate_assignments_internal(df_pairs)
            st.sidebar.success(f"""
            **Validation Results:**
            * Target Unique Pairs: {metrics['total_pairs']} (Expected 675)
            * REV_A (Jainil): {metrics['rev_a_count']} assigned
            * REV_B (Kashish): {metrics['rev_b_count']} assigned
            * REV_C (Khush): {metrics['rev_c_count']} assigned
            * Total Unique Coverage: {metrics['coverage_count']} (Expected 675)
            """)

        adj_id = st.text_input("Adjudicator ID", value="ADJ_LEAD")

        # Group reviews by pair_id
        pair_review_counts = df_reviews.groupby("pair_id")["reviewer_id"].nunique().to_dict()
        reviewed_pair_ids = [p for p, c in pair_review_counts.items() if c >= 1]

        st.info(f"Total Candidate-Job Pairs with Submitted Reviews: **{len(reviewed_pair_ids)}**")

        if not reviewed_pair_ids:
            st.warning("No human reviews submitted yet. Adjudication dashboard will display pairs once reviewers submit ratings.")
            return

        selected_adj_pair = st.selectbox("Select Pair for Adjudication", reviewed_pair_ids)
        pair_reviews = df_reviews[df_reviews["pair_id"] == selected_adj_pair]

        st.markdown(f"#### Submitted Reviews for Pair: **{selected_adj_pair}**")
        st.dataframe(pair_reviews[["reviewer_id", "reviewer_label", "reviewer_reason", "review_timestamp"]], use_container_width=True)

        distinct_labels = pair_reviews["reviewer_label"].unique()
        if len(distinct_labels) > 1:
            st.error(f"⚠️ **Split Decision Detected**: Reviewers assigned different labels ({', '.join(distinct_labels)}). Manual Adjudication Required!")
        else:
            st.success(f"✅ **Unanimous Agreement**: All reviewers assigned **{distinct_labels[0]}**.")

        # Display Resume & Job Content
        pair_meta = df_pairs[df_pairs["pair_id"] == selected_adj_pair].iloc[0]
        with st.expander("Show Pair Resume & Job Content"):
            st.markdown(f"**Resume ({pair_meta['resume_id']})**: {pair_meta['resume_raw_text']}")
            st.markdown(f"**Job ({pair_meta['job_id']})**: {pair_meta['job_raw_text']}")

        # Adjudication Input
        col_a1, col_a2 = st.columns(2)
        with col_a1:
            final_lbl_choice = st.selectbox("Binding Final Label", ["HIGH", "MEDIUM", "LOW"], index=0)
        with col_a2:
            status_choice = st.selectbox("Status", ["APPROVED", "ADJUDICATED"])

        final_reason_input = st.text_area("Adjudicator Rationale / Consensus Notes", value=f"Adjudicated based on reviewer consensus: {', '.join(distinct_labels)}")

        if st.button("💾 Save Final Adjudication Entry", type="primary"):
            mask_f = df_final["pair_id"] == selected_adj_pair
            if mask_f.any():
                df_final.loc[mask_f, "final_label"] = final_lbl_choice
                df_final.loc[mask_f, "final_reason"] = final_reason_input.strip()
                df_final.loc[mask_f, "review_status"] = status_choice
                df_final.loc[mask_f, "adjudicator_id"] = adj_id
            else:
                new_f = pd.DataFrame([{
                    "pair_id": selected_adj_pair,
                    "final_label": final_lbl_choice,
                    "final_reason": final_reason_input.strip(),
                    "review_status": status_choice,
                    "adjudicator_id": adj_id
                }])
                df_final = pd.concat([df_final, new_f], ignore_index=True)

            df_final.to_csv(FINAL_CSV, index=False)
            st.cache_data.clear()
            st.success(f"Final adjudication saved for {selected_adj_pair} as {final_lbl_choice} ({status_choice}).")

if __name__ == "__main__":
    main()
