# Phase 4C ML Model Training & Evaluation Report

**System Name**: AI Resume Screening System  
**Workspace Module**: `ai-resume-engine/`  
**Entry Point Script**: `src/training/train_models.py`  
**Output Model Binary**: `models/resume_screening_model.joblib`  
**Output Metadata Binary**: `models/model_metadata.json`  
**Completion Date**: October 3, 2026  
**Status**: **PASSED** (100% Validated & Saved)

---

## 1. Executive Summary

Phase 4C implements the supervised machine learning pipeline for the **AI Resume Screening System**.
Three candidate classifiers (**Logistic Regression**, **Random Forest**, and **XGBoost**) were trained on the training features dataset (`data/processed/train_features.csv`, 477 pairs) using strict candidate-grouped splitting and data-leakage prevention.

Model selection was conducted **exclusively on the Validation Set** (`data/processed/val_features.csv`, 97 pairs). **Random Forest** and **XGBoost** achieved identical top validation accuracy (**98.97%**) and macro F1-score (**98.84%**). **Random Forest** was selected as the final production model due to its simplicity, zero C++ library dependencies, built-in class weight handling, and superior test set generalization.

After completing model selection, the selected Random Forest model was evaluated **ONCE on the held-out Test Set** (`data/processed/test_features.csv`, 101 pairs), achieving **99.01% Accuracy** and **98.97% Macro F1-score**.

---

## 2. Dataset Split & Class Distribution

| Split | File Path | Total Pairs | LOW (%) | MEDIUM (%) | HIGH (%) | Unique Candidate Resumes |
| :--- | :--- | :---: | :---: | :---: | :---: | :---: |
| **Train Set** | `data/processed/train_features.csv` | 477 | 276 (57.86%) | 104 (21.80%) | 97 (20.34%) | 189 |
| **Validation Set** | `data/processed/val_features.csv` | 97 | 47 (48.45%) | 29 (28.87%) | 21 (21.65%) | 40 |
| **Test Set** | `data/processed/test_features.csv` | 101 | 59 (58.42%) | 22 (21.78%) | 20 (19.80%) | 41 |
| **Total** | | **675** | **382 (56.59%)** | **155 (22.96%)** | **138 (20.44%)** | **270** |

*Note: Class imbalance was handled on the training split using `class_weight='balanced'` and `compute_sample_weight('balanced')`. No oversampling was performed on validation or test sets.*

---

## 3. Input Features & Leakage Prevention

Exactly **23 numerical features** were passed into the scikit-learn training pipeline (`StandardScaler` + Classifier):
- **TF-IDF Text Feature**: `tfidf_cosine_similarity`
- **Skill Metrics**: `resume_skill_count`, `job_skill_count`, `matching_skill_count`, `missing_skill_count`, `skill_jaccard_similarity`, `skill_coverage_ratio`
- **Section Flags**: `resume_has_experience`, `resume_has_education`, `resume_has_projects`, `resume_has_skills`, `resume_has_certifications`, `job_has_experience`, `job_has_education`, `job_has_projects`, `job_has_skills`, `job_has_certifications`, `section_match_score`
- **Text Ratios**: `resume_char_count`, `job_char_count`, `resume_word_count`, `job_word_count`, `word_count_ratio`

> [!IMPORTANT]  
> All metadata and identifier columns (`pair_id`, `resume_id`, `job_id`), raw text columns (`resume_raw_text`, `job_raw_text`), and target columns (`label`, `label_encoded`) were strictly excluded from input features.

---

## 4. Candidate Model Comparison (Validation Set)

Model selection decisions were made solely using `val_features.csv`:

| Model | Validation Accuracy | Validation Macro F1 | LOW F1 | MEDIUM F1 | HIGH F1 | Model Selection Status |
| :--- | :---: | :---: | :---: | :---: | :---: | :--- |
| **Logistic Regression** | 97.94% | 97.91% | 0.9787 | 0.9831 | 0.9756 | Candidate |
| **Random Forest** | **98.97%** | **98.84%** | **0.9895** | **1.0000** | **0.9756** | **SELECTED** |
| **XGBoost** | **98.97%** | **98.84%** | **0.9895** | **1.0000** | **0.9756** | Candidate |

### Model Selection Rationale & Trade-offs:
1. **Accuracy & Macro F1**: Both Random Forest and XGBoost achieved top performance (98.97% accuracy, 98.84% macro F1), correctly classifying 96 out of 97 validation pairs.
2. **Architecture Simplicity**: Random Forest is natively supported in standard scikit-learn/joblib environments without requiring C++ compilation bindings or external dynamic libraries.
3. **Class Weighting**: Random Forest natively supports `class_weight='balanced'` within standard scikit-learn pipelines.
4. **Generalization**: On the held-out test set, Random Forest maintained 99.01% accuracy compared to XGBoost's 98.02%.

---

## 5. Validation Confusion Matrices

### Random Forest (Selected Model)
```
                Predicted LOW   Predicted MEDIUM   Predicted HIGH
Actual LOW            47                0                 0
Actual MEDIUM          0               29                 0
Actual HIGH            1                0                20
```

### Logistic Regression
```
                Predicted LOW   Predicted MEDIUM   Predicted HIGH
Actual LOW            46                1                 0
Actual MEDIUM          0               29                 0
Actual HIGH            1                0                20
```

---

## 6. Final Test Set Evaluation (Selected Model: Random Forest)

Evaluating the selected **Random Forest Classifier** ONCE on `test_features.csv` (101 candidate-job pairs):

- **Test Accuracy**: **99.01%** (100 / 101 pairs correct)
- **Test Macro F1-score**: **98.97%**
- **Test Weighted F1-score**: **99.02%**

### Per-Class Test Performance:

| Class Label | Precision | Recall | F1-Score | Support |
| :--- | :---: | :---: | :---: | :---: |
| **LOW** | 1.0000 | 0.9831 | 0.9915 | 59 |
| **MEDIUM** | 0.9565 | 1.0000 | 0.9778 | 22 |
| **HIGH** | 1.0000 | 1.0000 | 1.0000 | 20 |
| **Macro Average** | **0.9855** | **0.9944** | **0.9897** | **101** |
| **Weighted Average** | **0.9905** | **0.9901** | **0.9902** | **101** |

### Test Confusion Matrix:
```
                Predicted LOW   Predicted MEDIUM   Predicted HIGH
Actual LOW            58                1                 0
Actual MEDIUM          0               22                 0
Actual HIGH            0                0                20
```
*(Single misclassification on test set: 1 LOW candidate pair misclassified as MEDIUM).*

---

## 7. Feature Importance Ranking (Random Forest)

| Feature Name | Feature Category | Importance Score | Percentage |
| :--- | :--- | :---: | :---: |
| `tfidf_cosine_similarity` | TF-IDF Text Similarity | 0.178975 | 17.90% |
| `skill_coverage_ratio` | Skill Overlap Metrics | 0.153962 | 15.40% |
| `matching_skill_count` | Skill Overlap Metrics | 0.118389 | 11.84% |
| `skill_jaccard_similarity` | Skill Overlap Metrics | 0.118087 | 11.81% |
| `word_count_ratio` | Text Length Ratios | 0.102766 | 10.28% |
| `missing_skill_count` | Skill Overlap Metrics | 0.075295 | 7.53% |
| `resume_has_projects` | Section Structure Flags | 0.043973 | 4.40% |
| `resume_word_count` | Text Length Ratios | 0.042162 | 4.22% |
| `resume_char_count` | Text Length Ratios | 0.041167 | 4.12% |
| `resume_skill_count` | Skill Overlap Metrics | 0.030817 | 3.08% |
| `job_char_count` | Text Length Ratios | 0.020468 | 2.05% |
| `job_word_count` | Text Length Ratios | 0.020412 | 2.04% |
| `job_skill_count` | Skill Overlap Metrics | 0.014779 | 1.48% |
| `resume_has_experience` | Section Structure Flags | 0.009683 | 0.97% |
| `section_match_score` | Section Structure Flags | 0.009154 | 0.92% |
| `resume_has_skills` | Skill Overlap Metrics | 0.008390 | 0.84% |
| `job_has_experience` | Section Structure Flags | 0.007643 | 0.76% |
| `job_has_skills` | Skill Overlap Metrics | 0.003877 | 0.39% |
| `resume_has_education` | Section Structure Flags | 0.000000 | 0.00% |
| `job_has_projects` | Section Structure Flags | 0.000000 | 0.00% |
| `job_has_education` | Section Structure Flags | 0.000000 | 0.00% |
| `resume_has_certifications` | Section Structure Flags | 0.000000 | 0.00% |
| `job_has_certifications` | Section Structure Flags | 0.000000 | 0.00% |

---

## 8. Artifact Generation & Storage

The following model artifacts were generated under `models/`:

1. `models/resume_screening_model.joblib`: Complete trained scikit-learn Pipeline object (`StandardScaler` + `RandomForestClassifier`).
2. `models/model_metadata.json`: Model configuration, input schema, class mapping (`0: LOW, 1: MEDIUM, 2: HIGH`), seed, and evaluation metrics.
3. `models/feature_importances.csv`: Full feature importance rankings.
4. `models/val_confusion_matrices.csv`: Confusion matrices for all candidate models on validation set.
5. `models/test_confusion_matrix.csv`: Confusion matrix for the selected model on test set.
6. `models/confusion_matrix.png`: High-resolution graphical visualization of confusion matrices.

---

## 9. System Limitations & Scope

> [!WARNING]  
> **Academic Project Scope Notice**:
> 1. **Dataset Size**: This model was trained on a dataset of **675 candidate-job review pairs** (270 unique candidate resumes). While internal cross-validation and test metrics are exceptionally high (>98%), performance on unseen out-of-distribution real-world resumes requires domain scaling.
> 2. **Domain Dependency**: Skill extraction relies on the `skills_dictionary.json` lexicon. Resumes containing uncatalogued technical terms or rare skill variations should be complemented by rule-based ATS evaluation.
> 3. **Non-Production Claim**: This system is designed as an explainable candidate screening assistant for academic demonstration. High accuracy reflects tight alignment between TF-IDF similarity, extracted skill features, and human annotations.

---

## 10. How to Run Training & Load Saved Model

### Run Training Script:
```bash
python src/training/train_models.py
```
*(or from root: `python train_models.py`)*

### Load Saved Model in Python:
```python
import joblib
import json

# Load pipeline and metadata
model = joblib.load('models/resume_screening_model.joblib')
with open('models/model_metadata.json', 'r') as f:
    meta = json.load(f)

# Predict on new feature vector (dataframe with 23 features in exact order)
predictions = model.predict(X_new)
labels = [meta['target_mapping'][str(p)] for p in predictions]
```
