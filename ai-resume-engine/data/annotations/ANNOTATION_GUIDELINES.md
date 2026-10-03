# Human Resume-Job Match Annotation Guidelines

This document provides qualitative guidance for human reviewers evaluating candidate-job pairs for the **AI Resume Screening System**.

---

## Objective

Reviewers independently evaluate an anonymized candidate resume against a specific job posting and assign a qualitative match label (`HIGH`, `MEDIUM`, `LOW`) accompanied by a brief technical justification (`reviewer_reason`).

> [!IMPORTANT]
> **Qualitative Assessment Principle**:
> Ratings MUST be based on holistic human judgement rather than rigid mathematical percentage formulas (e.g. do NOT use rules such as "75% skills = HIGH" or "45% skills = MEDIUM").

---

## Evaluated Criteria

Reviewers must inspect the following 7 dimensions:
1. **Core Technical Skills**: Alignment with primary languages, frameworks, and databases.
2. **Complementary Tools**: Secondary tools (e.g., Git, Docker, Testing frameworks, CI/CD).
3. **Relevant Work Experience**: Quality and duration of domain-specific work experience.
4. **Project Demonstrations**: Practical application of technical skills in projects.
5. **Educational Background**: Specialization and degree relevance where applicable.
6. **Domain Alignment**: Industry sector fit (e.g., Backend, Frontend, Data, DevOps, QA).
7. **Overall Role Suitability**: Holistic evaluation of candidate readiness for the position.

---

## Rating Classes & Qualitative Definitions

### 1. HIGH MATCH (`HIGH`)

Assign **HIGH** when the candidate demonstrates strong overall suitability for the specific job posting.

**Characteristics**:
- Strong match in primary technical language/framework requirements (e.g. Java + Spring Boot for a Java Backend role).
- Relevant work experience duration or high-quality project demonstrations matching role expectations.
- Good domain alignment with minimal or minor skill gaps in non-critical tools.

**Example Reviewer Rationale**:
> `"Candidate demonstrates solid Java 17 and Spring Boot experience with microservices projects and PostgreSQL knowledge. Excellent fit for the Backend Developer role."`

---

### 2. MEDIUM MATCH (`MEDIUM`)

Assign **MEDIUM** when there is meaningful overall alignment but also noticeable skill, experience, or domain gaps.

**Characteristics**:
- Core programming foundation is present, but some important secondary skills or frameworks are missing (e.g. strong React developer missing TypeScript/GraphQL).
- Minor experience deficits offset by relevant hands-on project work.
- Partial domain alignment or transition between related technical domains.

**Example Reviewer Rationale**:
> `"Candidate has strong Java and Spring Boot fundamentals with good database skills, but lacks the requested AWS cloud infrastructure experience."`

---

### 3. LOW MATCH (`LOW`)

Assign **LOW** when there is a substantial mismatch between the candidate background and job requirements.

**Characteristics**:
- Missing major core technical prerequisites (e.g., Python Data Analyst resume evaluated against a Senior Java Developer job).
- Strongly unrelated technical domain (e.g., Graphic Design background for a DevOps Engineering position).
- Severe lack of relevant experience or technical skills required by the posting.

**Example Reviewer Rationale**:
> `"Candidate background is in QA automation and Selenium testing; lacks backend Java microservices development experience required for this role."`

---

## Reviewer Instructions & Workflow

1. Open the assigned batch from `human_annotation.csv`.
2. Inspect `resume_raw_text` and `job_raw_text`.
3. Assign `reviewer_id` (e.g., `REV_A`, `REV_B`, `REV_C`).
4. Select `reviewer_label` (`HIGH`, `MEDIUM`, `LOW`).
5. Write a concise 1-2 sentence justification in `reviewer_reason` highlighting key matches or missing requirements.
6. Do NOT modify `pair_id`, `resume_id`, or `job_id`.
7. Leave `final_label` blank until adjudication.

---

## Multi-Reviewer Adjudication Protocol

```
PAIR_ID
   │
   ├─► Reviewer A Rating & Reason
   ├─► Reviewer B Rating & Reason
   └─► Reviewer C Rating & Reason
   │
   ▼
[ Adjudication Review ]
   │
   ├─► All Agree ──────────────► Set final_label = Unanimous Rating, status = APPROVED
   └─► Split Decision (2 vs 1) ─► Senior Adjudicator reviews rationale, sets final_label & status = ADJUDICATED
```

- **Unanimous Agreement**: Automatically approved.
- **Split Decisions**: Evaluated by a senior annotator reviewing individual rationales (`reviewer_reason`). The consensus label is assigned without blindly forcing majority voting.
