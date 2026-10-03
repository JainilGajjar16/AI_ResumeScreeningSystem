package com.resume.screening.parser;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.io.File;

import net.sourceforge.tess4j.Tesseract;

@Component
public class OcrService {

    private static final Logger logger = LoggerFactory.getLogger(OcrService.class);

    public String extractTextFromPdfUsingOcr(File pdfFile) {
        if (pdfFile == null || !pdfFile.exists() || pdfFile.length() == 0) {
            return "";
        }

        StringBuilder ocrResult = new StringBuilder();

        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            if (document.isEncrypted()) {
                return "";
            }

            PDFRenderer pdfRenderer = new PDFRenderer(document);
            int pageCount = Math.min(document.getNumberOfPages(), 3); // Process up to first 3 pages

            Tesseract tesseract = null;
            try {
                tesseract = new Tesseract();
                String tessDataPrefix = System.getenv("TESSDATA_PREFIX");
                if (tessDataPrefix != null && new File(tessDataPrefix).exists()) {
                    tesseract.setDatapath(tessDataPrefix);
                }
            } catch (Throwable t) {
                logger.warn("Tesseract OCR initialization warning: {}", t.getMessage());
                return "";
            }

            for (int i = 0; i < pageCount; i++) {
                try {
                    BufferedImage bImage = pdfRenderer.renderImageWithDPI(i, 200);
                    if (tesseract != null) {
                        String pageText = tesseract.doOCR(bImage);
                        if (pageText != null && !pageText.trim().isEmpty()) {
                            ocrResult.append(pageText).append("\n");
                        }
                    }
                } catch (Throwable t) {
                    logger.warn("OCR rendering or extraction failed for page {}: {}", i, t.getMessage());
                }
            }
        } catch (Throwable t) {
            logger.warn("PDFBox image rendering for OCR failed: {}", t.getMessage());
        }

        return ocrResult.toString().trim();
    }
}
