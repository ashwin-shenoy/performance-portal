package com.hamza.performanceportal.performance.controller;

import com.hamza.performanceportal.performance.service.WordReportGenerationService;
import com.hamza.performanceportal.performance.entity.Report;
import com.hamza.performanceportal.performance.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST Controller for report generation and management
 */
@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {
    "http://localhost:3000",
    "http://localhost:5173"
}, allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class ReportController {

    private final ReportRepository reportRepository;
    private final WordReportGenerationService wordReportGenerationService;

    /**
     * Download generated report
     * 
     * @param reportId ID of the report
     * @return Report file as downloadable resource
     */
    @GetMapping("/download/{reportId}")
    public ResponseEntity<Resource> downloadReport(@PathVariable Long reportId) {
        log.info("Downloading report ID: {}", reportId);
        
        try {
            Report report = reportRepository.findById(reportId)
                    .orElseThrow(() -> new RuntimeException("Report not found: " + reportId));
            
            File file = new File(report.getFilePath());
            if (!file.exists()) {
                throw new RuntimeException("Report file not found: " + report.getFilePath());
            }
            
            Resource resource = new FileSystemResource(file);
            
            // Determine content type based on file extension
            MediaType contentType = determineContentType(report.getFileName());
            
            return ResponseEntity.ok()
                    .contentType(contentType)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + report.getFileName() + "\"")
                    .body(resource);
            
        } catch (Exception e) {
            log.error("Failed to download report {}: {}", reportId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    
    /**
     * Determine the appropriate content type based on file extension
     *
     * @param fileName Name of the file
     * @return MediaType for the file
     */
    private MediaType determineContentType(String fileName) {
        if (fileName == null) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        
        String lowerFileName = fileName.toLowerCase();
        
        if (lowerFileName.endsWith(".docx")) {
            return MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        } else if (lowerFileName.endsWith(".doc")) {
            return MediaType.parseMediaType("application/msword");
        } else if (lowerFileName.endsWith(".pdf")) {
            return MediaType.APPLICATION_PDF;
        } else if (lowerFileName.endsWith(".xlsx")) {
            return MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        } else if (lowerFileName.endsWith(".xls")) {
            return MediaType.parseMediaType("application/vnd.ms-excel");
        } else if (lowerFileName.endsWith(".csv")) {
            return MediaType.parseMediaType("text/csv");
        } else if (lowerFileName.endsWith(".html")) {
            return MediaType.TEXT_HTML;
        } else {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    /**
     * Get report generation status
     * 
     * @param testRunId ID of the test run
     * @return Report status information
     */
    @GetMapping("/status/{testRunId}")
    public ResponseEntity<Map<String, Object>> getReportStatus(@PathVariable Long testRunId) {
        log.info("Checking report status for test run ID: {}", testRunId);
        
        try {
            List<Report> reports = reportRepository.findByTestRunId(testRunId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("testRunId", testRunId);
            response.put("hasReport", !reports.isEmpty());
            response.put("reportCount", reports.size());
            
            if (!reports.isEmpty()) {
                Report latestReport = reports.get(reports.size() - 1);
                response.put("latestReport", Map.of(
                    "reportId", latestReport.getId(),
                    "fileName", latestReport.getFileName(),
                    "generatedAt", latestReport.getGeneratedAt(),
                    "downloadUrl", "/api/v1/reports/download/" + latestReport.getId()
                ));
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to check report status for test run {}: {}", testRunId, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to check report status: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get report preview/metadata
     * 
     * @param reportId ID of the report
     * @return Report metadata
     */
    @GetMapping("/{reportId}")
    public ResponseEntity<Map<String, Object>> getReportMetadata(@PathVariable Long reportId) {
        log.info("Getting metadata for report ID: {}", reportId);
        
        try {
            Report report = reportRepository.findById(reportId)
                    .orElseThrow(() -> new RuntimeException("Report not found: " + reportId));
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", Map.of(
                "reportId", report.getId(),
                "testRunId", report.getTestRun().getId(),
                "reportType", report.getReportType().toString(),
                "fileName", report.getFileName(),
                "fileSize", report.getFileSize(),
                "generatedBy", report.getGeneratedBy(),
                "generatedAt", report.getGeneratedAt(),
                "description", report.getDescription() != null ? report.getDescription() : "",
                "downloadUrl", "/api/v1/reports/download/" + report.getId()
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to get report metadata for {}: {}", reportId, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to get report metadata: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Delete a report
     * 
     * @param reportId ID of the report to delete
     * @return Deletion status
     */
    @DeleteMapping("/{reportId}")
    public ResponseEntity<Map<String, Object>> deleteReport(@PathVariable Long reportId) {
        log.info("Deleting report ID: {}", reportId);
        
        try {
            Report report = reportRepository.findById(reportId)
                    .orElseThrow(() -> new RuntimeException("Report not found: " + reportId));
            
            // Delete physical file
            File file = new File(report.getFilePath());
            if (file.exists()) {
                boolean deleted = file.delete();
                if (!deleted) {
                    log.warn("Failed to delete report file: {}", report.getFilePath());
                }
            }
            
            // Delete database record
            reportRepository.delete(report);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Report deleted successfully");
            response.put("reportId", reportId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to delete report {}: {}", reportId, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to delete report: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get all reports in the system
     *
     * @return List of all reports
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllReports() {
        log.info("Getting all reports");
        
        try {
            List<Report> reports = reportRepository.findAll();
            
            List<Map<String, Object>> reportList = reports.stream()
                    .map(report -> {
                        Map<String, Object> reportMap = new HashMap<>();
                        reportMap.put("reportId", report.getId());
                        reportMap.put("testRunId", report.getTestRun() != null ? report.getTestRun().getId() : null);
                        reportMap.put("reportType", report.getReportType().toString());
                        reportMap.put("fileName", report.getFileName());
                        reportMap.put("fileSize", report.getFileSize());
                        reportMap.put("generatedBy", report.getGeneratedBy());
                        reportMap.put("generatedAt", report.getGeneratedAt().toString());
                        reportMap.put("downloadUrl", "/api/v1/reports/download/" + report.getId());
                        return reportMap;
                    })
                    .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("reports", reportList);
            response.put("count", reports.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to get all reports: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to get reports: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * List all reports (alias for GET /)
     *
     * @return List of all reports
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> listAllReports() {
        log.info("Listing all reports via /list endpoint");
        return getAllReports();
    }

    /**
     * List all reports for a test run
     *
     * @param testRunId ID of the test run
     * @return List of reports
     */
    @GetMapping("/test-run/{testRunId}")
    public ResponseEntity<Map<String, Object>> getReportsByTestRun(@PathVariable Long testRunId) {
        log.info("Getting reports for test run ID: {}", testRunId);
        
        try {
            List<Report> reports = reportRepository.findByTestRunId(testRunId);
            
            List<Map<String, Object>> reportList = reports.stream()
                    .map(report -> Map.of(
                        "reportId", (Object) report.getId(),
                        "reportType", report.getReportType().toString(),
                        "fileName", report.getFileName(),
                        "fileSize", report.getFileSize(),
                        "generatedBy", report.getGeneratedBy(),
                        "generatedAt", report.getGeneratedAt().toString(),
                        "downloadUrl", "/api/v1/reports/download/" + report.getId()
                    ))
                    .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("testRunId", testRunId);
            response.put("reports", reportList);
            response.put("count", reports.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to get reports for test run {}: {}", testRunId, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to get reports: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Generate Word report from JMeter test run data (Stress/Endurance/Load tests)
     * This endpoint generates reports from actual test data (JTL files, metrics, transactions)
     *
     * @param testRunId ID of the test run
     * @param authentication Current user authentication
     * @return Report generation response
     */
    @PostMapping(value = "/generate/jmeter/{testRunId}", consumes = MediaType.ALL_VALUE)
    public ResponseEntity<Map<String, Object>> generateJMeterReport(
            @PathVariable Long testRunId,
            @RequestParam(value = "includeBaseline", defaultValue = "false") boolean includeBaseline,
            @RequestParam(value = "includeRegression", defaultValue = "false") boolean includeRegression,
            Authentication authentication) {
        
        log.info("Generating JMeter Word report for test run ID: {}", testRunId);
        
        try {
            String generatedBy = authentication != null ? authentication.getName() : "system";
            Report report = wordReportGenerationService.generateWordReport(testRunId, generatedBy, includeBaseline, includeRegression);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "JMeter report generated successfully from test run data");
            response.put("data", Map.of(
                "reportId", report.getId(),
                "fileName", report.getFileName(),
                "filePath", report.getFilePath(),
                "fileSize", report.getFileSize(),
                "generatedAt", report.getGeneratedAt(),
                "downloadUrl", "/api/v1/reports/download/" + report.getId()
            ));
            
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            List<String> missingFields = wordReportGenerationService.getMissingCoverFields(testRunId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            errorResponse.put("missingFields", missingFields);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            log.error("Failed to generate JMeter report for test run {}: {}", testRunId, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to generate JMeter report: " + e.getMessage());
            errorResponse.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Validate required cover page fields for a JMeter report
     */
    @GetMapping("/validate/jmeter/{testRunId}")
    public ResponseEntity<Map<String, Object>> validateJMeterReport(@PathVariable Long testRunId) {
        List<String> missingFields = wordReportGenerationService.getMissingCoverFields(testRunId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", missingFields.isEmpty());
        response.put("missingFields", missingFields);
        if (missingFields.isEmpty()) {
            response.put("message", "All required fields are present");
            return ResponseEntity.ok(response);
        }
        response.put("message", "Missing required fields for cover page");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Generate PDF report from JMeter test run data
     */
    @PostMapping(value = "/generate/jmeter/{testRunId}/pdf", consumes = MediaType.ALL_VALUE)
    public ResponseEntity<Map<String, Object>> generateJMeterPdfReport(
            @PathVariable Long testRunId,
            @RequestParam(value = "includeBaseline", defaultValue = "false") boolean includeBaseline,
            @RequestParam(value = "includeRegression", defaultValue = "false") boolean includeRegression,
            Authentication authentication) {
        
        try {
            String generatedBy = authentication != null ? authentication.getName() : "system";
            Report report = wordReportGenerationService.generatePdfReport(testRunId, generatedBy, includeBaseline, includeRegression);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "JMeter PDF report generated successfully");
            response.put("data", Map.of(
                "reportId", report.getId(),
                "fileName", report.getFileName(),
                "downloadUrl", "/api/v1/reports/download/" + report.getId()
            ));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to generate PDF report: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Generate both Word and PDF reports
     */
    @PostMapping(value = "/generate/jmeter/{testRunId}/both", consumes = MediaType.ALL_VALUE)
    public ResponseEntity<Map<String, Object>> generateJMeterBothReports(
            @PathVariable Long testRunId,
            @RequestParam(value = "includeBaseline", defaultValue = "false") boolean includeBaseline,
            @RequestParam(value = "includeRegression", defaultValue = "false") boolean includeRegression,
            Authentication authentication) {
        
        try {
            String generatedBy = authentication != null ? authentication.getName() : "system";
            Report pdfReport = wordReportGenerationService.generateBothReports(testRunId, generatedBy, includeBaseline, includeRegression);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Both Word and PDF reports generated successfully");
            response.put("data", Map.of(
                "pdfReportId", pdfReport.getId(),
                "pdfFileName", pdfReport.getFileName(),
                "pdfDownloadUrl", "/api/v1/reports/download/" + pdfReport.getId()
            ));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to generate both reports: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

}

// Made with Bob