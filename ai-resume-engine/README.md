# AI Resume Engine (Phase 3 Dataset & ML Service Workspace)

This directory contains the Python-based machine learning engine and dataset preparation pipeline for the AI Resume Screening System.

## Workspace Directory Structure

```
ai-resume-engine/
├── data/
│   ├── raw/           # Raw downloaded open datasets (e.g. Kaggle Resume Dataset, Job Descriptions)
│   ├── processed/     # Cleaned, deduplicated, and normalized resume/job data
│   ├── annotations/   # Candidate-job pairs formatted for human annotation
│   └── dictionaries/  # Technical skill preservation dictionary (e.g., C++, .NET, Spring Boot)
├── models/            # Saved vectorizer, scaler, and model binaries (.joblib)
├── src/
│   ├── preprocessing/ # Text cleaning, skill token preservation, section parser
│   ├── features/      # TF-IDF cosine similarity, skill Jaccard/coverage extractors
│   ├── training/      # Supervised model training scripts (Logistic Regression, Random Forest, XGBoost)
│   └── api/           # FastAPI microservice for Spring Boot integration
├── notebooks/         # Exploratory analysis & dataset inspection notebooks
├── requirements.txt   # Python dependencies
└── README.md          # Workspace documentation
```

## Phase 3 Scope & Guidelines
1. **Zero External AI APIs**: All preprocessing, feature extraction, and model training are performed locally.
2. **Human-Reviewed Ground Truth**: Labels (`HIGH`, `MEDIUM`, `LOW`) are assigned via human annotation guidelines rather than hardcoded percentage rules.
3. **Preventing Data Leakage**: Grouped stratified train/validation/test splits based on candidate IDs ensure generalization to unseen resumes.
