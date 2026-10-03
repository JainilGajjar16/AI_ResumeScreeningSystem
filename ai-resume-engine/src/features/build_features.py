"""
AI Resume Screening System - Feature Extraction Module
Location: ai-resume-engine/src/features/build_features.py
"""

import os
import sys
import json
import re
import random
import pandas as pd
import numpy as np
import joblib
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity

# Constants & Paths
BASE_DIR = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ANNOTATIONS_DIR = os.path.join(BASE_DIR, "data", "annotations")
PROCESSED_DIR = os.path.join(BASE_DIR, "data", "processed")
MODELS_DIR = os.path.join(BASE_DIR, "models")
DICTIONARIES_DIR = os.path.join(BASE_DIR, "data", "dictionaries")

REVIEWS_CSV = os.path.join(ANNOTATIONS_DIR, "annotation_reviews.csv")
UNLABELED_CSV = os.path.join(ANNOTATIONS_DIR, "unlabeled_pairs.csv")
SKILLS_DICT_JSON = os.path.join(DICTIONARIES_DIR, "skills_dictionary.json")

RANDOM_SEED = 62
LABEL_MAPPING = {"LOW": 0, "MEDIUM": 1, "HIGH": 2}

def load_skills_dictionary():
    """Load technical skills and token preservation regex mappings."""
    if not os.path.exists(SKILLS_DICT_JSON):
        raise FileNotFoundError(f"Skills dictionary not found at {SKILLS_DICT_JSON}")
    
    with open(SKILLS_DICT_JSON, "r", encoding="utf-8") as f:
        data = json.load(f)
    
    tech_skills = data.get("technical_skills", [])
    preservation_map = data.get("token_preservation_regex", {})
    return tech_skills, preservation_map

def preprocess_text(text, preservation_map):
    """Normalize text and apply skill token preservation regexes."""
    if not isinstance(text, str) or pd.isna(text):
        return ""
    
    text_lower = text.lower()
    for raw_token, token_replacement in preservation_map.items():
        pattern = r"\b" + re.escape(raw_token) + r"\b"
        text_lower = re.sub(pattern, token_replacement.lower(), text_lower)
    
    # Remove special punctuation but keep alphanumeric and underscores
    text_clean = re.sub(r"[^\w\s]", " ", text_lower)
    text_clean = re.sub(r"\s+", " ", text_clean).strip()
    return text_clean

def extract_skill_set(text, tech_skills):
    """Extract technical skills present in text using word boundaries."""
    if not isinstance(text, str) or pd.isna(text):
        return set()
    
    text_lower = text.lower()
    found = set()
    for skill in tech_skills:
        pattern = r"(?<!\w)" + re.escape(skill.lower()) + r"(?!\w)"
        if re.search(pattern, text_lower):
            found.add(skill)
    return found

def detect_sections(text):
    """Detect presence of key structural resume/job sections."""
    if not isinstance(text, str) or pd.isna(text):
        return {
            "has_experience": 0, "has_education": 0,
            "has_projects": 0, "has_skills": 0, "has_certifications": 0
        }
    
    t_lower = text.lower()
    return {
        "has_experience": int(bool(re.search(r"\b(experience|worked|built|developed|years|role)\b", t_lower))),
        "has_education": int(bool(re.search(r"\b(bachelor|master|degree|university|college|education|bs|ms)\b", t_lower))),
        "has_projects": int(bool(re.search(r"\b(projects|project|built|system|app)\b", t_lower))),
        "has_skills": int(bool(re.search(r"\b(skills|proficient|technical|stack|languages)\b", t_lower))),
        "has_certifications": int(bool(re.search(r"\b(certif|aws certified|certified|license)\b", t_lower)))
    }

def create_grouped_splits(merged_df, train_pct=0.70, val_pct=0.15, seed=RANDOM_SEED):
    """Split candidate-job pairs into reproducible Train, Val, and Test sets grouped by resume_id."""
    random.seed(seed)
    np.random.seed(seed)
    
    unique_resumes = sorted(list(merged_df['resume_id'].unique()))
    random.shuffle(unique_resumes)
    
    n_total = len(unique_resumes)
    n_train = int(n_total * train_pct)
    n_val = int(n_total * val_pct)
    
    train_resumes = set(unique_resumes[:n_train])
    val_resumes = set(unique_resumes[n_train:n_train + n_val])
    test_resumes = set(unique_resumes[n_train + n_val:])
    
    train_df = merged_df[merged_df['resume_id'].isin(train_resumes)].copy()
    val_df = merged_df[merged_df['resume_id'].isin(val_resumes)].copy()
    test_df = merged_df[merged_df['resume_id'].isin(test_resumes)].copy()
    
    # Verify no candidate resume overlap
    assert len(train_resumes.intersection(val_resumes)) == 0, "Resume leakage between Train and Val!"
    assert len(train_resumes.intersection(test_resumes)) == 0, "Resume leakage between Train and Test!"
    assert len(val_resumes.intersection(test_resumes)) == 0, "Resume leakage between Val and Test!"
    
    return train_df, val_df, test_df

def compute_features(df, tfidf_vectorizer, tech_skills, preservation_map, is_train=False):
    """
    Extract all features for a dataframe:
    - Text TF-IDF cosine similarity
    - Skill overlap (Jaccard, Coverage, Match Counts)
    - Section coverage flags
    - Explainable text metrics
    """
    df = df.copy()
    
    # Preprocess text
    df['clean_resume_text'] = df['resume_raw_text'].apply(lambda x: preprocess_text(x, preservation_map))
    df['clean_job_text'] = df['job_raw_text'].apply(lambda x: preprocess_text(x, preservation_map))
    
    # 1. TF-IDF & Cosine Similarity
    if is_train:
        # Fit vectorizer only on training set corpus
        train_corpus = pd.concat([df['clean_resume_text'], df['clean_job_text']])
        tfidf_vectorizer.fit(train_corpus)
    
    res_tfidf = tfidf_vectorizer.transform(df['clean_resume_text'])
    job_tfidf = tfidf_vectorizer.transform(df['clean_job_text'])
    
    # Compute row-wise cosine similarity between corresponding resume and job vectors
    cosine_sims = np.array([
        cosine_similarity(res_tfidf[i], job_tfidf[i])[0][0]
        for i in range(len(df))
    ])
    
    features_list = []
    
    for idx, row in df.iterrows():
        r_text = str(row['resume_raw_text']) if pd.notna(row['resume_raw_text']) else ""
        j_text = str(row['job_raw_text']) if pd.notna(row['job_raw_text']) else ""
        
        # Skill extraction
        r_skills = extract_skill_set(r_text, tech_skills)
        j_skills = extract_skill_set(j_text, tech_skills)
        
        matching = r_skills.intersection(j_skills)
        missing = j_skills - r_skills
        union_skills = r_skills.union(j_skills)
        
        r_skill_cnt = len(r_skills)
        j_skill_cnt = len(j_skills)
        match_cnt = len(matching)
        missing_cnt = len(missing)
        
        jaccard = match_cnt / len(union_skills) if len(union_skills) > 0 else 0.0
        coverage = match_cnt / j_skill_cnt if j_skill_cnt > 0 else 0.0
        
        # Section coverage
        r_sec = detect_sections(r_text)
        j_sec = detect_sections(j_text)
        
        sec_match_score = sum(
            1 for k in r_sec if r_sec[k] == j_sec[k] and r_sec[k] == 1
        )
        
        # Text metrics
        r_char_len = len(r_text)
        j_char_len = len(j_text)
        r_word_cnt = len(r_text.split())
        j_word_cnt = len(j_text.split())
        word_cnt_ratio = r_word_cnt / (j_word_cnt + 1e-5)
        
        feat_dict = {
            "pair_id": row["pair_id"],
            "resume_id": row["resume_id"],
            "job_id": row["job_id"],
            "label": row["reviewer_label"],
            "label_encoded": LABEL_MAPPING.get(str(row["reviewer_label"]).upper(), -1),
            
            # TF-IDF Feature
            "tfidf_cosine_similarity": float(cosine_sims[len(features_list)]),
            
            # Skill Features
            "resume_skill_count": r_skill_cnt,
            "job_skill_count": j_skill_cnt,
            "matching_skill_count": match_cnt,
            "missing_skill_count": missing_cnt,
            "skill_jaccard_similarity": float(jaccard),
            "skill_coverage_ratio": float(coverage),
            
            # Section Features
            "resume_has_experience": r_sec["has_experience"],
            "resume_has_education": r_sec["has_education"],
            "resume_has_projects": r_sec["has_projects"],
            "resume_has_skills": r_sec["has_skills"],
            "resume_has_certifications": r_sec["has_certifications"],
            "job_has_experience": j_sec["has_experience"],
            "job_has_education": j_sec["has_education"],
            "job_has_projects": j_sec["has_projects"],
            "job_has_skills": j_sec["has_skills"],
            "job_has_certifications": j_sec["has_certifications"],
            "section_match_score": sec_match_score,
            
            # Text Features
            "resume_char_count": r_char_len,
            "job_char_count": j_char_len,
            "resume_word_count": r_word_cnt,
            "job_word_count": j_word_cnt,
            "word_count_ratio": float(word_cnt_ratio)
        }
        features_list.append(feat_dict)
    
    return pd.DataFrame(features_list)

def main():
    print("==========================================================================")
    print("    AI RESUME SCREENING SYSTEM - PHASE 4B FEATURE EXTRACTION & SPLITTING  ")
    print("==========================================================================")
    
    os.makedirs(PROCESSED_DIR, exist_ok=True)
    os.makedirs(MODELS_DIR, exist_ok=True)
    
    # 1. Load Data
    print("\n--- 1. LOADING DATASETS ---")
    df_rev = pd.read_csv(REVIEWS_CSV)
    df_unlabeled = pd.read_csv(UNLABELED_CSV)
    tech_skills, preservation_map = load_skills_dictionary()
    
    print(f"Loaded {len(df_rev)} reviews from annotation_reviews.csv")
    print(f"Loaded {len(df_unlabeled)} pairs from unlabeled_pairs.csv")
    print(f"Loaded {len(tech_skills)} skills from skills_dictionary.json")
    
    merged = pd.merge(df_rev, df_unlabeled, on="pair_id", how="left")
    print(f"Merged dataset shape: {merged.shape}")
    
    # Check missing values
    missing_report = merged.isna().sum().to_dict()
    print("Missing value report:", missing_report)
    
    # 2. Grouped Dataset Splitting
    print("\n--- 2. CREATING REPRODUCIBLE GROUPED SPLITS ---")
    train_df, val_df, test_df = create_grouped_splits(merged, train_pct=0.70, val_pct=0.15, seed=RANDOM_SEED)
    
    print(f"Train split : {len(train_df):3d} pairs ({len(train_df)/len(merged):5.1%}), {train_df['resume_id'].nunique():3d} unique resumes")
    print(f"Val split   : {len(val_df):3d} pairs ({len(val_df)/len(merged):5.1%}), {val_df['resume_id'].nunique():3d} unique resumes")
    print(f"Test split  : {len(test_df):3d} pairs ({len(test_df)/len(merged):5.1%}), {test_df['resume_id'].nunique():3d} unique resumes")
    
    # Save raw split CSVs under data/processed/
    split_cols = ["pair_id", "resume_id", "job_id", "reviewer_label", "reviewer_reason", "resume_raw_text", "job_raw_text"]
    train_df[split_cols].to_csv(os.path.join(PROCESSED_DIR, "train_split.csv"), index=False)
    val_df[split_cols].to_csv(os.path.join(PROCESSED_DIR, "val_split.csv"), index=False)
    test_df[split_cols].to_csv(os.path.join(PROCESSED_DIR, "test_split.csv"), index=False)
    print("[SUCCESS] Raw split CSV files saved under data/processed/")
    
    # 3. Fit TF-IDF Pipeline & Extract Features
    print("\n--- 3. FEATURE EXTRACTION & NO-LEAKAGE TRANSFORM ---")
    tfidf = TfidfVectorizer(ngram_range=(1, 2), min_df=2, max_features=1000)
    
    # Fit TF-IDF ONLY on training set
    train_features_df = compute_features(train_df, tfidf, tech_skills, preservation_map, is_train=True)
    val_features_df = compute_features(val_df, tfidf, tech_skills, preservation_map, is_train=False)
    test_features_df = compute_features(test_df, tfidf, tech_skills, preservation_map, is_train=False)
    
    # Save feature CSVs
    train_features_df.to_csv(os.path.join(PROCESSED_DIR, "train_features.csv"), index=False)
    val_features_df.to_csv(os.path.join(PROCESSED_DIR, "val_features.csv"), index=False)
    test_features_df.to_csv(os.path.join(PROCESSED_DIR, "test_features.csv"), index=False)
    
    # Save fitted TF-IDF vectorizer model binary
    tfidf_model_path = os.path.join(MODELS_DIR, "tfidf_vectorizer.joblib")
    joblib.dump(tfidf, tfidf_model_path)
    print(f"[SUCCESS] Fitted TF-IDF vectorizer saved to {tfidf_model_path}")
    print("[SUCCESS] Feature CSV files saved under data/processed/")
    
    # 4. Summary & Verification Output
    print("\n--- 4. FEATURE EXTRACTION SUMMARY ---")
    print("Extracted Feature Columns:", [c for c in train_features_df.columns if c not in ["pair_id", "resume_id", "job_id", "label", "label_encoded"]])
    print(f"Total Features Extracted   : {len(train_features_df.columns) - 5}")
    print("\nSample Feature Metrics (Train set average):")
    print(f"  - Avg TF-IDF Cosine Similarity : {train_features_df['tfidf_cosine_similarity'].mean():.4f}")
    print(f"  - Avg Skill Jaccard Similarity : {train_features_df['skill_jaccard_similarity'].mean():.4f}")
    print(f"  - Avg Skill Coverage Ratio     : {train_features_df['skill_coverage_ratio'].mean():.4f}")
    print(f"  - Avg Matching Skill Count     : {train_features_df['matching_skill_count'].mean():.2f}")
    print("==========================================================================")
    print("RESULT: [PASS] PHASE 4B FEATURE EXTRACTION & SPLITTING COMPLETED!")
    print("==========================================================================")

if __name__ == "__main__":
    main()
