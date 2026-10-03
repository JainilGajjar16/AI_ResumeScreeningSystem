# Multi-Reviewer Batch Partitioning & Adjudication Strategy

This document outlines how the **675 candidate-job pairs** are divided among 3 independent human annotators (225 pairs each).

---

## 1. Multi-Reviewer Batch Distribution

The target dataset of **675 unique pairs** is partitioned into 3 distinct batches:

| Batch ID | Pair Range | Count | Assigned Reviewer | Status |
| :--- | :--- | :--- | :--- | :--- |
| **REV_A (Jainil)** | `PAIR_0001` - `PAIR_0225` | 225 pairs | Jainil (REV_A) | **225 / 225 COMPLETE** |
| **REV_B (Kashish)** | `PAIR_0226` - `PAIR_0450` | 225 pairs | Kashish (REV_B) | **0 / 225 PENDING** |
| **REV_C (Khush)** | `PAIR_0451` - `PAIR_0675` | 225 pairs | Khush (REV_C) | **0 / 225 PENDING** |

---

## 2. Multi-Reviewer Storage Schema

When multiple reviewers annotate the overlap set (`PAIR_0001` - `PAIR_0200`), individual decisions are recorded in `multi_reviewer_logs.csv` before consensus label assignment:

```csv
pair_id,reviewer_id,reviewer_label,reviewer_reason,timestamp
PAIR_0001,REV_A,HIGH,"Strong Java 17 and Spring Boot microservices experience matching job description.",2026-09-23T23:10:00Z
PAIR_0001,REV_B,HIGH,"Candidate background in backend Java and REST APIs is well aligned.",2026-09-23T23:12:00Z
PAIR_0001,REV_C,MEDIUM,"Good Java skills but lacks requested AWS cloud deployment experience.",2026-09-23T23:15:00Z
```

---

## 3. Consensus & Adjudication Workflow

```
                         Pair Annotation Completed
                                     │
                                     ▼
                       Are all Reviewer Labels Identical?
                                    │
               ┌────────────────────┴────────────────────┐
               │ YES                                     │ NO
               ▼                                         ▼
   Set final_label = Reviewer Label          Set review_status = ADJUDICATION_REQUIRED
   Set review_status = APPROVED                          │
                                                         ▼
                                             Senior Lead Reviewer inspects
                                             individual rationales & skills
                                                         │
                                                         ▼
                                             Assigns binding final_label
                                             Set review_status = ADJUDICATED
```

---

## 4. Execution Commands

Reviewers can verify their progress using the summary utility at any point:

```bash
python ai-resume-engine/src/preprocessing/annotation_summary.py
```
Or via Java:
```bash
java scratch/AnnotationSummaryRunner.java
```
