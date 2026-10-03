package com.resume.screening.engine.impl;

import com.resume.screening.dto.AnalysisResult;
import com.resume.screening.entity.ParsedJobRequirement;
import com.resume.screening.entity.ParsedResume;
import com.resume.screening.entity.StudentProfile;
import com.resume.screening.engine.ResumeAnalysisEngine;
import com.resume.screening.parser.SkillDictionaryService;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RuleBasedResumeAnalysisEngine implements ResumeAnalysisEngine {

    private final SkillDictionaryService skillDictionaryService;

    // Configurable baseline weights (Total = 100%)
    private double skillsWeight = 50.0;
    private double experienceWeight = 20.0;
    private double qualificationWeight = 15.0;
    private double completenessWeight = 10.0;
    private double keywordWeight = 5.0;

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "the", "and", "for", "with", "from", "this", "that", "to", "in", "on", "at", "by",
            "is", "are", "be", "or", "an", "a", "of", "as", "it", "its", "your", "we", "our",
            "you", "will", "have", "has", "had", "not", "but", "all", "can", "should", "must"
    ));

    public RuleBasedResumeAnalysisEngine(SkillDictionaryService skillDictionaryService) {
        this.skillDictionaryService = skillDictionaryService;
    }

    public SkillDictionaryService getSkillDictionaryService() {
        return skillDictionaryService;
    }

    // Configurable weight getters and setters
    public double getSkillsWeight() { return skillsWeight; }
    public void setSkillsWeight(double skillsWeight) { this.skillsWeight = skillsWeight; }
    public double getExperienceWeight() { return experienceWeight; }
    public void setExperienceWeight(double experienceWeight) { this.experienceWeight = experienceWeight; }
    public double getQualificationWeight() { return qualificationWeight; }
    public void setQualificationWeight(double qualificationWeight) { this.qualificationWeight = qualificationWeight; }
    public double getCompletenessWeight() { return completenessWeight; }
    public void setCompletenessWeight(double completenessWeight) { this.completenessWeight = completenessWeight; }
    public double getKeywordWeight() { return keywordWeight; }
    public void setKeywordWeight(double keywordWeight) { this.keywordWeight = keywordWeight; }

    @Override
    public AnalysisResult analyze(ParsedResume parsedResume, ParsedJobRequirement parsedJobRequirement, StudentProfile studentProfile) {
        if (parsedResume == null || parsedJobRequirement == null) {
            throw new IllegalArgumentException("Parsed resume and parsed job requirement must not be null.");
        }

        // 1. Skill Matching
        List<String> reqSkills = parseSkillList(parsedJobRequirement.getRequiredSkills());
        List<String> candSkills = parseSkillList(parsedResume.getSkills());

        // Fallback: If candidate skills are empty, extract using SkillDictionaryService
        if (candSkills.isEmpty() && parsedResume.getRawText() != null && skillDictionaryService != null) {
            candSkills = skillDictionaryService.extractSkills(parsedResume.getRawText());
        }

        Set<String> matchedSkillsSet = new LinkedHashSet<>();
        Set<String> missingSkillsSet = new LinkedHashSet<>();

        String fullResumeText = (parsedResume.getRawText() != null ? parsedResume.getRawText() : "") + " " +
                (parsedResume.getSkills() != null ? parsedResume.getSkills() : "");

        for (String reqSkill : reqSkills) {
            if (isSkillPresent(reqSkill, candSkills, fullResumeText)) {
                matchedSkillsSet.add(reqSkill);
            } else {
                missingSkillsSet.add(reqSkill);
            }
        }

        List<String> matchedSkills = new ArrayList<>(matchedSkillsSet);
        List<String> missingSkills = new ArrayList<>(missingSkillsSet);

        double skillMatchPct = reqSkills.isEmpty() ? 100.0 :
                roundDouble(((double) matchedSkills.size() / reqSkills.size()) * 100.0);
        double skillsMatchScoreContribution = roundDouble((skillMatchPct / 100.0) * skillsWeight);

        // 2. Qualification Matching
        String qualMatchResult = evaluateQualification(parsedResume.getEducation(), studentProfile, parsedJobRequirement.getQualification());
        double qualRatio = getMatchRatio(qualMatchResult);
        double qualScoreContribution = roundDouble(qualRatio * qualificationWeight);

        // 3. Experience Matching
        String expMatchResult = evaluateExperience(parsedResume.getExperience(), parsedJobRequirement.getExperience());
        double expRatio = getMatchRatio(expMatchResult);
        double expScoreContribution = roundDouble(expRatio * experienceWeight);

        // 4. Resume Completeness
        double completenessPct = evaluateCompleteness(parsedResume, studentProfile);
        double completenessScoreContribution = roundDouble((completenessPct / 100.0) * completenessWeight);

        // 5. Keyword Matching
        double keywordPct = evaluateKeywordMatch(parsedResume, parsedJobRequirement);
        double keywordScoreContribution = roundDouble((keywordPct / 100.0) * keywordWeight);

        // 6. Total ATS Score Calculation (Bounded 0 to 100)
        double rawTotal = skillsMatchScoreContribution + qualScoreContribution + expScoreContribution +
                completenessScoreContribution + keywordScoreContribution;
        double totalAtsScore = roundDouble(Math.min(100.0, Math.max(0.0, rawTotal)));

        // 7. Strengths & Suggestions
        List<String> strengths = generateStrengths(parsedResume, parsedJobRequirement, studentProfile,
                matchedSkills, qualMatchResult, expMatchResult, completenessPct);
        List<String> suggestions = generateSuggestions(missingSkills, qualMatchResult, expMatchResult,
                completenessPct, parsedResume, studentProfile);

        return AnalysisResult.builder()
                .atsScore(totalAtsScore)
                .skillMatchPercentage(skillMatchPct)
                .matchedSkills(matchedSkills)
                .missingSkills(missingSkills)
                .qualificationMatch(qualMatchResult)
                .experienceMatch(expMatchResult)
                .skillsMatchScore(skillsMatchScoreContribution)
                .experienceScore(expScoreContribution)
                .qualificationScore(qualScoreContribution)
                .resumeCompletenessScore(completenessScoreContribution)
                .keywordMatchScore(keywordScoreContribution)
                .strengths(strengths)
                .suggestions(suggestions)
                .build();
    }

    private List<String> parseSkillList(String skillsText) {
        if (skillsText == null || skillsText.trim().isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> list = new LinkedHashSet<>();
        String[] tokens = skillsText.split("[,;\\n]");
        for (String t : tokens) {
            String trimmed = t.trim();
            if (!trimmed.isEmpty()) {
                list.add(trimmed);
            }
        }
        return new ArrayList<>(list);
    }

    private boolean isSkillPresent(String targetSkill, List<String> candidateSkills, String fullText) {
        String normTarget = targetSkill.trim().toLowerCase();

        // 1. Direct case-insensitive match against candidate skill list
        for (String cs : candidateSkills) {
            if (cs.trim().equalsIgnoreCase(targetSkill.trim())) {
                return true;
            }
            // Handle variations like "Spring Boot" vs "SpringBoot", "Node.js" vs "NodeJS"
            String normCs = cs.trim().toLowerCase().replaceAll("[^a-z0-9]", "");
            String normTg = normTarget.replaceAll("[^a-z0-9]", "");
            if (!normTg.isEmpty() && normTg.equalsIgnoreCase(normCs)) {
                return true;
            }
        }

        // 2. Strict boundary regex check against full resume text to avoid false sub-string matches (e.g., Java vs JavaScript)
        if (targetSkill.equalsIgnoreCase("Java")) {
            Pattern p = Pattern.compile("(^|[^a-zA-Z0-9#+])Java([^a-zA-Z0-9#+script]|$)", Pattern.CASE_INSENSITIVE);
            return p.matcher(fullText).find();
        } else if (targetSkill.equalsIgnoreCase("C")) {
            Pattern p = Pattern.compile("(^|[^a-zA-Z0-9#+])C([^a-zA-Z0-9#+]|$)", Pattern.CASE_INSENSITIVE);
            return p.matcher(fullText).find();
        }

        String escaped = Pattern.quote(normTarget);
        Pattern pattern = Pattern.compile("(^|[^a-zA-Z0-9#+])" + escaped + "([^a-zA-Z0-9#+]|$)", Pattern.CASE_INSENSITIVE);
        return pattern.matcher(fullText.toLowerCase()).find();
    }

    private String evaluateQualification(String candidateEducationText, StudentProfile studentProfile, String jobQualText) {
        if (jobQualText == null || jobQualText.trim().isEmpty() || "Not specified".equalsIgnoreCase(jobQualText.trim())) {
            return "NOT_AVAILABLE";
        }

        String combinedCandEdu = (candidateEducationText != null ? candidateEducationText : "") + " " +
                (studentProfile != null && studentProfile.getDegree() != null ? studentProfile.getDegree() : "") + " " +
                (studentProfile != null && studentProfile.getBranch() != null ? studentProfile.getBranch() : "");

        if (combinedCandEdu.trim().isEmpty()) {
            return "NOT_AVAILABLE";
        }

        int reqLevel = getDegreeLevel(jobQualText);
        int candLevel = getDegreeLevel(combinedCandEdu);

        if (candLevel > 0 && reqLevel > 0) {
            if (candLevel >= reqLevel) {
                return "MATCH";
            } else if (candLevel == reqLevel - 1) {
                return "PARTIAL_MATCH";
            } else {
                return "NO_MATCH";
            }
        }

        // Keyword fallback check
        String lowerJobQual = jobQualText.toLowerCase();
        String lowerCandEdu = combinedCandEdu.toLowerCase();

        if (lowerJobQual.contains("bachelor") || lowerJobQual.contains("b.tech") || lowerJobQual.contains("degree") || lowerJobQual.contains("bca")) {
            if (lowerCandEdu.contains("bachelor") || lowerCandEdu.contains("b.tech") || lowerCandEdu.contains("bca") || lowerCandEdu.contains("b.e") || lowerCandEdu.contains("bs")) {
                return "MATCH";
            }
        }

        if (lowerCandEdu.contains(lowerJobQual) || lowerJobQual.contains(lowerCandEdu)) {
            return "MATCH";
        }

        return "PARTIAL_MATCH";
    }

    private int getDegreeLevel(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("phd") || lower.contains("doctorate")) return 4;
        if (lower.contains("master") || lower.contains("m.tech") || lower.contains("mtech") || lower.contains("m.s") || lower.contains("mba") || lower.contains("mca")) return 3;
        if (lower.contains("bachelor") || lower.contains("b.tech") || lower.contains("btech") || lower.contains("b.e") || lower.contains("bca") || lower.contains("b.sc") || lower.contains("bs")) return 2;
        if (lower.contains("diploma") || lower.contains("12th") || lower.contains("high school")) return 1;
        return 0;
    }

    private String evaluateExperience(String candidateExpText, String jobExpText) {
        if (jobExpText == null || jobExpText.trim().isEmpty() || "Not specified".equalsIgnoreCase(jobExpText.trim())) {
            return "NOT_AVAILABLE";
        }

        int reqYears = extractYears(jobExpText);
        boolean candExpBlank = (candidateExpText == null || candidateExpText.trim().isEmpty());

        if (candExpBlank) {
            if (reqYears == 0) {
                return "MATCH";
            }
            return "NOT_AVAILABLE";
        }

        int candYears = extractYears(candidateExpText);

        if (reqYears == 0) {
            return "MATCH";
        }

        if (candYears >= reqYears) {
            return "MATCH";
        } else if (candYears > 0 || candidateExpText.toLowerCase().contains("intern") || candidateExpText.toLowerCase().contains("project")) {
            return "PARTIAL_MATCH";
        } else {
            return "NO_MATCH";
        }
    }

    private int extractYears(String text) {
        if (text == null) return 0;
        Pattern p = Pattern.compile("(\\d+)\\s*(?:\\+)?\\s*(?:year|yr)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ignored) {}
        }
        return 0;
    }

    private double evaluateCompleteness(ParsedResume parsedResume, StudentProfile studentProfile) {
        int points = 0;

        if (parsedResume.getSummary() != null && !parsedResume.getSummary().trim().isEmpty()) points += 15;
        if (parsedResume.getSkills() != null && !parsedResume.getSkills().trim().isEmpty()) points += 25;
        if (parsedResume.getEducation() != null && !parsedResume.getEducation().trim().isEmpty()) points += 20;
        if (parsedResume.getExperience() != null && !parsedResume.getExperience().trim().isEmpty()) points += 15;
        if (parsedResume.getProjects() != null && !parsedResume.getProjects().trim().isEmpty()) points += 15;
        if (parsedResume.getCertifications() != null && !parsedResume.getCertifications().trim().isEmpty()) points += 10;

        return Math.min(100.0, points);
    }

    private double evaluateKeywordMatch(ParsedResume parsedResume, ParsedJobRequirement parsedJobRequirement) {
        String jobKeywords = parsedJobRequirement.getKeywords();
        if (jobKeywords == null || jobKeywords.trim().isEmpty()) {
            return 80.0; // Default high baseline if job keywords are unspecified
        }

        String[] keywords = jobKeywords.split("[,;\\s]+");
        Set<String> cleanJobKeywords = new HashSet<>();
        for (String kw : keywords) {
            String clean = kw.trim().toLowerCase().replaceAll("[^a-z0-9]", "");
            if (clean.length() > 2 && !STOP_WORDS.contains(clean)) {
                cleanJobKeywords.add(clean);
            }
        }

        if (cleanJobKeywords.isEmpty()) {
            return 80.0;
        }

        String fullText = (parsedResume.getRawText() != null ? parsedResume.getRawText() : "").toLowerCase();
        int matchedCount = 0;
        for (String kw : cleanJobKeywords) {
            if (fullText.contains(kw)) {
                matchedCount++;
            }
        }

        return roundDouble(((double) matchedCount / cleanJobKeywords.size()) * 100.0);
    }

    private double getMatchRatio(String matchStatus) {
        if ("MATCH".equalsIgnoreCase(matchStatus)) return 1.0;
        if ("PARTIAL_MATCH".equalsIgnoreCase(matchStatus)) return 0.5;
        if ("NOT_AVAILABLE".equalsIgnoreCase(matchStatus)) return 0.5; // neutral non-punitive score
        return 0.0; // NO_MATCH
    }

    private List<String> generateStrengths(ParsedResume parsedResume, ParsedJobRequirement parsedJobReq,
                                           StudentProfile studentProfile, List<String> matchedSkills,
                                           String qualMatch, String expMatch, double completenessPct) {
        List<String> strengths = new ArrayList<>();

        if (!matchedSkills.isEmpty()) {
            strengths.add("Strong skill alignment with " + matchedSkills.size() + " key technical skills matched (" + String.join(", ", matchedSkills.subList(0, Math.min(4, matchedSkills.size()))) + ").");
        }
        if ("MATCH".equals(qualMatch)) {
            strengths.add("Required educational qualification criteria met.");
        }
        if ("MATCH".equals(expMatch)) {
            strengths.add("Candidate's work experience meets or exceeds job requirements.");
        }
        if (parsedResume.getProjects() != null && !parsedResume.getProjects().trim().isEmpty()) {
            strengths.add("Resume includes documented technical projects.");
        }
        if (studentProfile != null && studentProfile.getGithubUrl() != null && !studentProfile.getGithubUrl().trim().isEmpty()) {
            strengths.add("GitHub profile available for technical repository review.");
        }
        if (studentProfile != null && studentProfile.getLinkedinUrl() != null && !studentProfile.getLinkedinUrl().trim().isEmpty()) {
            strengths.add("LinkedIn profile available for professional verification.");
        }
        if (completenessPct >= 80.0) {
            strengths.add("Well-structured resume with high overall completeness.");
        }

        if (strengths.isEmpty()) {
            strengths.add("Basic resume structure is present.");
        }

        return strengths;
    }

    private List<String> generateSuggestions(List<String> missingSkills, String qualMatch, String expMatch,
                                             double completenessPct, ParsedResume parsedResume, StudentProfile studentProfile) {
        List<String> suggestions = new ArrayList<>();

        if (!missingSkills.isEmpty()) {
            String missingListStr = String.join(", ", missingSkills.subList(0, Math.min(3, missingSkills.size())));
            suggestions.add("Add missing skills (" + missingListStr + ") to your resume if you genuinely have experience with them.");
        }
        if (parsedResume.getProjects() == null || parsedResume.getProjects().trim().isEmpty()) {
            suggestions.add("Include relevant technical projects showcasing practical application of your core skills.");
        } else {
            suggestions.add("Add measurable outcomes or metrics to your project descriptions.");
        }
        if (parsedResume.getSummary() == null || parsedResume.getSummary().trim().isEmpty()) {
            suggestions.add("Add a concise professional summary tailored to target job roles.");
        }
        if (parsedResume.getCertifications() == null || parsedResume.getCertifications().trim().isEmpty()) {
            suggestions.add("Consider obtaining and listing industry-recognized certifications to validate your skill set.");
        }
        if (studentProfile != null && (studentProfile.getGithubUrl() == null || studentProfile.getGithubUrl().trim().isEmpty())) {
            suggestions.add("Add a GitHub profile link to highlight your code portfolio.");
        }

        return suggestions;
    }

    private double roundDouble(double val) {
        return Math.round(val * 10.0) / 10.0;
    }
}
