# Phase 4B Feature Extraction & Dataset Splitting Report

**System Name**: AI Resume Screening System  
**Workspace Module**: `ai-resume-engine/`  
**Entry Point Script**: `build_features.py` (and `src/features/build_features.py`)  
**Output Directory**: `data/processed/`  
**Model Directory**: `models/`  
**Completion Date**: October 2, 2026  
**Status**: **PASSED** (100% Validated & Verified)

---

## 1. Executive Summary

Phase 4B establishes the reproducible candidate-grouped dataset splits and feature engineering pipeline for the **AI Resume Screening System**.

Starting from the 675 human-reviewed candidate-job pairs in `annotation_reviews.csv`, the dataset was split into **Train (70.7%)**, **Validation (14.4%)**, and **Test (15.0%)** sets grouped by candidate `resume_id`. 

Strict data leakage prevention was enforced: the TF-IDF vectorizer model binary (`models/tfidf_vectorizer.joblib`) was fitted **exclusively on the training split**, and applied without re-fitting to the validation and test splits. Exactly **23 numerical features** were extracted across TF-IDF cosine similarity, technical skill overlap metrics, structural section flags, and text length ratios.

---

## 2. Reproducible Grouped Dataset Splitting

> [!IMPORTANT]  
> Candidate `resume_id`s were partitioned to ensure **zero resume leakage** across splits. The same candidate resume never appears in more than one split.

### Split Size & Candidate Resume Partitioning:

| Dataset Split | File Path | Pair Count | Percentage | Unique Candidate Resumes | Candidate Leakage |
| :--- | :--- | :---: | :---: | :---: | :---: |
| **Train Set** | `data/processed/train_split.csv` | 477 | 70.67% | 189 | **0** |
| **Validation Set** | `data/processed/val_split.csv` | 97 | 14.37% | 40 | **0** |
| **Test Set** | `data/processed/test_split.csv` | 101 | 14.96% | 41 | **0** |
| **Total** | | **675** | **100.00%** | **270** | **0** |

---

## 3. Class Distribution Across Splits

The three target classes (`HIGH`, `MEDIUM`, `LOW`) were preserved without modifying human labels.

| Split | LOW Count (%) | MEDIUM Count (%) | HIGH Count (%) | Total Pairs |
| :--- | :---: | :---: | :---: | :---: |
| **Train Set** | 276 (57.86%) | 104 (21.80%) | 97 (20.34%) | 477 |
| **Validation Set** | 47 (48.45%) | 29 (28.87%) | 21 (21.65%) | 97 |
| **Test Set** | 59 (58.42%) | 22 (21.78%) | 20 (19.80%) | 101 |
| **Overall Dataset** | **382 (56.59%)** | **155 (22.96%)** | **138 (20.44%)** | **675** |

---

## 4. Feature Extraction Schema (23 Features)

Every candidate-job pair was transformed into a 23-dimensional numerical feature vector:

### A. TF-IDF Text Features (Data Leakage Free)
1. `tfidf_cosine_similarity`: Cosine similarity between resume TF-IDF vector and job TF-IDF vector (fitted ONLY on train set).

### B. Skill Overlap Metrics (Preserved via `skills_dictionary.json`)
2. `resume_skill_count`: Total technical skills found in resume text.
3. `job_skill_count`: Total technical skills required in job posting.
4. `matching_skill_count`: Count of overlapping skills between resume and job.
5. `missing_skill_count`: Count of required job skills missing in resume.
6. `skill_jaccard_similarity`: Jaccard similarity index $\frac{|R \cap J|}{|R \cup J|}$.
7. `skill_coverage_ratio`: Job skill coverage ratio $\frac{|R \cap J|}{|J|}$.

### C. Structural Section Flags & Coverage
8–12. `resume_has_experience`, `resume_has_education`, `resume_has_projects`, `resume_has_skills`, `resume_has_certifications`
13–17. `job_has_experience`, `job_has_education`, `job_has_projects`, `job_has_skills`, `job_has_certifications`
18. `section_match_score`: Count of matching active structural sections.

### D. Explainable Text Metrics
19. `resume_char_count`: Character length of raw resume text.
20. `job_char_count`: Character length of raw job text.
21. `resume_word_count`: Word count of resume text.
22. `job_word_count`: Word count of job posting text.
23. `word_count_ratio`: $\frac{\text{resume\_word\_count}}{\text{job\_word\_count} + 1e-5}$.

---

## 5. Summary of Generated Files

| File Path | Description | Rows / Columns / Type |
| :--- | :--- | :--- |
| `data/processed/train_split.csv` | Raw text & metadata for Training split | 477 rows × 7 cols |
| `data/processed/val_split.csv` | Raw text & metadata for Validation split | 97 rows × 7 cols |
| `data/processed/test_split.csv` | Raw text & metadata for Test split | 101 rows × 7 cols |
| `data/processed/train_features.csv` | Extracted numerical feature matrix for Training | 477 rows × 28 cols |
| `data/processed/val_features.csv` | Extracted numerical feature matrix for Validation | 97 rows × 28 cols |
| `data/processed/test_features.csv` | Extracted numerical feature matrix for Test | 101 rows × 28 cols |
| `models/tfidf_vectorizer.joblib` | Saved scikit-learn TF-IDF vectorizer model binary | `TfidfVectorizer` object |

---

## 6. Execution Command & Verification Results

### Command to Run:
```bash
python ai-resume-engine/build_features.py
```

### Verification Output:
```
==========================================================================
    AI RESUME SCREENING SYSTEM - PHASE 4B FEATURE EXTRACTION & SPLITTING  
==========================================================================

--- 1. LOADING DATASETS ---
Loaded 675 reviews from annotation_reviews.csv
Loaded 1475 pairs from unlabeled_pairs.csv
Loaded 51 skills from skills_dictionary.json
Merged dataset shape: (675, 9)
Missing value report: {'pair_id': 0, 'reviewer_id': 0, 'reviewer_label': 0, 'reviewer_reason': 0, 'review_timestamp': 0, 'resume_id': 0, 'job_id': 0, 'resume_raw_text': 0, 'job_raw_text': 0}

--- 2. CREATING REPRODUCIBLE GROUPED SPLITS ---
Train split : 477 pairs (70.7%), 189 unique resumes
Val split   :  97 pairs (14.4%),  40 unique resumes
Test split  : 101 pairs (15.0%),  41 unique resumes
[SUCCESS] Raw split CSV files saved under data/processed/

--- 3. FEATURE EXTRACTION & NO-LEAKAGE TRANSFORM ---
[SUCCESS] Fitted TF-IDF vectorizer saved to models/tfidf_vectorizer.joblib
[SUCCESS] Feature CSV files saved under data/processed/

--- 4. FEATURE EXTRACTION SUMMARY ---
Total Features Extracted   : 23

Sample Feature Metrics (Train set average):
  - Avg TF-IDF Cosine Similarity : 0.0938
  - Avg Skill Jaccard Similarity : 0.2875
  - Avg Skill Coverage Ratio     : 0.4100
  - Avg Matching Skill Count     : 1.77
==========================================================================
RESULT: [PASS] PHASE 4B FEATURE EXTRACTION & SPLITTING COMPLETED!
==========================================================================
```
