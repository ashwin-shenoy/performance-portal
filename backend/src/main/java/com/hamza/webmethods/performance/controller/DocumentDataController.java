package com.hamza.durandhar.performance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamza.durandhar.performance.entity.TestRun;
import com.hamza.durandhar.performance.repository.TestRunRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for managing document data (narrative sections) for reports
 */
@RestController
@RequestMapping("/test-runs")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {
    "http://localhost:3000",
    "http://localhost:5173"
}, allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class DocumentDataController {

    private final TestRunRepository testRunRepository;
    private final ObjectMapper objectMapper;

    /**
     * Save document data (narrative sections) for a test run
     * 
     * @param testRunId ID of the test run
     * @param documentData Document data containing narrative sections
     * @return Success response
     */
    @PostMapping("/{testRunId}/document-data")
    public ResponseEntity<Map<String, Object>> saveDocumentData(
            @PathVariable Long testRunId,
            @RequestBody Map<String, Object> documentData) {
        
        log.info("Saving document data for test run ID: {}", testRunId);
        
        try {
            TestRun testRun = testRunRepository.findById(testRunId)
                    .orElseThrow(() -> new RuntimeException("Test run not found with ID: " + testRunId));
            
            // Convert document data to JSON string
            String jsonData = objectMapper.writeValueAsString(documentData);
            
            // Store in test run (you may need to add a documentData field to TestRun entity)
            // For now, we'll store it in the notes field or create a new field
            testRun.setNotes(jsonData); // Temporary solution
            testRunRepository.save(testRun);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Document data saved successfully");
            response.put("testRunId", testRunId);
            
            log.info("Document data saved successfully for test run ID: {}", testRunId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error saving document data for test run ID: {}", testRunId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to save document data: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get document data for a test run
     * 
     * @param testRunId ID of the test run
     * @return Document data
     */
    @GetMapping("/{testRunId}/document-data")
    public ResponseEntity<Map<String, Object>> getDocumentData(@PathVariable Long testRunId) {
        
        log.info("Retrieving document data for test run ID: {}", testRunId);
        
        try {
            TestRun testRun = testRunRepository.findById(testRunId)
                    .orElseThrow(() -> new RuntimeException("Test run not found with ID: " + testRunId));
            
            Map<String, Object> response = new HashMap<>();
            
            // Parse JSON from notes field (temporary solution)
            if (testRun.getNotes() != null && !testRun.getNotes().isEmpty()) {
                try {
                    Map<String, Object> documentData = objectMapper.readValue(
                            testRun.getNotes(), 
                            Map.class
                    );
                    response.put("success", true);
                    response.put("data", documentData);
                } catch (Exception e) {
                    log.warn("Could not parse document data as JSON, returning as string");
                    response.put("success", true);
                    response.put("data", new HashMap<>());
                }
            } else {
                response.put("success", true);
                response.put("data", new HashMap<>());
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error retrieving document data for test run ID: {}", testRunId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to retrieve document data: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Delete document data for a test run
     * 
     * @param testRunId ID of the test run
     * @return Success response
     */
    @DeleteMapping("/{testRunId}/document-data")
    public ResponseEntity<Map<String, Object>> deleteDocumentData(@PathVariable Long testRunId) {
        
        log.info("Deleting document data for test run ID: {}", testRunId);
        
        try {
            TestRun testRun = testRunRepository.findById(testRunId)
                    .orElseThrow(() -> new RuntimeException("Test run not found with ID: " + testRunId));
            
            testRun.setNotes(null);
            testRunRepository.save(testRun);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Document data deleted successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error deleting document data for test run ID: {}", testRunId, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to delete document data: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}

// Made with Bob
