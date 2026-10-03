package com.resume.screening.parser;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

@Component
public class DocxResumeParser {

    private static final Logger logger = LoggerFactory.getLogger(DocxResumeParser.class);

    public String extractText(File docxFile) {
        if (docxFile == null || !docxFile.exists()) {
            throw new IllegalArgumentException("DOCX file does not exist.");
        }
        if (docxFile.length() == 0) {
            throw new IllegalArgumentException("DOCX file is empty (0 bytes).");
        }

        try (FileInputStream fis = new FileInputStream(docxFile);
             OPCPackage opcPackage = OPCPackage.open(fis);
             XWPFDocument document = new XWPFDocument(opcPackage);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {

            String text = extractor.getText();
            if (text == null || text.trim().isEmpty()) {
                throw new IllegalArgumentException("DOCX document contains no readable text.");
            }

            return text;
        } catch (InvalidFormatException ex) {
            logger.error("Invalid DOCX format: {}", docxFile.getName(), ex);
            throw new IllegalArgumentException("Corrupt or invalid DOCX document format.");
        } catch (IOException ex) {
            logger.error("Failed to read DOCX file: {}", docxFile.getName(), ex);
            throw new IllegalArgumentException("Corrupt or unreadable DOCX document: " + ex.getMessage());
        } catch (Exception ex) {
            logger.error("Unexpected error parsing DOCX file: {}", docxFile.getName(), ex);
            throw new IllegalArgumentException("Failed to extract text from DOCX: " + ex.getMessage());
        }
    }
}
