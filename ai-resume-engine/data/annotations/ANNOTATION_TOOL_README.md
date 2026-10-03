# Human Resume-Job Match Annotation & Adjudication Tool Guide

This directory contains the multi-reviewer annotation application and adjudication infrastructure for the **AI Resume Screening System**.

---

## 1. Storage Architecture

```
ai-resume-engine/data/annotations/
├── unlabeled_pairs.csv    # Original 1,475 candidate-job pairs (READ-ONLY)
├── annotation_reviews.csv # Multi-reviewer submissions (pair_id, reviewer_id, label, reason, timestamp)
├── final_annotations.csv  # Final consensus & adjudication targets (pair_id, final_label, final_reason, review_status, adjudicator_id)
├── ANNOTATION_GUIDELINES.md # Qualitative rating definitions
└── REVIEWER_PARTITIONING_PLAN.md # Batch distribution & overlap plan
```

---

## 2. Installation & Setup

### Requirements
- Python 3.9+
- Packages: `pandas`, `streamlit`

```bash
pip install pandas streamlit
```

---

## 3. Starting the Annotation Tool

Run the Streamlit application from the project root:

```bash
streamlit run ai-resume-engine/src/preprocessing/annotation_app.py
```

The web dashboard will launch locally at `http://localhost:8501`.

---

## 4. Reviewer Workflow

1. **Select Reviewer ID**: Choose `REV_A`, `REV_B`, `REV_C`, or enter a custom ID in the left sidebar.
2. **Review Candidate vs Job**: Examine candidate resume details on the left and job description details on the right.
3. **Select Match Rating**: Click `HIGH`, `MEDIUM`, or `LOW`.
4. **Enter Rationale**: Write a brief 1-2 sentence technical justification in the mandatory text box.
5. **Save & Next**: Click `💾 Save & Next Pair`. The entry will be recorded in `data/annotations/annotation_reviews.csv`.

---

## 5. Duplicate Entry Protection & Resuming

- **Duplicate Protection**: If `pair_id + reviewer_id` already exists in `annotation_reviews.csv`, saving will **update** the existing review timestamp and rationale rather than creating a duplicate row.
- **Resuming Progress**: Click `📌 Resume First Pending Pair` in the sidebar to jump immediately to the first pair unreviewed by your reviewer ID.

---

## 6. Adjudication Workflow

1. In the sidebar, select **Adjudication Admin Dashboard**.
2. Select a pair with submitted reviews.
3. The dashboard compares individual reviewer ratings (e.g. `REV_A: HIGH`, `REV_B: MEDIUM`, `REV_C: HIGH`).
4. If ratings differ, the pair is flagged with **Split Decision Detected**.
5. The adjudicator enters `final_label`, `final_reason`, and `adjudicator_id`, saving the consensus record to `final_annotations.csv`.

---

## 7. Inter-Rater Agreement Calculation

To calculate **Fleiss' Kappa** on the multi-reviewer overlap batch (`PAIR_0001` - `PAIR_0200`):

```bash
python ai-resume-engine/src/preprocessing/calculate_kappa.py
```
Or via Java:
```bash
java scratch/CalculateKappaRunner.java
```

> [!NOTE]
> Fleiss' Kappa evaluates human reviewer consistency. If no human reviews have been submitted yet, the script safely reports zero counts without generating fake labels.
