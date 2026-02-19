package com.hamza.performanceportal.performance.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

/**
 * Service for validating uploaded files
 */
@Service
@Slf4j
public class FileValidationService {

    // Architecture Diagram Validation
    private static final List<String> ARCHITECTURE_DIAGRAM_MIME_TYPES = Arrays.asList(
            "image/png",
            "image/jpeg",
            "image/jpg",
            "image/svg+xml"
    );
    private static final long ARCHITECTURE_DIAGRAM_MAX_SIZE = 10 * 1024 * 1024; // 10MB

    // Test Cases Summary Validation
    private static final List<String> TEST_CASES_MIME_TYPES = Arrays.asList(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", // .xlsx
            "application/vnd.ms-excel", // .xls
            "text/csv",
            "application/pdf"
    );
    private static final long TEST_CASES_MAX_SIZE = 50 * 1024 * 1024; // 50MB

    // Performance Data Validation
    private static final List<String> PERFORMANCE_DATA_MIME_TYPES = Arrays.asList(
            "text/plain" // .jtl
    );
    private static final long PERFORMANCE_DATA_MAX_SIZE = 100 * 1024 * 1024; // 100MB

    // JMeter Results Image Validation
    private static final List<String> JMETER_IMAGE_MIME_TYPES = Arrays.asList(
            "image/png",
            "image/jpeg",
            "image/jpg"
    );
    private static final long JMETER_IMAGE_MAX_SIZE = 20 * 1024 * 1024; // 20MB

    /**
     * Validate architecture diagram file
     */
    public ValidationResult validateArchitectureDiagram(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ValidationResult.error("Architecture diagram file is empty");
        }

        String contentType = file.getContentType();
        String fileName = file.getOriginalFilename();

        // Check file extension
        if (fileName != null) {
            String extension = getFileExtension(fileName).toLowerCase();
            if (!Arrays.asList("png", "jpg", "jpeg", "svg").contains(extension)) {
                return ValidationResult.error("Invalid file extension. Allowed: PNG, JPG, JPEG, SVG");
            }
        }

        // Check MIME type
        if (contentType == null || !ARCHITECTURE_DIAGRAM_MIME_TYPES.contains(contentType)) {
            return ValidationResult.error("Invalid file type. Allowed: PNG, JPG, SVG");
        }

        // Check file size
        if (file.getSize() > ARCHITECTURE_DIAGRAM_MAX_SIZE) {
            return ValidationResult.error("File size exceeds 10MB limit");
        }

        log.info("Architecture diagram validation passed: {}", fileName);
        return ValidationResult.success();
    }

    /**
     * Validate test cases summary file
     */
    public ValidationResult validateTestCasesSummary(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ValidationResult.error("Test cases summary file is empty");
        }

        String contentType = file.getContentType();
        String fileName = file.getOriginalFilename();

        // Check file extension
        if (fileName != null) {
            String extension = getFileExtension(fileName).toLowerCase();
            if (!Arrays.asList("xlsx", "xls", "csv", "pdf").contains(extension)) {
                return ValidationResult.error("Invalid file extension. Allowed: XLSX, XLS, CSV, PDF");
            }
        }

        // Check MIME type
        if (contentType == null || !TEST_CASES_MIME_TYPES.contains(contentType)) {
            return ValidationResult.error("Invalid file type. Allowed: Excel, CSV, PDF");
        }

        // Check file size
        if (file.getSize() > TEST_CASES_MAX_SIZE) {
            return ValidationResult.error("File size exceeds 50MB limit");
        }

        log.info("Test cases summary validation passed: {}", fileName);
        return ValidationResult.success();
    }

    /**
     * Validate performance data file
     */
    public ValidationResult validatePerformanceData(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ValidationResult.error("Performance data file is empty");
        }

        String contentType = file.getContentType();
        String fileName = file.getOriginalFilename();

        // Check file extension
        if (fileName != null) {
            String extension = getFileExtension(fileName).toLowerCase();
            if (!Arrays.asList("jtl").contains(extension)) {
                return ValidationResult.error("Invalid file extension. Allowed: JTL");
            }
        }

        // Check file size
        if (file.getSize() > PERFORMANCE_DATA_MAX_SIZE) {
            return ValidationResult.error("File size exceeds 100MB limit");
        }

        log.info("Performance data validation passed: {}", fileName);
        return ValidationResult.success();
    }

    /**
     * Validate JMeter results image file
     */
    public ValidationResult validateJMeterImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ValidationResult.error("JMeter results image file is empty");
        }

        String contentType = file.getContentType();
        String fileName = file.getOriginalFilename();

        // Check file extension
        if (fileName != null) {
            String extension = getFileExtension(fileName).toLowerCase();
            if (!Arrays.asList("png", "jpg", "jpeg").contains(extension)) {
                return ValidationResult.error("Invalid file extension. Allowed: PNG, JPG, JPEG");
            }
        }

        // Check MIME type
        if (contentType == null || !JMETER_IMAGE_MIME_TYPES.contains(contentType)) {
            return ValidationResult.error("Invalid file type. Allowed: PNG, JPG, JPEG");
        }

        // Check file size
        if (file.getSize() > JMETER_IMAGE_MAX_SIZE) {
            return ValidationResult.error("File size exceeds 20MB limit");
        }

        log.info("JMeter results image validation passed: {}", fileName);
        return ValidationResult.success();
    }

    /**
     * Get file extension from filename
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    /**
     * Validation result class
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult error(String message) {
            return new ValidationResult(false, message);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}

// Made with Bob