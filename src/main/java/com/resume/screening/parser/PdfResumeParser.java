package com.resume.screening.parser;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Component
public class PdfResumeParser {

    private static final Logger logger = LoggerFactory.getLogger(PdfResumeParser.class);

    private final OcrService ocrService;

    public PdfResumeParser(OcrService ocrService) {
        this.ocrService = ocrService;
    }

    public static class PdfParseResult {
        private final String text;
        private final boolean ocrUsed;

        public PdfParseResult(String text, boolean ocrUsed) {
            this.text = text;
            this.ocrUsed = ocrUsed;
        }

        public String getText() {
            return text;
        }

        public boolean isOcrUsed() {
            return ocrUsed;
        }
    }

    public String extractText(File pdfFile) {
        PdfParseResult result = extractTextWithResult(pdfFile);
        return result.getText();
    }

    public PdfParseResult extractTextWithResult(File pdfFile) {
        if (pdfFile == null || !pdfFile.exists()) {
            throw new IllegalArgumentException("PDF file does not exist.");
        }
        if (pdfFile.length() == 0) {
            throw new IllegalArgumentException("PDF file is empty (0 bytes).");
        }

        String pdfBoxText = "";
        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            if (document.isEncrypted()) {
                throw new IllegalArgumentException("PDF file is password-protected and cannot be parsed.");
            }

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            pdfBoxText = stripper.getText(document);

        } catch (InvalidPasswordException ex) {
            logger.error("Password protected PDF: {}", pdfFile.getName(), ex);
            throw new IllegalArgumentException("PDF file is password-protected.");
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (IOException ex) {
            logger.error("Failed to read PDF file: {}", pdfFile.getName(), ex);
        } catch (Exception ex) {
            logger.error("Unexpected error parsing PDF file: {}", pdfFile.getName(), ex);
        }

        // 1. Check if PDFBox extracted readable text
        if (pdfBoxText != null && isReadableText(pdfBoxText)) {
            return new PdfParseResult(pdfBoxText.trim(), false);
        }

        // 2. Fallback: OCR for scanned / image PDF
        logger.info("PDFBox extracted empty or unusable text for {}. Attempting OCR fallback...", pdfFile.getName());
        String ocrText = "";
        try {
            ocrText = ocrService.extractTextFromPdfUsingOcr(pdfFile);
        } catch (Exception ex) {
            logger.warn("OCR fallback exception: {}", ex.getMessage());
        }

        if (ocrText != null && isReadableText(ocrText)) {
            logger.info("OCR successfully extracted readable text for {}", pdfFile.getName());
            return new PdfParseResult(ocrText.trim(), true);
        }

        // 3. Both PDFBox and OCR failed
        logger.warn("Both PDFBox and OCR failed to extract readable text for {}", pdfFile.getName());
        throw new IllegalArgumentException("Unable to extract readable text from this PDF. Please upload a text-based PDF or DOCX resume.");
    }

    private boolean isReadableText(String text) {
        if (text == null) return false;
        String trimmed = text.trim();
        if (trimmed.length() < 10) return false;
        long letterOrDigitCount = trimmed.chars().filter(Character::isLetterOrDigit).count();
        return letterOrDigitCount >= 5;
    }
}
