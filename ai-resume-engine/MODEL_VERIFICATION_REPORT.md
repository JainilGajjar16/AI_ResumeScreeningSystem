# Phase 4D Independent Model Verification Report

**System Name**: AI Resume Screening System  
**Workspace Module**: `ai-resume-engine/`  
**Verification Script**: `scratch/verify_phase4d.py` & `scratch/generate_verification_report.py`  
**Output Verification Data**: `models/verification_results.json`  
**Completion Date**: October 3, 2026  
**Verification Status**: **VERIFIED & PASSED** (100% Reproducible, Zero Data Leakage)

---

## 1. Executive Verification Summary

An independent verification audit of the trained **Random Forest Classifier** (`models/resume_screening_model.joblib`) was performed. 

### Core Audit Audit Findings:
1. **100% Metric Reproducibility**: Reloading the saved pipeline binary and evaluating on `test_features.csv` reproduced the exact reported metrics with **zero discrepancy** (**99.01% Accuracy**, **98.97% Macro F1**).
2. **Strict Feature Schema Verification**: All metadata identifiers (`pair_id`, `resume_id`, `job_id`), raw text columns (`resume_raw_text`, `job_raw_text`), and target labels (`label`, `label_encoded`) were confirmed to be strictly excluded from model input features. Model inputs consist strictly of the **23 numerical features** in exact trained order.
3. **Zero Candidate Resume Leakage**: Grouped partitioning by candidate `resume_id` was verified across Train (189 resumes), Validation (40 resumes), and Test (41 resumes) splits. Overlap across all split pairs is **exactly 0**.
4. **Zero Dataset Duplicates**: Audit confirmed 0 duplicate `pair_id` records and 0 duplicate `(resume_id, job_id)` pairs.
5. **Cause of High Metrics (>98-99%)**: Deep statistical analysis confirmed that the 23 engineered features (specifically TF-IDF cosine similarity, skill coverage ratio, and matching skill count) align strongly with the deterministic criteria used by human reviewers (`REV_A`, `REV_B`, `REV_C`). The features create well-separated decision boundaries across the 675 candidate-job pairs.

---

## 2. Independent Test Set Metric Recalculation

Recalculated directly from test target labels `y_test` and reloaded model predictions `test_preds`:

| Metric Name | Reported in Metadata | Recalculated Metric | Discrepancy | Verification Result |
| :--- | :---: | :---: | :---: | :---: |
| **Test Accuracy** | `0.9900990099` (99.01%) | `0.9900990099` (99.01%) | `0.00000000` | **MATCH** |
| **Test Macro F1** | `0.9897435897` (98.97%) | `0.9897435897` (98.97%) | `0.00000000` | **MATCH** |
| **Test Weighted F1** | `0.9901867167` (99.02%) | `0.9901867167` (99.02%) | `0.00000000` | **MATCH** |

### Detailed Per-Class Recalculation:

| Class Label | Precision | Recall | F1-Score | Support (Actual Count) |
| :--- | :---: | :---: | :---: | :---: |
| **LOW** | 1.0000 | 0.9831 | 0.9915 | 59 |
| **MEDIUM** | 0.9565 | 1.0000 | 0.9778 | 22 |
| **HIGH** | 1.0000 | 1.0000 | 1.0000 | 20 |
| **Macro Average** | **0.9855** | **0.9944** | **0.9897** | **101** |
| **Weighted Average** | **0.9905** | **0.9901** | **0.9902** | **101** |

### Recalculated Test Confusion Matrix:
```
                Predicted LOW   Predicted MEDIUM   Predicted HIGH
Actual LOW            58                1                 0
Actual MEDIUM          0               22                 0
Actual HIGH            0                0                20
```

---

## 3. Data Leakage & Candidate Grouping Audit

### A. Candidate Resume Leakage Check across Dataset Splits:
- **Train Resumes**: 189 unique candidates (`data/processed/train_features.csv`)
- **Val Resumes**: 40 unique candidates (`data/processed/val_features.csv`)
- **Test Resumes**: 41 unique candidates (`data/processed/test_features.csv`)
- **Train $\cap$ Val Overlap**: **0 candidates**
- **Train $\cap$ Test Overlap**: **0 candidates**
- **Val $\cap$ Test Overlap**: **0 candidates**

### B. Input Feature Exclusion Check:
- Verified input columns fed into `pipeline.predict(X_test)`:
  `['tfidf_cosine_similarity', 'resume_skill_count', 'job_skill_count', 'matching_skill_count', 'missing_skill_count', 'skill_jaccard_similarity', 'skill_coverage_ratio', 'resume_has_experience', 'resume_has_education', 'resume_has_projects', 'resume_has_skills', 'resume_has_certifications', 'job_has_experience', 'job_has_education', 'job_has_projects', 'job_has_skills', 'job_has_certifications', 'section_match_score', 'resume_char_count', 'job_char_count', 'resume_word_count', 'job_word_count', 'word_count_ratio']` (23 numeric columns).
- Excluded metadata/target columns verified absent from feature matrix:
  - `pair_id` (Identifier)
  - `resume_id` (Candidate Identifier)
  - `job_id` (Job Posting Identifier)
  - `label` (String Target: LOW, MEDIUM, HIGH)
  - `label_encoded` (Numeric Target: 0, 1, 2)
  - Raw resume text & Raw job text

---

## 4. Investigation of High Metric Causes (>98-99%)

Why are the validation (98.97%) and test (99.01%) metrics exceptionally high?

### 1. High Signal-to-Noise Ratio in Feature Engineering
The 23 engineered features capture direct numerical proxies for human annotation criteria:
- **`tfidf_cosine_similarity`**: Average value is **0.0468** for `LOW`, **0.0879** for `MEDIUM`, and **0.2392** for `HIGH`.
- **`skill_coverage_ratio`**: Average value is **0.238** for `LOW`, **0.584** for `MEDIUM`, and **0.812** for `HIGH`.
- **`matching_skill_count`**: Average value is **0.69** for `LOW`, **2.45** for `MEDIUM`, and **3.91** for `HIGH`.

### 2. Consistency of Human Annotations
Human reviewers (`REV_A`, `REV_B`, `REV_C`) assigned labels based on structured skill and experience matching rules (as documented in `reviewer_reason` strings). Because human review logic strongly relied on technical skill overlap and job-resume keyword similarity, tree-based models easily learn clean non-linear decision boundaries separating `LOW`, `MEDIUM`, and `HIGH`.

### 3. Lack of Noise / Edge Cases in College Dataset
The 675 candidate-job pairs represent structured synthetic/curated resumes and job postings with distinct skill profiles. Out-of-vocabulary terms or messy unstructured resumes are rare in this dataset.

---

## 5. System Scope & Limitations

> [!WARNING]  
> **Scope & Generalization Constraints**:
> 1. **Academic Dataset Scope**: Tested on 675 candidate-job pairs (270 resumes). Near-perfect accuracy reflects dataset consistency rather than real-world universal coverage.
> 2. **Lexicon Bound**: Skill extraction is governed by `skills_dictionary.json` (51 technical skills). Resumes with uncatalogued technologies or non-standard formatting may degrade performance without lexicon updates.
> 3. **Rule Engine Synergy**: The ML classifier is designed to complement—not replace—the Java Spring Boot rule-based ATS engine.

---

## 6. Verification Conclusion & Readiness

- **Model Integrity**: **CONFIRMED** (Valid scikit-learn Pipeline object, exact feature schema).
- **Data Leakage**: **NONE DETECTED** (Train-only TF-IDF vectorizer fit, train-only StandardScaler fit, candidate grouping enforced).
- **Metric Reproducibility**: **100% REPRODUCIBLE** (Zero discrepancy).
- **System Change Check**: Zero changes made to Spring Boot code, MySQL databases, controllers, or rule-based ATS services.

The ML screening model is **VERIFIED AND READY FOR API INTEGRATION (Phase 5)**.
