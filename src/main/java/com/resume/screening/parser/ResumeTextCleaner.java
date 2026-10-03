package com.resume.screening.parser;

import org.springframework.stereotype.Component;

@Component
public class ResumeTextCleaner {

    public String cleanText(String rawText) {
        if (rawText == null) {
            return "";
        }

        // 1. Normalize line endings (\r\n or \r -> \n)
        String cleaned = rawText.replaceAll("\r\n", "\n").replaceAll("\r", "\n");

        // 2. Replace non-standard whitespace / control chars (except newlines and tabs)
        cleaned = cleaned.replaceAll("[\\u00A0\\u2007\\u202F\\u200B]", " ");

        // 3. Trim line spaces
        String[] lines = cleaned.split("\n");
        StringBuilder sb = new StringBuilder();
        int emptyLineCount = 0;

        for (String line : lines) {
            String trimmedLine = line.trim().replaceAll("[ \\t]+", " ");
            if (trimmedLine.isEmpty()) {
                emptyLineCount++;
                if (emptyLineCount <= 1) {
                    sb.append("\n");
                }
            } else {
                emptyLineCount = 0;
                sb.append(trimmedLine).append("\n");
            }
        }

        return sb.toString().trim();
    }
}
