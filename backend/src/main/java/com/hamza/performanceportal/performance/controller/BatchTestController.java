package com.hamza.performanceportal.performance.controller;

import com.hamza.performanceportal.performance.dto.*;
import com.hamza.performanceportal.performance.service.TestBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for batch test execution operations
 * Handles creating, executing, and reporting on batch test runs
 */
@RestController
@RequestMapping("/batch-tests")
@RequiredArgsConstructor
@Slf4j
public class BatchTestController {

    private final TestBatchService testBatchService;

    /**
     * Create a new batch test with specified test cases
     * POST /api/v1/batch-tests/create
     */
    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> createBatch(
            @RequestBody TestBatchExecutionRequest request) {
        log.info("POST /api/batch-tests/create - Creating batch: {}", request.getBatchName());

        try {
            TestBatchDTO batch = testBatchService.createBatch(request);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Batch created successfully");
            response.put("data", batch);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error creating batch", e);
            return buildErrorResponse("Failed to create batch: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Execute a batch test
     * POST /api/v1/batch-tests/{batchId}/execute
     */
    @PostMapping("/{batchId}/execute")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> executeBatch(
            @PathVariable String batchId) {
        log.info("POST /api/batch-tests/{}/execute - Executing batch", batchId);

        try {
            TestBatchDTO batch = testBatchService.executeBatch(batchId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Batch execution started");
            response.put("data", batch);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error executing batch", e);
            return buildErrorResponse("Failed to execute batch: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Get batch status and details
     * GET /api/v1/batch-tests/{batchId}
     */
    @GetMapping("/{batchId}")
    public ResponseEntity<Map<String, Object>> getBatchStatus(
            @PathVariable String batchId) {
        log.info("GET /api/batch-tests/{} - Getting batch status", batchId);

        try {
            TestBatchDTO batch = testBatchService.getBatchStatus(batchId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", batch);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching batch status", e);
            return buildErrorResponse("Batch not found: " + batchId, HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Generate consolidated report for a completed batch
     * POST /api/v1/batch-tests/{batchId}/generate-report
     */
    @PostMapping("/{batchId}/generate-report")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> generateReport(
            @PathVariable String batchId,
            @RequestBody(required = false) Map<String, List<String>> reportRequest) {
        log.info("POST /api/batch-tests/{}/generate-report - Generating consolidated report", batchId);

        try {
            List<String> reportTypes = reportRequest != null && reportRequest.containsKey("reportTypes")
                    ? reportRequest.get("reportTypes")
                    : List.of("TECHNICAL_PDF", "EXECUTIVE_PDF");

            testBatchService.generateConsolidatedReport(batchId, reportTypes);

            TestBatchDTO batch = testBatchService.getBatchStatus(batchId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Reports generated successfully");
            response.put("data", batch);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error generating report", e);
            return buildErrorResponse("Failed to generate report: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Get all batches for a capability
     * GET /api/v1/batch-tests/capability/{capabilityId}
     */
    @GetMapping("/capability/{capabilityId}")
    public ResponseEntity<Map<String, Object>> getBatchesByCapability(
            @PathVariable Long capabilityId) {
        log.info("GET /api/batch-tests/capability/{} - Fetching batches", capabilityId);

        try {
            List<TestBatchDTO> batches = testBatchService.getBatchesByCapability(capabilityId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", batches.size());
            response.put("data", batches);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching batches", e);
            return buildErrorResponse("Failed to fetch batches: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Update batch with test run result
     * POST /api/v1/batch-tests/{batchId}/update-result
     * Internal use: called after test execution completes
     */
    @PostMapping("/{batchId}/update-result")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateBatchResult(
            @PathVariable String batchId,
            @RequestBody Map<String, Object> resultData) {
        log.info("POST /api/batch-tests/{}/update-result - Updating batch result", batchId);

        try {
            Long testRunId = ((Number) resultData.get("testRunId")).longValue();
            Boolean success = (Boolean) resultData.get("success");

            testBatchService.updateBatchWithTestResult(batchId, testRunId, success != null ? success : false);

            TestBatchDTO batch = testBatchService.getBatchStatus(batchId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Batch result updated");
            response.put("data", batch);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating batch result", e);
            return buildErrorResponse("Failed to update result: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Build error response
     */
    private ResponseEntity<Map<String, Object>> buildErrorResponse(String message, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return ResponseEntity.status(status).body(response);
    }
}
