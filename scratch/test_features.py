import os
import json
import re
import pandas as pd
import numpy as np
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity

base_dir = r"C:\Users\jaini\Documents\AI-ResumeScreeningSystem\ai-resume-engine"
dict_path = os.path.join(base_dir, "data", "dictionaries", "skills_dictionary.json")

with open(dict_path, "r", encoding="utf-8") as f:
    skills_dict = json.load(f)

tech_skills = skills_dict.get("technical_skills", [])
preservation_regex = skills_dict.get("token_preservation_regex", {})

print(f"Loaded {len(tech_skills)} technical skills.")
print("Token preservation mapping:", preservation_regex)

def preprocess_text(text):
    if not isinstance(text, str):
        return ""
    text_lower = text.lower()
    # Apply token preservation regexes
    for orig, replacement in preservation_regex.items():
        pattern = r"\b" + re.escape(orig) + r"\b"
        text_lower = re.sub(pattern, replacement.lower(), text_lower)
    return text_lower

def extract_skills(text):
    if not isinstance(text, str):
        return set()
    text_lower = text.lower()
    found = set()
    for skill in tech_skills:
        # Match word boundary
        pattern = r"(?<!\w)" + re.escape(skill.lower()) + r"(?!\w)"
        if re.search(pattern, text_lower):
            found.add(skill)
    return found

def detect_sections(text):
    if not isinstance(text, str):
        return {}
    text_lower = text.lower()
    sections = {
        "has_experience": int(bool(re.search(r"\b(experience|worked|built|developed|years)\b", text_lower))),
        "has_education": int(bool(re.search(r"\b(bachelor|master|degree|university|college|education|bs|ms)\b", text_lower))),
        "has_projects": int(bool(re.search(r"\b(projects|project|built|created)\b", text_lower))),
        "has_skills_section": int(bool(re.search(r"\b(skills|proficient|technical|stack)\b", text_lower))),
        "has_certifications": int(bool(re.search(r"\b(certif|aws certified|certified)\b", text_lower)))
    }
    return sections

# Test sample text
sample_resume = "Java Developer with 3+ years experience building Spring Boot microservices, REST APIs, C++, and MySQL databases. Proficient in Git, JUnit, Docker, and CI/CD."
sample_job = "Senior Java Microservices Architect requiring 5+ years experience in Java 17, Spring Cloud, AWS, C++, and Docker."

clean_res = preprocess_text(sample_resume)
clean_job = preprocess_text(sample_job)

res_skills = extract_skills(sample_resume)
job_skills = extract_skills(sample_job)

print("\nProcessed Resume Text:", clean_res)
print("Resume Skills:", res_skills)
print("Job Skills:", job_skills)
print("Matching Skills:", res_skills.intersection(job_skills))

# Section coverage
print("Resume Sections:", detect_sections(sample_resume))
print("Job Sections:", detect_sections(sample_job))

# TF-IDF Cosine Similarity Test
vec = TfidfVectorizer(ngram_range=(1, 2), min_df=1)
tfidf_mat = vec.fit_transform([clean_res, clean_job])
sim = cosine_similarity(tfidf_mat[0:1], tfidf_mat[1:2])[0][0]
print(f"TF-IDF Cosine Similarity: {sim:.4f}")
