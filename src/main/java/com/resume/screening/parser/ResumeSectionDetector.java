package com.resume.screening.parser;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

@Component
public class ResumeSectionDetector {

    public enum SectionType {
        SUMMARY,
        EDUCATION,
        SKILLS,
        EXPERIENCE,
        PROJECTS,
        CERTIFICATIONS,
        ACHIEVEMENTS,
        LANGUAGES,
        UNKNOWN
    }

    private static final Map<SectionType, List<Pattern>> SECTION_PATTERNS = new LinkedHashMap<>();

    static {
        SECTION_PATTERNS.put(SectionType.SUMMARY, Arrays.asList(
                createHeaderPattern("SUMMARY"),
                createHeaderPattern("OBJECTIVE"),
                createHeaderPattern("PROFILE"),
                createHeaderPattern("PROFESSIONAL SUMMARY"),
                createHeaderPattern("CAREER SUMMARY"),
                createHeaderPattern("ABOUT ME"),
                createHeaderPattern("EXECUTIVE SUMMARY"),
                createHeaderPattern("PERSONAL STATEMENT")
        ));

        SECTION_PATTERNS.put(SectionType.EDUCATION, Arrays.asList(
                createHeaderPattern("EDUCATION"),
                createHeaderPattern("ACADEMIC QUALIFICATION"),
                createHeaderPattern("ACADEMIC QUALIFICATIONS"),
                createHeaderPattern("EDUCATIONAL BACKGROUND"),
                createHeaderPattern("QUALIFICATIONS"),
                createHeaderPattern("ACADEMIC BACKGROUND"),
                createHeaderPattern("EDUCATION & TRAINING"),
                createHeaderPattern("ACADEMIC CREDENTIALS")
        ));

        SECTION_PATTERNS.put(SectionType.SKILLS, Arrays.asList(
                createHeaderPattern("SKILLS"),
                createHeaderPattern("TECHNICAL SKILLS"),
                createHeaderPattern("CORE COMPETENCIES"),
                createHeaderPattern("KEY SKILLS"),
                createHeaderPattern("TECHNOLOGIES"),
                createHeaderPattern("TECHNICAL PROFICIENCY"),
                createHeaderPattern("PROFESSIONAL SKILLS"),
                createHeaderPattern("TECHNICAL EXPERTISE"),
                createHeaderPattern("AREAS OF EXPERTISE")
        ));

        SECTION_PATTERNS.put(SectionType.EXPERIENCE, Arrays.asList(
                createHeaderPattern("EXPERIENCE"),
                createHeaderPattern("WORK EXPERIENCE"),
                createHeaderPattern("INTERNSHIP"),
                createHeaderPattern("INTERNSHIPS"),
                createHeaderPattern("WORK HISTORY"),
                createHeaderPattern("PROFESSIONAL EXPERIENCE"),
                createHeaderPattern("EMPLOYMENT HISTORY"),
                createHeaderPattern("PRACTICAL EXPERIENCE"),
                createHeaderPattern("INDUSTRY EXPERIENCE")
        ));

        SECTION_PATTERNS.put(SectionType.PROJECTS, Arrays.asList(
                createHeaderPattern("PROJECTS"),
                createHeaderPattern("ACADEMIC PROJECTS"),
                createHeaderPattern("PERSONAL PROJECTS"),
                createHeaderPattern("KEY PROJECTS"),
                createHeaderPattern("MAJOR PROJECTS"),
                createHeaderPattern("SELECTED PROJECTS")
        ));

        SECTION_PATTERNS.put(SectionType.CERTIFICATIONS, Arrays.asList(
                createHeaderPattern("CERTIFICATIONS"),
                createHeaderPattern("CERTIFICATES"),
                createHeaderPattern("CERTIFICATIONS & LICENSES"),
                createHeaderPattern("PROFESSIONAL CERTIFICATIONS"),
                createHeaderPattern("COURSES"),
                createHeaderPattern("TRAINING & CERTIFICATIONS")
        ));

        SECTION_PATTERNS.put(SectionType.ACHIEVEMENTS, Arrays.asList(
                createHeaderPattern("ACHIEVEMENTS"),
                createHeaderPattern("ACCOMPLISHMENTS"),
                createHeaderPattern("HONORS & AWARDS"),
                createHeaderPattern("AWARDS"),
                createHeaderPattern("EXTRA-CURRICULAR ACTIVITIES"),
                createHeaderPattern("EXTRA CURRICULAR ACTIVITIES")
        ));

        SECTION_PATTERNS.put(SectionType.LANGUAGES, Arrays.asList(
                createHeaderPattern("LANGUAGES"),
                createHeaderPattern("LANGUAGES KNOWN"),
                createHeaderPattern("SPOKEN LANGUAGES")
        ));
    }

    private static Pattern createHeaderPattern(String headerName) {
        String escaped = Pattern.quote(headerName);
        // Header pattern: standalone line, optional trailing colon/dashes, case insensitive
        return Pattern.compile("^(?:[\\#\\*\\-\\•\\>\\s]*)" + escaped + "(?:[\\s\\:\\-\\=\\_\\|]*)$", Pattern.CASE_INSENSITIVE);
    }

    public Map<SectionType, String> parseSections(String cleanText) {
        Map<SectionType, StringBuilder> sectionMap = new EnumMap<>(SectionType.class);
        for (SectionType type : SectionType.values()) {
            if (type != SectionType.UNKNOWN) {
                sectionMap.put(type, new StringBuilder());
            }
        }

        if (cleanText == null || cleanText.trim().isEmpty()) {
            return convertMapToString(sectionMap);
        }

        String[] lines = cleanText.split("\n");
        SectionType currentSection = SectionType.SUMMARY; // default start section for header text

        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty()) continue;

            SectionType detected = detectHeader(trimmedLine);
            if (detected != SectionType.UNKNOWN) {
                currentSection = detected;
            } else {
                if (currentSection != SectionType.UNKNOWN) {
                    sectionMap.get(currentSection).append(line).append("\n");
                }
            }
        }

        return convertMapToString(sectionMap);
    }

    private SectionType detectHeader(String line) {
        if (line.length() > 60) { // Headers are usually short titles
            return SectionType.UNKNOWN;
        }

        for (Map.Entry<SectionType, List<Pattern>> entry : SECTION_PATTERNS.entrySet()) {
            for (Pattern pattern : entry.getValue()) {
                if (pattern.matcher(line).matches()) {
                    return entry.getKey();
                }
            }
        }

        return SectionType.UNKNOWN;
    }

    private Map<SectionType, String> convertMapToString(Map<SectionType, StringBuilder> sectionMap) {
        Map<SectionType, String> result = new EnumMap<>(SectionType.class);
        for (Map.Entry<SectionType, StringBuilder> entry : sectionMap.entrySet()) {
            String content = entry.getValue().toString().trim();
            if (!content.isEmpty()) {
                result.put(entry.getKey(), content);
            }
        }
        return result;
    }
}
