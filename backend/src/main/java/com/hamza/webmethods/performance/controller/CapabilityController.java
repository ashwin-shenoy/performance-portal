package com.hamza.durandhar.performance.controller;

import com.hamza.durandhar.performance.entity.Capability;
import com.hamza.durandhar.performance.entity.CapabilityTestCase;
import com.hamza.durandhar.performance.service.CapabilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST Controller for Capability management
 */
@RestController
@RequestMapping("/capabilities")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {
    "http://localhost:3000",
    "http://localhost:5173"
}, allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class CapabilityController {

    private final CapabilityService capabilityService;

    /**
     * Get all capabilities
     */
    @GetMapping
    public ResponseEntity<List<Capability>> getAllCapabilities() {
        log.info("GET /api/capabilities - Fetching all Hamza durandhar iPaaS capabilities");
        List<Capability> capabilities = capabilityService.getAllCapabilities();
        log.debug("Retrieved {} capabilities from database", capabilities.size());
        return ResponseEntity.ok(capabilities);
    }

    /**
     * Get test case counts per capability
     */
    @GetMapping("/test-case-counts")
    public ResponseEntity<Map<Long, Long>> getTestCaseCounts() {
        log.info("GET /api/capabilities/test-case-counts - Fetching capability test case counts");
        Map<Long, Long> counts = capabilityService.getTestCaseCounts();
        return ResponseEntity.ok(counts);
    }

    /**
     * Get all active capabilities
     */
    @GetMapping("/active")
    public ResponseEntity<List<Capability>> getActiveCapabilities() {
        log.info("GET /api/capabilities/active - Fetching active Hamza durandhar iPaaS capabilities");
        List<Capability> capabilities = capabilityService.getActiveCapabilities();
        log.debug("Retrieved {} active capabilities", capabilities.size());
        return ResponseEntity.ok(capabilities);
    }

    /**
     * Get capabilities formatted for dropdown (value-label pairs)
     */
    @GetMapping("/dropdown")
    public ResponseEntity<List<Map<String, String>>> getCapabilitiesForDropdown() {
        log.info("GET /api/capabilities/dropdown - Fetching Hamza durandhar iPaaS capabilities for UI dropdown");
        List<Capability> capabilities = capabilityService.getActiveCapabilities();
        
        List<Map<String, String>> dropdownOptions = capabilities.stream()
            .map(cap -> Map.of(
                "value", cap.getName(),
                "label", cap.getName()
            ))
            .collect(Collectors.toList());
        
        log.debug("Prepared {} capability options for dropdown", dropdownOptions.size());
        return ResponseEntity.ok(dropdownOptions);
    }

    /**
     * Get capability by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Capability> getCapabilityById(@PathVariable Long id) {
        log.info("GET /api/capabilities/{} - Fetching Hamza durandhar capability by ID", id);
        return capabilityService.getCapabilityById(id)
            .map(cap -> {
                log.debug("Found capability: {}", cap.getName());
                return ResponseEntity.ok(cap);
            })
            .orElseGet(() -> {
                log.warn("Capability with ID {} not found", id);
                return ResponseEntity.notFound().build();
            });
    }

    /**
     * Get capability by name
     */
    @GetMapping("/name/{name}")
    public ResponseEntity<Capability> getCapabilityByName(@PathVariable String name) {
        log.info("GET /api/capabilities/name/{} - Fetching Hamza durandhar capability by name", name);
        return capabilityService.getCapabilityByName(name)
            .map(cap -> {
                log.debug("Found capability: {} (ID: {})", cap.getName(), cap.getId());
                return ResponseEntity.ok(cap);
            })
            .orElseGet(() -> {
                log.warn("Capability '{}' not found in database", name);
                return ResponseEntity.notFound().build();
            });
    }

    /**
     * Create new capability
     */
    @PostMapping
    public ResponseEntity<Capability> createCapability(@RequestBody Capability capability) {
        log.info("POST /api/capabilities - Creating new Hamza durandhar capability: {}", capability.getName());
        try {
            Capability created = capabilityService.createCapability(capability);
            log.info("Successfully created capability: {} (ID: {})", created.getName(), created.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            log.error("Error creating capability '{}': {}", capability.getName(), e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update existing capability
     */
    @PutMapping("/{id}")
    public ResponseEntity<Capability> updateCapability(
            @PathVariable Long id,
            @RequestBody Capability capability) {
        log.info("PUT /api/capabilities/{} - Updating Hamza durandhar capability", id);
        try {
            Capability updated = capabilityService.updateCapability(id, capability);
            log.info("Successfully updated capability: {} (ID: {})", updated.getName(), updated.getId());
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.error("Error updating capability with ID {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Delete capability
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCapability(@PathVariable Long id) {
        log.info("DELETE /api/capabilities/{} - Deleting Hamza durandhar capability", id);
        try {
            capabilityService.deleteCapability(id);
            log.info("Successfully deleted capability with ID: {}", id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.error("Error deleting capability with ID {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Toggle capability active status
     */
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<Capability> toggleCapabilityStatus(@PathVariable Long id) {
        log.info("PATCH /api/capabilities/{}/toggle - Toggling Hamza durandhar capability status", id);
        try {
            Capability updated = capabilityService.toggleCapabilityStatus(id);
            log.info("Toggled capability '{}' status to: {}", updated.getName(), updated.getIsActive() ? "ACTIVE" : "INACTIVE");
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.error("Error toggling capability status for ID {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    // ---- Metadata Endpoints ----

    /**
     * Update capability metadata (test objective, scope, environment, acceptance criteria)
     */
    @PutMapping("/{id}/metadata")
    public ResponseEntity<Map<String, Object>> updateCapabilityMetadata(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        log.info("PUT /capabilities/{}/metadata - Updating capability metadata", id);
        try {
            String testObjective = (String) body.get("testObjective");
            String testScope = (String) body.get("testScope");
            String environmentDetails = (String) body.get("environmentDetails");
            @SuppressWarnings("unchecked")
            Map<String, Object> acceptanceCriteria = (Map<String, Object>) body.get("acceptanceCriteria");

            Capability updated = capabilityService.updateCapabilityMetadata(
                id, testObjective, testScope, environmentDetails, acceptanceCriteria);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Capability metadata updated successfully");
            response.put("capabilityId", updated.getId());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Update baseline metrics for a capability
     */
    @PutMapping("/{id}/baseline")
    public ResponseEntity<Map<String, Object>> updateCapabilityBaseline(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        log.info("PUT /capabilities/{}/baseline - Updating baseline metrics", id);
        try {
            Capability updated = capabilityService.updateCapabilityBaseline(id, body);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Baseline metrics updated successfully");
            response.put("capabilityId", updated.getId());
            response.put("baseline", capabilityService.getCapabilityBaseline(id));
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get baseline metrics for a capability
     */
    @GetMapping("/{id}/baseline")
    public ResponseEntity<Map<String, Object>> getCapabilityBaseline(@PathVariable Long id) {
        log.info("GET /capabilities/{}/baseline - Fetching baseline metrics", id);
        try {
            Map<String, Object> baseline = capabilityService.getCapabilityBaseline(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("baseline", baseline);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ---- Architecture Diagram Endpoints ----

    /**
     * Upload capability-level architecture diagram
     */
    @PostMapping(value = "/{id}/architecture-diagram", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadArchitectureDiagram(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        log.info("POST /capabilities/{}/architecture-diagram - Uploading architecture diagram", id);
        try {
            String filePath = capabilityService.uploadCapabilityArchitectureDiagram(id, file);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Architecture diagram uploaded successfully");
            response.put("filePath", filePath);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to upload architecture diagram: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Preview capability-level architecture diagram
     */
    @GetMapping("/{id}/architecture-diagram/preview")
    public ResponseEntity<Resource> previewArchitectureDiagram(@PathVariable Long id) {
        log.info("GET /capabilities/{}/architecture-diagram/preview", id);
        try {
            return buildArchitectureDiagramResponse(id, false);
        } catch (IOException e) {
            log.error("Failed to preview architecture diagram: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Download capability-level architecture diagram
     */
    @GetMapping("/{id}/architecture-diagram/download")
    public ResponseEntity<Resource> downloadArchitectureDiagram(@PathVariable Long id) {
        log.info("GET /capabilities/{}/architecture-diagram/download", id);
        try {
            return buildArchitectureDiagramResponse(id, true);
        } catch (IOException e) {
            log.error("Failed to download architecture diagram: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Remove capability-level architecture diagram
     */
    @DeleteMapping("/{id}/architecture-diagram")
    public ResponseEntity<Map<String, Object>> deleteArchitectureDiagram(@PathVariable Long id) {
        log.info("DELETE /capabilities/{}/architecture-diagram", id);
        try {
            boolean removed = capabilityService.removeCapabilityArchitectureDiagram(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", removed ? "Architecture diagram removed" : "No diagram to remove");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IOException e) {
            log.error("Failed to remove architecture diagram: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    private ResponseEntity<Resource> buildArchitectureDiagramResponse(Long id, boolean asAttachment) throws IOException {
        return capabilityService.getCapabilityArchitectureDiagramPath(id)
            .map(path -> {
                try {
                    byte[] fileContent = Files.readAllBytes(path);
                    String contentType = Files.probeContentType(path);
                    if (contentType == null || contentType.isBlank()) {
                        contentType = "application/octet-stream";
                    }

                    ByteArrayResource resource = new ByteArrayResource(fileContent);
                    ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .contentLength(fileContent.length);

                    if (asAttachment) {
                        String fileName = path.getFileName().toString();
                        builder.header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + fileName + "\"");
                    }

                    return builder.body((Resource) resource);
                } catch (IOException e) {
                    log.error("Failed to read architecture diagram: {}", e.getMessage(), e);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).<Resource>build();
                }
            })
            .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).<Resource>build());
    }

    // ---- Test Case Endpoints ----

    /**
     * List all test cases for a capability
     */
    @GetMapping("/{id}/test-cases")
    public ResponseEntity<List<CapabilityTestCase>> getTestCases(@PathVariable Long id) {
        log.info("GET /capabilities/{}/test-cases", id);
        List<CapabilityTestCase> testCases = capabilityService.getTestCases(id);
        return ResponseEntity.ok(testCases);
    }

    /**
     * Create a test case for a capability
     */
    @PostMapping("/{id}/test-cases")
    public ResponseEntity<CapabilityTestCase> createTestCase(
            @PathVariable Long id,
            @RequestBody CapabilityTestCase testCase) {
        log.info("POST /capabilities/{}/test-cases - Creating test case: {}", id, testCase.getTestCaseName());
        try {
            CapabilityTestCase created = capabilityService.addTestCase(id, testCase);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Update a test case
     */
    @PutMapping("/{id}/test-cases/{testCaseId}")
    public ResponseEntity<CapabilityTestCase> updateTestCase(
            @PathVariable Long id,
            @PathVariable Long testCaseId,
            @RequestBody CapabilityTestCase testCase) {
        log.info("PUT /capabilities/{}/test-cases/{} - Updating test case", id, testCaseId);
        try {
            CapabilityTestCase updated = capabilityService.updateTestCase(testCaseId, testCase);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Delete a test case
     */
    @DeleteMapping("/{id}/test-cases/{testCaseId}")
    public ResponseEntity<Void> deleteTestCase(
            @PathVariable Long id,
            @PathVariable Long testCaseId) {
        log.info("DELETE /capabilities/{}/test-cases/{}", id, testCaseId);
        try {
            capabilityService.deleteTestCase(testCaseId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
