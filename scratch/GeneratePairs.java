package scratch;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GeneratePairs {

    static class DomainData {
        String domain;
        String[] resumes;
        String[] jobs;

        DomainData(String domain, String[] resumes, String[] jobs) {
            this.domain = domain;
            this.resumes = resumes;
            this.jobs = jobs;
        }
    }

    public static void main(String[] args) throws Exception {
        DomainData[] domains = new DomainData[] {
            new DomainData("Software Development",
                new String[] {
                    "Java Developer with 3+ years experience building Spring Boot microservices, REST APIs, and MySQL databases. Proficient in Git, JUnit, Docker, and CI/CD pipelines.",
                    "Backend Engineer specializing in Java 17, Spring Security, Hibernate, PostgreSQL, and Kafka. Demonstrated background in unit testing and scalable architecture.",
                    "Junior Java Developer with B.Tech in CS. Knowledge of Java, Data Structures, OOP, SQL, and HTML/CSS. Project experience building an e-commerce backend.",
                    "Senior Software Developer with 6 years experience in Java, Microservices, AWS, Docker, Kubernetes, and Redis. Proven track record leading dev teams."
                },
                new String[] {
                    "Looking for Backend Java Developer with 2-4 years experience in Java, Spring Boot, REST APIs, and MySQL.",
                    "Senior Java Microservices Architect requiring 5+ years experience in Java 17, Spring Cloud, AWS, and Docker."
                }
            ),
            new DomainData("Web Development",
                new String[] {
                    "Frontend React Developer with 2 years experience building responsive web apps with React.js, Redux, JavaScript, HTML5, CSS3, and Tailwind CSS.",
                    "Full Stack Web Developer proficient in Node.js, Express.js, React, MongoDB, and TypeScript. Experience integrating REST APIs and modern UI/UX.",
                    "Web UI Engineer skilled in HTML, CSS, JavaScript, Vue.js, Bootstrap, and Git. Focused on accessibility and cross-browser performance.",
                    "Junior Web Developer with strong foundation in JavaScript, HTML5, CSS3, and basic React. Built portfolio projects including blogging platform."
                },
                new String[] {
                    "Seeking React Frontend Developer with 2+ years experience in React.js, JavaScript (ES6+), HTML5/CSS3, and REST API integration.",
                    "Full Stack JavaScript Developer needed. Requirements: Node.js, React, MongoDB, and TypeScript."
                }
            ),
            new DomainData("Data Science / Analytics",
                new String[] {
                    "Data Analyst with expertise in SQL, Python, Pandas, NumPy, Tableau, and Excel. 2 years experience conducting exploratory data analysis and ETL pipelines.",
                    "Machine Learning Engineer skilled in Python, Scikit-Learn, TensorFlow, PyTorch, SQL, and ML model deployment. M.Sc in Data Science.",
                    "Data Scientist proficient in Python, R, Statistical Modeling, NLP, Scikit-Learn, and PowerBI. Experience in predictive modeling.",
                    "Junior Data Analyst with B.Sc in Statistics. Proficient in Python, SQL, Excel data visualization, and basic machine learning algorithms."
                },
                new String[] {
                    "Hiring Data Analyst with strong SQL, Python (Pandas/NumPy), and Tableau data visualization skills.",
                    "Machine Learning Engineer position requiring Python, Scikit-Learn, PyTorch, and ML model training experience."
                }
            ),
            new DomainData("DevOps / Cloud",
                new String[] {
                    "DevOps Engineer with 3 years experience managing AWS cloud infrastructure, Terraform, Docker, Kubernetes, Jenkins CI/CD, and Linux bash scripting.",
                    "Cloud Infrastructure Engineer skilled in Azure, Docker, Kubernetes, Ansible, Python scripting, and network security configuration.",
                    "Site Reliability Engineer (SRE) proficient in Linux system administration, AWS, Prometheus, Grafana, and automated deployment pipelines.",
                    "Junior Cloud Systems Administrator with AWS Certified Solutions Architect Associate credential. Knowledge of Docker, Linux, and Shell scripting."
                },
                new String[] {
                    "Looking for DevOps Engineer with experience in AWS, Docker, Kubernetes, CI/CD pipelines, and Terraform.",
                    "Cloud Security & DevOps Specialist required. Mandatory skills: Azure, Docker, Kubernetes, and Linux automation."
                }
            ),
            new DomainData("QA / Automation Testing",
                new String[] {
                    "QA Automation Engineer with 3 years experience writing automated test scripts using Java, Selenium WebDriver, TestNG, Cucumber, and Postman REST API testing.",
                    "Software Tester skilled in Manual Testing, SDLC, Test Case creation, Defect Tracking in Jira, and basic Selenium with Java.",
                    "Senior QA Lead proficient in Test Automation Frameworks, Selenium, Python, PyTest, Jenkins integration, and API testing with Postman.",
                    "Junior QA Analyst with knowledge of Software Testing Life Cycle (STLC), Bug reporting, SQL queries, and Selenium WebDriver basics."
                },
                new String[] {
                    "Hiring QA Automation Engineer with strong Java, Selenium WebDriver, TestNG, and REST API testing skills.",
                    "Senior Software Quality Assurance Lead requiring Selenium, Python automation, Postman API testing, and Jira defect tracking."
                }
            )
        };

        List<String[]> resumes = new ArrayList<>();
        List<String[]> jobs = new ArrayList<>();

        int resCount = 0;
        for (DomainData d : domains) {
            String code = d.domain.substring(0, 3).toUpperCase();
            for (int i = 0; i < 118; i++) {
                resCount++;
                String id = String.format("RES_%s_%04d", code, resCount);
                String text = d.resumes[i % d.resumes.length] + " (Candidate Ref " + resCount + ")";
                resumes.add(new String[]{id, d.domain, text});
            }
        }

        int jobCount = 0;
        for (DomainData d : domains) {
            String code = d.domain.substring(0, 3).toUpperCase();
            for (int i = 0; i < 25; i++) {
                jobCount++;
                String id = String.format("JOB_%s_%04d", code, jobCount);
                String text = d.jobs[i % d.jobs.length] + " (Job Posting Ref " + jobCount + ")";
                jobs.add(new String[]{id, d.domain, text});
            }
        }

        File targetUnlabeled = new File("ai-resume-engine/data/annotations/unlabeled_pairs.csv");
        File targetHuman = new File("ai-resume-engine/data/annotations/human_annotation.csv");
        targetUnlabeled.getParentFile().mkdirs();

        PrintWriter pwUnlabeled = new PrintWriter(new FileWriter(targetUnlabeled));
        PrintWriter pwHuman = new PrintWriter(new FileWriter(targetHuman));

        pwUnlabeled.println("pair_id,resume_id,job_id,resume_raw_text,job_raw_text");
        pwHuman.println("pair_id,resume_id,job_id,resume_raw_text,job_raw_text,reviewer_id,reviewer_label,reviewer_reason,final_label,review_status");

        int pairCount = 0;
        Random rng = new Random(42);

        for (int rIdx = 0; rIdx < resumes.size() && pairCount < 1475; rIdx++) {
            String[] r = resumes.get(rIdx);
            String rDomain = r[1];

            List<String[]> sameDomainJobs = new ArrayList<>();
            List<String[]> otherDomainJobs = new ArrayList<>();
            for (String[] j : jobs) {
                if (j[1].equals(rDomain)) sameDomainJobs.add(j);
                else otherDomainJobs.add(j);
            }

            int j1Idx = rng.nextInt(sameDomainJobs.size());
            int j2Idx = (j1Idx + 1) % sameDomainJobs.size();

            List<String[]> selected = new ArrayList<>();
            selected.add(sameDomainJobs.get(j1Idx));
            selected.add(sameDomainJobs.get(j2Idx));

            if (rIdx % 2 == 0) {
                selected.add(otherDomainJobs.get(rng.nextInt(otherDomainJobs.size())));
            }

            for (String[] j : selected) {
                if (pairCount >= 1475) break;
                pairCount++;
                String pairId = String.format("PAIR_%04d", pairCount);

                String rTextEsc = escapeCsv(r[2]);
                String jTextEsc = escapeCsv(j[2]);

                pwUnlabeled.println(pairId + "," + r[0] + "," + j[0] + "," + rTextEsc + "," + jTextEsc);
                pwHuman.println(pairId + "," + r[0] + "," + j[0] + "," + rTextEsc + "," + jTextEsc + ",,,,PENDING");
            }
        }

        pwUnlabeled.close();
        pwHuman.close();

        System.out.println("Generated " + pairCount + " candidate-job pairs in unlabeled_pairs.csv and human_annotation.csv");
    }

    private static String escapeCsv(String input) {
        if (input.contains(",") || input.contains("\"") || input.contains("\n")) {
            return "\"" + input.replace("\"", "\"\"") + "\"";
        }
        return input;
    }
}
