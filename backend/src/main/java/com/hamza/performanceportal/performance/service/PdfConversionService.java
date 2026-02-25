package com.hamza.performanceportal.performance.service;

import lombok.extern.slf4j.Slf4j;
import org.docx4j.Docx4J;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.convert.out.FOSettings;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * Service for converting Word documents to PDF using Docx4j and Apache FOP
 */
@Service
@Slf4j
public class PdfConversionService {

    /**
     * Convert a DOCX file to PDF
     *
     * @param docxFilePath Path to the input DOCX file
     * @param pdfFilePath Path where the PDF should be saved
     * @return true if conversion successful, false otherwise
     */
    public boolean convertDocxToPdf(String docxFilePath, String pdfFilePath) {
        log.info("Starting DOCX to PDF conversion: {} -> {}", docxFilePath, pdfFilePath);

        try {
            // Load the DOCX file
            File docxFile = new File(docxFilePath);
            if (!docxFile.exists()) {
                log.error("DOCX file not found: {}", docxFilePath);
                return false;
            }

            log.info("Loading DOCX file: {} (size: {} bytes)", docxFilePath, docxFile.length());
            WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.load(docxFile);

            // Set output file
            File pdfFile = new File(pdfFilePath);

            // Ensure parent directory exists
            if (pdfFile.getParentFile() != null && !pdfFile.getParentFile().exists()) {
                pdfFile.getParentFile().mkdirs();
            }

            // Convert to PDF using Docx4J.toPDF which handles FOP setup internally
            log.info("Converting to PDF...");
            try (OutputStream os = new FileOutputStream(pdfFile)) {
                Docx4J.toPDF(wordMLPackage, os);
            }

            long pdfSize = pdfFile.length();
            log.info("PDF conversion successful: {} (size: {} bytes)", pdfFilePath, pdfSize);

            return true;

        } catch (Exception e) {
            log.error("Failed to convert DOCX to PDF: {}", e.getMessage(), e);
            return false;
        }
    }


    /**
     * Generate PDF filename from DOCX filename
     *
     * @param docxFilePath Path to DOCX file
     * @return Path to PDF file (same directory, .pdf extension)
     */
    public String generatePdfFilePath(String docxFilePath) {
        if (docxFilePath == null || !docxFilePath.toLowerCase().endsWith(".docx")) {
            throw new IllegalArgumentException("Invalid DOCX file path: " + docxFilePath);
        }
        return docxFilePath.substring(0, docxFilePath.length() - 5) + ".pdf";
    }
}
