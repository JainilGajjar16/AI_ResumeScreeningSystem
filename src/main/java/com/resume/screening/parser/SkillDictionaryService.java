package com.resume.screening.parser;

import com.resume.screening.entity.Skill;
import com.resume.screening.repository.SkillRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class SkillDictionaryService {

    private final SkillRepository skillRepository;

    // Standard pre-defined skill list for fallbacks and initial dictionary
    private static final List<String> DEFAULT_SKILLS = Arrays.asList(
            "Java", "Python", "JavaScript", "TypeScript", "HTML", "HTML5", "CSS", "CSS3",
            "Bootstrap", "Spring Boot", "Spring Framework", "Spring", "MySQL", "SQL",
            "MongoDB", "Git", "GitHub", "GitLab", "React", "React.js", "Angular",
            "Flutter", "Dart", "PHP", "C", "C++", "C#", ".NET", "ASP.NET", "REST API",
            "RESTful API", "AWS", "Docker", "Linux", "Unix", "Machine Learning",
            "Data Science", "Deep Learning", "Node.js", "Express.js", "PostgreSQL",
            "Redis", "Kubernetes", "DevOps", "CI/CD", "Microservices", "Hibernate",
            "JPA", "Android", "iOS", "Vue.js", "Tailwind CSS", "Next.js", "GraphQL",
            "PyTorch", "TensorFlow", "Scikit-learn", "OpenCV", "Tableau", "Power BI",
            "Hadoop", "Spark", "Kafka", "Selenium", "JUnit", "Postman", "Figma",
            "Swift", "Kotlin", "Go", "Golang", "Rust", "R", "Scala", "Bash", "Shell",
            "Jenkins", "Jira", "Maven", "Gradle"
    );

    public SkillDictionaryService(SkillRepository skillRepository) {
        this.skillRepository = skillRepository;
    }

    public List<String> getCanonicalSkills() {
        Set<String> uniqueSkills = new LinkedHashSet<>(DEFAULT_SKILLS);

        try {
            List<Skill> dbSkills = skillRepository.findAll();
            for (Skill skill : dbSkills) {
                if (skill.getName() != null && !skill.getName().trim().isEmpty()) {
                    uniqueSkills.add(skill.getName().trim());
                }
            }
        } catch (Exception ignored) {}

        return new ArrayList<>(uniqueSkills);
    }

    public List<String> extractSkills(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<String> canonicalSkills = getCanonicalSkills();
        Set<String> matchedSkills = new LinkedHashSet<>();

        // Sort skill names by length descending so longer skill names match before substrings
        // (e.g., "Spring Boot" before "Spring", "JavaScript" before "Java", "C++" before "C")
        List<String> sortedSkills = canonicalSkills.stream()
                .sorted((s1, s2) -> Integer.compare(s2.length(), s1.length()))
                .collect(Collectors.toList());

        String lowerText = text.toLowerCase();

        for (String skillName : sortedSkills) {
            if (containsSkill(text, lowerText, skillName)) {
                matchedSkills.add(normalizeSkillName(skillName));
            }
        }

        return new ArrayList<>(matchedSkills);
    }

    private boolean containsSkill(String originalText, String lowerText, String skillName) {
        String lowerSkill = skillName.toLowerCase();

        // Special handling for short symbols like C, C++, C#, .NET, R
        if (skillName.equalsIgnoreCase("C")) {
            Pattern pattern = Pattern.compile("(^|[^a-zA-Z0-9#+])C([^a-zA-Z0-9#+]|$)", Pattern.CASE_INSENSITIVE);
            return pattern.matcher(originalText).find();
        } else if (skillName.equalsIgnoreCase("C++")) {
            return originalText.contains("C++") || originalText.contains("c++") || lowerText.contains("cpp");
        } else if (skillName.equalsIgnoreCase("C#")) {
            return originalText.contains("C#") || originalText.contains("c#") || lowerText.contains("c-sharp");
        } else if (skillName.equalsIgnoreCase(".NET")) {
            return lowerText.contains(".net") || lowerText.contains("dotnet");
        } else if (skillName.equalsIgnoreCase("R")) {
            Pattern pattern = Pattern.compile("(^|[^a-zA-Z0-9#+])R([^a-zA-Z0-9#+]|$)", Pattern.CASE_INSENSITIVE);
            return pattern.matcher(originalText).find();
        }

        // Standard regex boundary matching
        String escaped = Pattern.quote(lowerSkill);
        Pattern pattern = Pattern.compile("(^|[^a-zA-Z0-9])" + escaped + "([^a-zA-Z0-9]|$)", Pattern.CASE_INSENSITIVE);
        return pattern.matcher(lowerText).find();
    }

    private String normalizeSkillName(String name) {
        if (name == null || name.trim().isEmpty()) return "";

        // Standardize common variants
        String trimmed = name.trim();
        if (trimmed.equalsIgnoreCase("javascript") || trimmed.equalsIgnoreCase("js")) return "JavaScript";
        if (trimmed.equalsIgnoreCase("typescript") || trimmed.equalsIgnoreCase("ts")) return "TypeScript";
        if (trimmed.equalsIgnoreCase("html5")) return "HTML";
        if (trimmed.equalsIgnoreCase("css3")) return "CSS";
        if (trimmed.equalsIgnoreCase("react.js")) return "React";
        if (trimmed.equalsIgnoreCase("node.js")) return "Node.js";
        if (trimmed.equalsIgnoreCase("express.js")) return "Express";
        if (trimmed.equalsIgnoreCase("vue.js")) return "Vue.js";
        if (trimmed.equalsIgnoreCase("spring framework")) return "Spring";
        if (trimmed.equalsIgnoreCase("golang")) return "Go";

        // Capitalize nicely if lowercase
        return trimmed;
    }
}
