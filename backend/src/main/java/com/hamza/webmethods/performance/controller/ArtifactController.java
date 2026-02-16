package com.hamza.durandhar.performance.controller;

import com.hamza.durandhar.performance.entity.TestArtifact;
import com.hamza.durandhar.performance.service.TestArtifactService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for Test Artifact operations
 */
@RestController
@RequestMapping("/artifacts")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {
    "http://localhost:3000",
    "http://localhost:5173"
}, allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class ArtifactController {

    private final TestArtifactService testArtifactService;

    /**
     * Get all artifacts for a test run
     */
    @GetMapping("/test-run/{testRunId}")
    public ResponseEntity<List<TestArtifact>> getArtifactsByTestRun(@PathVariable Long testRunId) {
        log.info("REST request to get artifacts for test run: {}", testRunId);
        try {
            List<TestArtifact> artifacts = testArtifactService.getArtifactsByTestRunId(testRunId);
            return ResponseEntity.ok(artifacts);
        } catch (Exception e) {
            log.error("Error fetching artifacts", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get artifact by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<TestArtifact> getArtifactById(@PathVariable Long id) {
        log.info("REST request to get artifact: {}", id);
        Optional<TestArtifact> artifact = testArtifactService.getArtifactById(id);
        return artifact.map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    /**
     * Get architecture diagram for test run
     */
    @GetMapping("/test-run/{testRunId}/architecture-diagram")
    public ResponseEntity<Map<String, Object>> getArchitectureDiagram(@PathVariable Long testRunId) {
        log.info("REST request to get architecture diagram for test run: {}", testRunId);
        try {
            Optional<TestArtifact> artifact = testArtifactService.getArchitectureDiagram(testRunId);
            
            if (artifact.isPresent()) {
                TestArtifact diagram = artifact.get();
                Map<String, Object> response = new HashMap<>();
                response.put("id", diagram.getId());
                response.put("fileName", diagram.getFileName());
                response.put("fileType", diagram.getFileType());
                response.put("fileSize", diagram.getFileSize());
                response.put("mimeType", diagram.getMimeType());
                response.put("uploadedAt", diagram.getUploadedAt());
                response.put("downloadUrl", "/api/v1/artifacts/" + diagram.getId() + "/download");
                response.put("previewUrl", "/api/v1/artifacts/" + diagram.getId() + "/preview");
                response.put("metadata", diagram.getMetadata());
                
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
        } catch (Exception e) {
            log.error("Error fetching architecture diagram", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get test cases summary for test run
     */
    @GetMapping("/test-run/{testRunId}/test-cases-summary")
    public ResponseEntity<Map<String, Object>> getTestCasesSummary(@PathVariable Long testRunId) {
        log.info("REST request to get test cases summary for test run: {}", testRunId);
        try {
            Optional<TestArtifact> artifact = testArtifactService.getTestCasesSummary(testRunId);
            
            if (artifact.isPresent()) {
                TestArtifact summary = artifact.get();
                Map<String, Object> response = new HashMap<>();
                response.put("id", summary.getId());
                response.put("fileName", summary.getFileName());
                response.put("fileType", summary.getFileType());
                response.put("fileSize", summary.getFileSize());
                response.put("mimeType", summary.getMimeType());
                response.put("uploadedAt", summary.getUploadedAt());
                response.put("downloadUrl", "/api/v1/artifacts/" + summary.getId() + "/download");
                response.put("metadata", summary.getMetadata());
                
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
        } catch (Exception e) {
            log.error("Error fetching test cases summary", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Download artifact file
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadArtifact(@PathVariable Long id) {
        log.info("REST request to download artifact: {}", id);
        try {
            Optional<TestArtifact> artifactOpt = testArtifactService.getArtifactById(id);
            
            if (artifactOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            TestArtifact artifact = artifactOpt.get();
            byte[] fileContent = testArtifactService.getArtifactFile(id);
            
            ByteArrayResource resource = new ByteArrayResource(fileContent);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"" + artifact.getFileName() + "\"")
                    .contentType(MediaType.parseMediaType(artifact.getMimeType()))
                    .contentLength(fileContent.length)
                    .body(resource);
        } catch (IOException e) {
            log.error("Error downloading artifact", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Preview artifact (for images)
     */
    @GetMapping("/{id}/preview")
    public ResponseEntity<Resource> previewArtifact(@PathVariable Long id) {
        log.info("REST request to preview artifact: {}", id);
        try {
            Optional<TestArtifact> artifactOpt = testArtifactService.getArtifactById(id);
            
            if (artifactOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            TestArtifact artifact = artifactOpt.get();
            
            // Only allow preview for images
            if (!artifact.getMimeType().startsWith("image/")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            byte[] fileContent = testArtifactService.getArtifactFile(id);
            ByteArrayResource resource = new ByteArrayResource(fileContent);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(artifact.getMimeType()))
                    .contentLength(fileContent.length)
                    .body(resource);
        } catch (IOException e) {
            log.error("Error previewing artifact", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete artifact
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteArtifact(@PathVariable Long id) {
        log.info("REST request to delete artifact: {}", id);
        try {
            testArtifactService.deleteArtifact(id);
            return ResponseEntity.noContent().build();
        } catch (IOException e) {
            log.error("Error deleting artifact", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

// Made with Bob