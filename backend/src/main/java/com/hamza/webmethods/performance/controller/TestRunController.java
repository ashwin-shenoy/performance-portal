package com.hamza.durandhar.performance.controller;

import com.hamza.durandhar.performance.entity.TestRun;
import com.hamza.durandhar.performance.service.TestRunService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/tests")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {
    "http://localhost:3000",
    "http://localhost:5173"
}, allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class TestRunController {

    private final TestRunService testRunService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllTestRuns(
            @RequestParam(required = false) Long capabilityId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String uploadedBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        log.info("REST request to get all test runs with filters");
        try {
            List<TestRun> testRuns;

            if (capabilityId != null) {
                testRuns = testRunService.getTestRunsByCapabilityId(capabilityId);
            } else if (status != null) {
                testRuns = testRunService.getTestRunsByStatus(TestRun.TestStatus.valueOf(status.toUpperCase()));
            } else if (uploadedBy != null) {
                testRuns = testRunService.getTestRunsByUploadedBy(uploadedBy);
            } else if (startDate != null && endDate != null) {
                testRuns = testRunService.getTestRunsByDateRange(startDate, endDate);
            } else {
                testRuns = testRunService.getAllTestRuns();
            }

            List<Map<String, Object>> response = testRuns.stream()
                    .map(this::mapTestRunToResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching test runs", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getTestRunById(@PathVariable Long id) {
        log.info("REST request to get test run with id: {}", id);
        return testRunService.getTestRunById(id)
                .map(testRun -> ResponseEntity.ok(mapTestRunToResponse(testRun)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PostMapping
    public ResponseEntity<TestRun> createTestRun(@RequestBody TestRun testRun) {
        log.info("REST request to create test run: {}", testRun.getTestName());
        try {
            TestRun created = testRunService.createTestRun(testRun);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            log.error("Error creating test run", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<TestRun> updateTestRun(@PathVariable Long id, @RequestBody TestRun testRun) {
        log.info("REST request to update test run with id: {}", id);
        try {
            TestRun updated = testRunService.updateTestRun(id, testRun);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            log.error("Error updating test run", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Error updating test run", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTestRun(@PathVariable Long id) {
        log.info("REST request to delete test run with id: {}", id);
        try {
            testRunService.deleteTestRun(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting test run", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get file content preview for a test run
     *
     * @param id Test run ID
     * @param maxLines Maximum number of lines to return (default 100)
     * @return File content preview
     */
    @GetMapping("/{id}/file-content")
    public ResponseEntity<Map<String, Object>> getFileContent(
            @PathVariable Long id,
            @RequestParam(defaultValue = "100") int maxLines) {
        log.info("REST request to get file content for test run: {}", id);
        
        try {
            TestRun testRun = testRunService.getTestRunById(id)
                    .orElseThrow(() -> new RuntimeException("Test run not found: " + id));
            
            String filePath = testRun.getFilePath();
            if (filePath == null || filePath.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("content", "File path not available");
                response.put("truncated", false);
                return ResponseEntity.ok(response);
            }
            
            File file = new File(filePath);
            if (!file.exists()) {
                Map<String, Object> response = new HashMap<>();
                response.put("content", "File not found: " + filePath);
                response.put("truncated", false);
                return ResponseEntity.ok(response);
            }
            
            // Read file content with line limit
            StringBuilder content = new StringBuilder();
            int lineCount = 0;
            boolean truncated = false;
            
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null && lineCount < maxLines) {
                    content.append(line).append("\n");
                    lineCount++;
                }
                
                // Check if there are more lines
                if (reader.readLine() != null) {
                    truncated = true;
                }
            } catch (IOException e) {
                log.error("Error reading file content", e);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("content", "Error reading file: " + e.getMessage());
                errorResponse.put("truncated", false);
                return ResponseEntity.ok(errorResponse);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("content", content.toString());
            response.put("truncated", truncated);
            response.put("lines", lineCount);
            response.put("fileName", testRun.getFileName());
            response.put("fileType", testRun.getFileType() != null ? testRun.getFileType().toString() : "UNKNOWN");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error fetching file content for test run {}: {}", id, e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("content", "Error: " + e.getMessage());
            errorResponse.put("truncated", false);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get baseline evaluation results for a test run
     */
    @GetMapping("/{id}/baseline-evaluation")
    public ResponseEntity<Map<String, Object>> getBaselineEvaluation(@PathVariable Long id) {
        try {
            TestRun testRun = testRunService.getTestRunById(id)
                    .orElseThrow(() -> new RuntimeException("Test run not found: " + id));

            Map<String, Object> capabilityData = testRun.getCapabilitySpecificData();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("testRunId", id);
            response.put("baselineEvaluation",
                    capabilityData != null ? capabilityData.getOrDefault("baselineEvaluation", new HashMap<>()) : new HashMap<>());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching baseline evaluation for test run {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private Map<String, Object> mapTestRunToResponse(TestRun testRun) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", testRun.getId());
        response.put("testName", testRun.getTestName());
        response.put("capability", testRun.getCapability() != null ? testRun.getCapability().getName() : "Unknown");
        response.put("capabilityId", testRun.getCapability() != null ? testRun.getCapability().getId() : null);
        response.put("status", testRun.getStatus().toString());
        response.put("uploadDate", testRun.getTestDate());
        response.put("totalTransactions", testRun.getTotalRequests() != null ? testRun.getTotalRequests() : 0);
        response.put("avgResponseTime", testRun.getAvgResponseTime() != null ? testRun.getAvgResponseTime() : 0.0);
        response.put("errorRate", testRun.getErrorRate() != null ? testRun.getErrorRate() : 0.0);
        response.put("throughput", testRun.getThroughput() != null ? testRun.getThroughput() : 0.0);
        response.put("description", testRun.getDescription());
        response.put("uploadedBy", testRun.getUploadedBy());
        response.put("fileName", testRun.getFileName());
        response.put("fileType", testRun.getFileType() != null ? testRun.getFileType().toString() : null);
        response.put("testType", testRun.getTestType() != null ? testRun.getTestType().toString() : determineTestType(testRun));
        response.put("virtualUsers", testRun.getVirtualUsers());
        response.put("buildNumber", testRun.getBuildNumber());
        response.put("testDuration", testRun.getTestDurationSeconds());
        return response;
    }

    private String determineTestType(TestRun testRun) {
        // Determine test type based on file type if not explicitly set
        if (testRun.getFileType() == TestRun.FileType.JTL) {
            return "JMETER";
        }
        return "UNKNOWN";
    }
}

// Made with Bob
