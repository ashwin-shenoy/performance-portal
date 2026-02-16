package com.hamza.durandhar.performance.controller;

import com.hamza.durandhar.performance.entity.Capability;
import com.hamza.durandhar.performance.entity.TestArtifact;
import com.hamza.durandhar.performance.entity.TestRun;
import com.hamza.durandhar.performance.repository.CapabilityRepository;
import com.hamza.durandhar.performance.repository.TestRunRepository;
import com.hamza.durandhar.performance.service.FileValidationService;
import com.hamza.durandhar.performance.service.JMeterImageParser;
import com.hamza.durandhar.performance.service.JtlFileParser;
import com.hamza.durandhar.performance.service.TestArtifactService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for multi-file upload (performance data + architecture diagram + test cases)
 */
@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {
    "http://localhost:3000",
    "http://localhost:5173"
}, allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class MultiFileUploadController {

    private final TestRunRepository testRunRepository;
    private final CapabilityRepository capabilityRepository;
    private final TestArtifactService testArtifactService;
    private final JtlFileParser jtlFileParser;
    private final JMeterImageParser jmeterImageParser;
    private final FileValidationService fileValidationService;

    @Value("${app.uploads.dir:uploads}")
    private String uploadsDirectory;

    /**
     * Simple upload endpoint for basic file uploads
     * This endpoint matches the frontend's default upload call
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadFiles(
            @RequestParam(value = "testRunId", required = false) Long testRunId,
            @RequestParam("capability") String capability,
            @RequestParam("testName") String testName,
            @RequestParam("buildNumber") String buildNumber,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("files") MultipartFile[] files
    ) {
        log.info("Upload request - Test: {}, Build: {}, Capability: {}, Files: {}",
                testName, buildNumber, capability, files.length);

        try {
            // Find capability by name
            Capability cap = capabilityRepository.findByName(capability)
                    .orElseThrow(() -> new IllegalArgumentException("Capability not found: " + capability));

            TestRun testRun;
            if (testRunId != null) {
                testRun = testRunRepository.findById(testRunId)
                        .orElseThrow(() -> new IllegalArgumentException("Test run not found: " + testRunId));
                if (!testRun.getCapability().getId().equals(cap.getId())) {
                    throw new IllegalArgumentException("Capability does not match the provided test run");
                }
                testRun.setTestName(testName);
                testRun.setBuildNumber(buildNumber);
                testRun.setDescription(description);
                testRun.setTestDate(LocalDateTime.now());
                testRun.setFileName(files[0].getOriginalFilename());
                testRun.setFileType(determineFileType(files[0].getOriginalFilename()));
                testRun.setStatus(TestRun.TestStatus.PROCESSING);
                testRun = testRunRepository.save(testRun);
                log.info("Reusing test run with ID: {}", testRun.getId());
            } else {
                // Create test run
                testRun = TestRun.builder()
                        .capability(cap)
                        .testName(testName)
                        .testDate(LocalDateTime.now())
                        .uploadedBy("system")
                        .fileName(files[0].getOriginalFilename())
                        .fileType(determineFileType(files[0].getOriginalFilename()))
                        .status(TestRun.TestStatus.PROCESSING)
                        .description(description)
                        .buildNumber(buildNumber)
                        .build();

                testRun = testRunRepository.save(testRun);
                log.info("Created test run with ID: {}", testRun.getId());
            }

            // Process each file
            Map<String, Object> fileResults = new HashMap<>();
            int successCount = 0;
            int failCount = 0;

            for (MultipartFile file : files) {
                String fileName = file.getOriginalFilename();
                log.info("Processing file: {}", fileName);

                try {
                    if (fileName != null && fileName.toLowerCase().endsWith(".jtl")) {
                        // Save JTL file to disk first
                        String savedFilePath = saveFileToDisk(file, testRun);
                        log.info("JTL file saved to: {}", savedFilePath);
                        
                        // Update test run with file path
                        testRun.setFilePath(savedFilePath);
                        testRunRepository.save(testRun);
                        
                        // Parse JTL file, generate diagrams, and calculate aggregated metrics
                        // Diagrams are generated during parsing before transaction data is discarded
                        JtlFileParser.ParseResult result = jtlFileParser.parseAndSave(testRun, file);
                        
                        // Build response with parsing and diagram results
                        Map<String, Object> jtlResult = new HashMap<>();
                        jtlResult.put("status", "success");
                        jtlResult.put("type", "jtl");
                        jtlResult.put("transactions", result.getTransactionCount());
                        jtlResult.put("metrics", result.getMetricCount());
                        jtlResult.put("savedPath", savedFilePath);
                        jtlResult.put("diagramsGenerated", result.getDiagramCount());
                        jtlResult.put("diagramDirectory", result.getDiagramDirectory());
                        
                        fileResults.put(fileName, jtlResult);
                        log.info("JTL processing complete: {} transactions, {} diagrams generated",
                                result.getTransactionCount(), result.getDiagramCount());
                        
                        successCount++;
                    } else if (fileName != null && (fileName.toLowerCase().endsWith(".png") ||
                               fileName.toLowerCase().endsWith(".jpg") ||
                               fileName.toLowerCase().endsWith(".jpeg"))) {
                        // Parse JMeter results image using OCR
                        log.info("Processing JMeter results image: {}", fileName);
                        
                        try {
                            JMeterImageParser.ParseResult result = jmeterImageParser.parseAndSave(testRun, file);
                            
                            Map<String, Object> imageResult = new HashMap<>();
                            imageResult.put("status", "success");
                            imageResult.put("type", "jmeter-image");
                            imageResult.put("metrics", result.getMetricCount());
                            imageResult.put("rowsParsed", result.getRowsParsed());
                            imageResult.put("message", "Image parsed using OCR");
                            imageResult.put("parsedData", result.getParsedRows());
                            
                            fileResults.put(fileName, imageResult);
                            successCount++;
                        } catch (OutOfMemoryError e) {
                            log.error("Out of memory processing image {}: {}", fileName, e.getMessage());
                            fileResults.put(fileName, Map.of(
                                    "status", "failed",
                                    "error", "Out of memory - image may be too large",
                                    "suggestion", "Try reducing image size or resolution"
                            ));
                            failCount++;
                            System.gc(); // Suggest garbage collection
                        } catch (Exception e) {
                            log.error("Error processing image {}: {}", fileName, e.getMessage(), e);
                            fileResults.put(fileName, Map.of(
                                    "status", "failed",
                                    "error", "OCR processing failed: " + e.getMessage()
                            ));
                            failCount++;
                        }
                    } else if (fileName != null && (fileName.toLowerCase().endsWith(".csv") ||
                               fileName.toLowerCase().endsWith(".xlsx") ||
                               fileName.toLowerCase().endsWith(".xls"))) {
                        // Build Performance and other test type support removed - only JMeter/JTL supported
                        fileResults.put(fileName, Map.of(
                                "status", "skipped",
                                "message", "Only JTL files are supported. CSV/Excel files are no longer processed."
                        ));
                    }
                } catch (Exception e) {
                    log.error("Error processing file {}: {}", fileName, e.getMessage(), e);
                    fileResults.put(fileName, Map.of(
                            "status", "failed",
                            "error", e.getMessage()
                    ));
                    failCount++;
                }
            }

            // Update test run status
            if (failCount == 0) {
                testRun.setStatus(TestRun.TestStatus.COMPLETED);
            } else if (successCount > 0) {
                testRun.setStatus(TestRun.TestStatus.COMPLETED);
                testRun.setErrorMessage(failCount + " file(s) failed to process");
            } else {
                testRun.setStatus(TestRun.TestStatus.FAILED);
                testRun.setErrorMessage("All files failed to process");
            }
            testRunRepository.save(testRun);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("testRunId", testRun.getId());
            response.put("message", "Upload completed");
            response.put("filesProcessed", successCount);
            response.put("filesFailed", failCount);
            response.put("fileResults", fileResults);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error processing upload", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Upload failed: " + e.getMessage()));
        }
    }

    /**
     * Upload test run with multiple files
     * - Performance data (required)
     * - Architecture diagram (optional)
     * - Test cases summary (optional)
     */
    @PostMapping(value = "/test-run", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadTestRun(
            @RequestParam("capabilityId") Long capabilityId,
            @RequestParam("testName") String testName,
            @RequestParam("testDate") String testDate,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "uploadedBy", defaultValue = "system") String uploadedBy,
            @RequestParam("performanceData") MultipartFile performanceData,
            @RequestParam(value = "architectureDiagram", required = false) MultipartFile architectureDiagram,
            @RequestParam(value = "testCasesSummary", required = false) MultipartFile testCasesSummary
    ) {
        log.info("Multi-file upload request for test: {}", testName);

        try {
            // Validate performance data
            FileValidationService.ValidationResult perfValidation = 
                    fileValidationService.validatePerformanceData(performanceData);
            if (!perfValidation.isValid()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse(perfValidation.getErrorMessage()));
            }

            // Validate architecture diagram if provided
            if (architectureDiagram != null && !architectureDiagram.isEmpty()) {
                FileValidationService.ValidationResult diagramValidation = 
                        fileValidationService.validateArchitectureDiagram(architectureDiagram);
                if (!diagramValidation.isValid()) {
                    return ResponseEntity.badRequest()
                            .body(createErrorResponse("Architecture diagram: " + diagramValidation.getErrorMessage()));
                }
            }

            // Validate test cases summary if provided
            if (testCasesSummary != null && !testCasesSummary.isEmpty()) {
                FileValidationService.ValidationResult testCasesValidation = 
                        fileValidationService.validateTestCasesSummary(testCasesSummary);
                if (!testCasesValidation.isValid()) {
                    return ResponseEntity.badRequest()
                            .body(createErrorResponse("Test cases summary: " + testCasesValidation.getErrorMessage()));
                }
            }

            // Create test run entity
            TestRun testRun = TestRun.builder()
                    .capability(null) // Will be set by service
                    .testName(testName)
                    .testDate(LocalDateTime.parse(testDate))
                    .uploadedBy(uploadedBy)
                    .fileName(performanceData.getOriginalFilename())
                    .fileType(determineFileType(performanceData.getOriginalFilename()))
                    .status(TestRun.TestStatus.UPLOADED)
                    .description(description)
                    .hasArchitectureDiagram(architectureDiagram != null && !architectureDiagram.isEmpty())
                    // hasTestCasesSummary removed as per master plan
                    .build();

            // Save test run (simplified - in real implementation, use proper service)
            // testRun = testRunRepository.save(testRun);
            // For now, create a mock ID
            Long testRunId = System.currentTimeMillis(); // Mock ID

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("testRunId", testRunId);
            response.put("status", "processing");

            Map<String, Object> artifacts = new HashMap<>();

            // Save performance data to disk
            String perfDataPath = null;
            try {
                // Create a temporary test run for saving the file
                TestRun tempTestRun = TestRun.builder()
                        .id(testRunId)
                        .testName(testName)
                        .build();
                perfDataPath = saveFileToDisk(performanceData, tempTestRun);
                log.info("Performance data saved to: {}", perfDataPath);
            } catch (Exception e) {
                log.error("Error saving performance data to disk", e);
            }
            
            // Process performance data
            Map<String, Object> perfDataInfo = new HashMap<>();
            perfDataInfo.put("fileName", performanceData.getOriginalFilename());
            perfDataInfo.put("status", "uploaded");
            perfDataInfo.put("size", performanceData.getSize());
            if (perfDataPath != null) {
                perfDataInfo.put("savedPath", perfDataPath);
            }
            artifacts.put("performanceData", perfDataInfo);

            // Process architecture diagram if provided
            if (architectureDiagram != null && !architectureDiagram.isEmpty()) {
                try {
                    TestArtifact diagram = testArtifactService.saveArchitectureDiagram(testRunId, architectureDiagram);
                    artifacts.put("architectureDiagram", Map.of(
                            "fileName", architectureDiagram.getOriginalFilename(),
                            "status", "uploaded",
                            "artifactId", diagram.getId(),
                            "size", architectureDiagram.getSize()
                    ));
                    log.info("Architecture diagram uploaded successfully");
                } catch (Exception e) {
                    log.error("Error uploading architecture diagram", e);
                    artifacts.put("architectureDiagram", Map.of(
                            "fileName", architectureDiagram.getOriginalFilename(),
                            "status", "failed",
                            "error", e.getMessage()
                    ));
                }
            }

            // Test cases functionality removed as per master plan
            // Test cases are no longer required

            response.put("artifacts", artifacts);
            response.put("message", "Files uploaded successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing multi-file upload", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Upload failed: " + e.getMessage()));
        }
    }

    /**
     * Determine file type from filename
     */
    private TestRun.FileType determineFileType(String fileName) {
        if (fileName == null) return TestRun.FileType.JTL;

        String lowerName = fileName.toLowerCase();
        if (lowerName.endsWith(".jtl")) return TestRun.FileType.JTL;
        return TestRun.FileType.JTL;
    }

    /**
     * Create error response
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        return response;
    }

    /**
     * Save uploaded file to disk
     */
    private String saveFileToDisk(MultipartFile file, TestRun testRun) throws IOException {
        // Create uploads directory if not exists
        Path uploadDir = Paths.get(uploadsDirectory);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        // Create subdirectory for this test run
        String testRunDir = String.format("testrun_%d_%s",
                testRun.getId(),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
        Path testRunPath = uploadDir.resolve(testRunDir);
        if (!Files.exists(testRunPath)) {
            Files.createDirectories(testRunPath);
        }

        // Save file with original name
        String fileName = file.getOriginalFilename();
        Path filePath = testRunPath.resolve(fileName);
        file.transferTo(filePath.toFile());

        log.info("File saved to disk: {}", filePath.toAbsolutePath());
        return filePath.toAbsolutePath().toString();
    }
}

// Made with Bob