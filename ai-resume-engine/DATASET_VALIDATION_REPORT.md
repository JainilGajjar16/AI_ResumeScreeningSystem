# Phase 4A Dataset Validation Report: Human-Annotated Resume Screening Dataset

**System Name**: AI Resume Screening System  
**Workspace Module**: `ai-resume-engine/`  
**Dataset File**: `data/annotations/annotation_reviews.csv`  
**Backup Location**: `data/annotations/annotation_reviews.csv.bak`  
**Validation Date**: October 2, 2026  
**Status**: **PASSED** (100% Validated & Verified)

---

## 1. Executive Summary

This validation report presents the empirical verification of the human-annotated dataset (`annotation_reviews.csv`) for the **AI Resume Screening System** prior to model training. 

The dataset contains exactly **675 human-reviewed resume-job pairs** annotated by 3 reviewers (**REV_A - Jainil**, **REV_B - Kashish**, and **REV_C - Khush**), each reviewing 225 assigned pairs.

All requirements for Phase 4A have been fully checked and verified. The dataset is uncorrupted, contains zero missing labels or reasons, joins 100% cleanly with the source dataset, and is ready for Phase 4B feature extraction and model training.

---

## 2. File & Backup Verification

> [!IMPORTANT]  
> The original dataset file `ai-resume-engine/data/annotations/annotation_reviews.csv` was **not modified or deleted**. A bit-for-bit backup copy was created prior to inspection.

| Item | Path | Status |
| :--- | :--- | :--- |
| **Original Dataset** | `ai-resume-engine/data/annotations/annotation_reviews.csv` | **Intact (Unmodified)** |
| **Backup Dataset** | `ai-resume-engine/data/annotations/annotation_reviews.csv.bak` | **Created & Verified** |

---

## 3. Dataset Structure & Partition Verification

The target dataset of **675 pairs** is divided into 3 equal reviewer partitions of **225 pairs** each.

| Reviewer ID | Reviewer Name | Assigned Range | Expected Count | Actual Count | Status |
| :--- | :--- | :--- | :---: | :---: | :---: |
| **REV_A** | Jainil Gajjar | `PAIR_0001` - `PAIR_0225` | 225 | 225 | **PASSED** |
| **REV_B** | Kashish | `PAIR_0226` - `PAIR_0450` | 225 | 225 | **PASSED** |
| **REV_C** | Khush | `PAIR_0451` - `PAIR_0675` | 225 | 225 | **PASSED** |
| **Total** | | `PAIR_0001` - `PAIR_0675` | **675** | **675** | **PASSED** |

### Duplicate & Integrity Checks:
- **Total Rows**: 675 (Expected: 675) — **PASSED**
- **Unique `pair_id` Count**: 675 (Expected: 675) — **PASSED**
- **Duplicate Records**: 0 — **PASSED**

---

## 4. Label & Rationale Quality Inspection

All assigned labels were inspected for validity against the schema values: `HIGH`, `MEDIUM`, or `LOW`.

### Label Distribution Overview:

| Label | Count | Percentage |
| :--- | :---: | :---: |
| **LOW** | 382 | 56.59% |
| **MEDIUM** | 155 | 22.96% |
| **HIGH** | 138 | 20.44% |
| **Total** | **675** | **100.00%** |

### Breakdown by Reviewer:

| Reviewer ID | HIGH | MEDIUM | LOW | Total Reviews |
| :--- | :---: | :---: | :---: | :---: |
| **REV_A (Jainil)** | 46 (20.44%) | 69 (30.67%) | 110 (48.89%) | 225 |
| **REV_B (Kashish)** | 46 (20.44%) | 37 (16.44%) | 142 (63.11%) | 225 |
| **REV_C (Khush)** | 46 (20.44%) | 49 (21.78%) | 130 (57.78%) | 225 |

### Quality Audit Results:
- **Invalid Labels**: 0 (0.0%) — **PASSED**
- **Missing Labels**: 0 (0.0%) — **PASSED**
- **Missing Reviewer Reasons**: 0 (0.0%) — **PASSED**

---

## 5. Source Data Join & Text Completeness

The dataset in `annotation_reviews.csv` was joined with `unlabeled_pairs.csv` on `pair_id` to verify references to source resume text, job description text, `resume_id`, and `job_id`.

| Field | Check | Result | Status |
| :--- | :--- | :--- | :---: |
| **Join Match Rate** | `annotation_reviews.csv` ⟗ `unlabeled_pairs.csv` | 675 / 675 pairs matched (100%) | **PASSED** |
| **`resume_id` Integrity** | Null/Missing Count | 0 missing | **PASSED** |
| **`job_id` Integrity** | Null/Missing Count | 0 missing | **PASSED** |
| **`resume_raw_text`** | Empty string count | 0 empty (148-182 chars, avg: 165.8) | **PASSED** |
| **`job_raw_text`** | Empty string count | 0 empty (115-148 chars, avg: 128.7) | **PASSED** |
| **Candidate-Job Duplicates** | Duplicate `(resume_id, job_id)` pairs | 0 duplicates | **PASSED** |

---

## 6. Data Leakage Analysis & Grouped Split Strategy

### Data Leakage Risk Identification:
- The 675 reviewed pairs contain **270 unique candidate resumes** and **112 unique job descriptions**.
- On average, each candidate resume appears in **2.5 candidate-job pairs**.
- **Risk**: If standard random splitting is used, pairs containing the same candidate resume (e.g. `RES_SOF_0001`) would be split across training and test sets. The ML model would memorize candidate-specific text patterns instead of learning generalized match criteria.

### Recommended Split Strategy:
- **Grouped Stratified Split by Candidate Resume ID (`resume_id`)**:
  - All pairs belonging to a given `resume_id` are strictly assigned to either Train, Validation, or Test.
  - Zero `resume_id` overlap between splits.

### Simulated Split Results (70% Train / 15% Val / 15% Test):

| Split | Candidate-Job Pairs | Unique Resumes | Resume Leakage |
| :--- | :---: | :---: | :---: |
| **Train Set** | 476 pairs (70.5%) | 189 resumes | **0** |
| **Validation Set** | 96 pairs (14.2%) | 40 resumes | **0** |
| **Test Set** | 103 pairs (15.3%) | 41 resumes | **0** |
| **Total** | **675 pairs (100%)** | **270 resumes** | **0** |

---

## 7. Requirement Checklist & Verification Summary

| # | Requirement | Status | Verification Note |
| :---: | :--- | :---: | :--- |
| 1 | Do not modify/delete original CSV | **PASSED** | File untouched |
| 2 | Create backup before changes | **PASSED** | Created `annotation_reviews.csv.bak` |
| 3 | Verify row count, unique pairs, reviewers, label stats, missing values | **PASSED** | 675 rows, 3 reviewers x 225 pairs, 0 missing |
| 4 | Check resume/job text references | **PASSED** | 100% present, non-empty |
| 5 | Verify labels are valid (HIGH/MEDIUM/LOW) | **PASSED** | 100% valid |
| 6 | Check join with source datasets | **PASSED** | 675/675 pairs joined cleanly |
| 7 | Identify data leakage & plan grouped split | **PASSED** | Grouped by `resume_id`, 0 leakage |
| 8 | Do not create fake labels / synthetic records | **PASSED** | 0 fake/synthetic records created |
| 9 | Do not train a model yet | **PASSED** | 0 models trained |
| 10 | Do not change Spring Boot / ATS functionality | **PASSED** | System untouched |

---

## 8. Next Steps (Simple Instructions)

1. **Proceed to Phase 4B (Feature Extraction & Dataset Splitting)**:
   - Create the grouped train/val/test splitting module (`src/features/build_features.py`).
   - Extract numerical features (TF-IDF cosine similarity, skill Jaccard overlap, section coverage).
2. **Train Models in Phase 4C**:
   - Train Baseline Logistic Regression, Random Forest, and XGBoost classifiers using the grouped training split.
