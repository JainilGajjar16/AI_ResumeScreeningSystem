import csv
import os
import random

# Technical domains and sample clean resume texts & job descriptions for 5 domains
domains = {
    "Software Development": {
        "resumes": [
            "Java Developer with 3+ years experience building Spring Boot microservices, REST APIs, and MySQL databases. Proficient in Git, JUnit, Docker, and CI/CD pipelines.",
            "Backend Engineer specializing in Java 17, Spring Security, Hibernate, PostgreSQL, and Kafka. Demonstrated background in unit testing and scalable architecture.",
            "Junior Java Developer with B.Tech in CS. Knowledge of Java, Data Structures, OOP, SQL, and HTML/CSS. Project experience building an e-commerce backend.",
            "Senior Software Developer with 6 years experience in Java, Microservices, AWS, Docker, Kubernetes, and Redis. Proven track record leading dev teams."
        ],
        "jobs": [
            "Looking for Backend Java Developer with 2-4 years experience in Java, Spring Boot, REST APIs, and MySQL.",
            "Senior Java Microservices Architect requiring 5+ years experience in Java 17, Spring Cloud, AWS, and Docker."
        ]
    },
    "Web Development": {
        "resumes": [
            "Frontend React Developer with 2 years experience building responsive web apps with React.js, Redux, JavaScript, HTML5, CSS3, and Tailwind CSS.",
            "Full Stack Web Developer proficient in Node.js, Express.js, React, MongoDB, and TypeScript. Experience integrating REST APIs and modern UI/UX.",
            "Web UI Engineer skilled in HTML, CSS, JavaScript, Vue.js, Bootstrap, and Git. Focused on accessibility and cross-browser performance.",
            "Junior Web Developer with strong foundation in JavaScript, HTML5, CSS3, and basic React. Built portfolio projects including blogging platform."
        ],
        "jobs": [
            "Seeking React Frontend Developer with 2+ years experience in React.js, JavaScript (ES6+), HTML5/CSS3, and REST API integration.",
            "Full Stack JavaScript Developer needed. Requirements: Node.js, React, MongoDB, and TypeScript."
        ]
    },
    "Data Science / Analytics": {
        "resumes": [
            "Data Analyst with expertise in SQL, Python, Pandas, NumPy, Tableau, and Excel. 2 years experience conducting exploratory data analysis and ETL pipelines.",
            "Machine Learning Engineer skilled in Python, Scikit-Learn, TensorFlow, PyTorch, SQL, and ML model deployment. M.Sc in Data Science.",
            "Data Scientist proficient in Python, R, Statistical Modeling, NLP, Scikit-Learn, and PowerBI. Experience in predictive modeling.",
            "Junior Data Analyst with B.Sc in Statistics. Proficient in Python, SQL, Excel data visualization, and basic machine learning algorithms."
        ],
        "jobs": [
            "Hiring Data Analyst with strong SQL, Python (Pandas/NumPy), and Tableau data visualization skills.",
            "Machine Learning Engineer position requiring Python, Scikit-Learn, PyTorch, and ML model training experience."
        ]
    },
    "DevOps / Cloud": {
        "resumes": [
            "DevOps Engineer with 3 years experience managing AWS cloud infrastructure, Terraform, Docker, Kubernetes, Jenkins CI/CD, and Linux bash scripting.",
            "Cloud Infrastructure Engineer skilled in Azure, Docker, Kubernetes, Ansible, Python scripting, and network security configuration.",
            "Site Reliability Engineer (SRE) proficient in Linux system administration, AWS, Prometheus, Grafana, and automated deployment pipelines.",
            "Junior Cloud Systems Administrator with AWS Certified Solutions Architect Associate credential. Knowledge of Docker, Linux, and Shell scripting."
        ],
        "jobs": [
            "Looking for DevOps Engineer with experience in AWS, Docker, Kubernetes, CI/CD pipelines, and Terraform.",
            "Cloud Security & DevOps Specialist required. Mandatory skills: Azure, Docker, Kubernetes, and Linux automation."
        ]
    },
    "QA / Automation Testing": {
        "resumes": [
            "QA Automation Engineer with 3 years experience writing automated test scripts using Java, Selenium WebDriver, TestNG, Cucumber, and Postman REST API testing.",
            "Software Tester skilled in Manual Testing, SDLC, Test Case creation, Defect Tracking in Jira, and basic Selenium with Java.",
            "Senior QA Lead proficient in Test Automation Frameworks, Selenium, Python, PyTest, Jenkins integration, and API testing with Postman.",
            "Junior QA Analyst with knowledge of Software Testing Life Cycle (STLC), Bug reporting, SQL queries, and Selenium WebDriver basics."
        ],
        "jobs": [
            "Hiring QA Automation Engineer with strong Java, Selenium WebDriver, TestNG, and REST API testing skills.",
            "Senior Software Quality Assurance Lead requiring Selenium, Python automation, Postman API testing, and Jira defect tracking."
        ]
    }
}

target_file = r"c:\Users\jaini\Documents\AI-ResumeScreeningSystem\ai-resume-engine\data\annotations\unlabeled_pairs.csv"
os.makedirs(os.path.dirname(target_file), exist_ok=True)

resumes = []
jobs = []

# Build distinct pools of resumes (590) and jobs (125)
res_count = 0
for domain_name, data in domains.items():
    for r_idx in range(118): # 118 * 5 = 590 resumes
        res_text = data["resumes"][r_idx % len(data["resumes"])]
        resumes.append({
            "resume_id": f"RES_{domain_name[:3].upper()}_{res_count+1:04d}",
            "domain": domain_name,
            "text": f"{res_text} (Candidate Ref {res_count+1})"
        })
        res_count += 1

job_count = 0
for domain_name, data in domains.items():
    for j_idx in range(25): # 25 * 5 = 125 jobs
        j_text = data["jobs"][j_idx % len(data["jobs"])]
        jobs.append({
            "job_id": f"JOB_{domain_name[:3].upper()}_{job_count+1:04d}",
            "domain": domain_name,
            "text": f"{j_text} (Job Posting Ref {job_count+1})"
        })
        job_count += 1

# Generate exactly 1,475 candidate-job pairs (average 2.5 pairs per resume)
pairs = []
pair_count = 0

# Strategy: Each resume gets 2 intra-domain pairs + 0.5 cross-domain pair
random.seed(42)

for r in resumes:
    # 2 intra-domain jobs
    same_domain_jobs = [j for j in jobs if j["domain"] == r["domain"]]
    other_domain_jobs = [j for j in jobs if j["domain"] != r["domain"]]
    
    selected_jobs = random.sample(same_domain_jobs, 2)
    if pair_count % 2 == 0:
        selected_jobs.append(random.choice(other_domain_jobs))
        
    for j in selected_jobs:
        pair_count += 1
        pairs.append({
            "pair_id": f"PAIR_{pair_count:04d}",
            "resume_id": r["resume_id"],
            "job_id": j["job_id"],
            "resume_raw_text": r["text"],
            "job_raw_text": j["text"]
        })
        if len(pairs) >= 1475:
            break
    if len(pairs) >= 1475:
        break

with open(target_file, "w", newline="", encoding="utf-8") as f:
    writer = csv.DictWriter(f, fieldnames=["pair_id", "resume_id", "job_id", "resume_raw_text", "job_raw_text"])
    writer.writeheader()
    writer.writerows(pairs[:1475])

print(f"Successfully generated {len(pairs[:1475])} unlabeled candidate-job pairs in {target_file}")
