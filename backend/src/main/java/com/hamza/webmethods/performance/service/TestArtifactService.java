package com.hamza.durandhar.performance.service;

import com.hamza.durandhar.performance.entity.TestArtifact;
import com.hamza.durandhar.performance.entity.TestRun;
import com.hamza.durandhar.performance.repository.TestArtifactRepository;
import com.hamza.durandhar.performance.repository.TestRunRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for managing test artifacts
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TestArtifactService {

    private final TestArtifactRepository testArtifactRepository;
    private final TestRunRepository testRunRepository;
    private final FileValidationService fileValidationService;

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    /**
     * Save architecture diagram artifact
     */
    @Transactional
    public TestArtifact saveArchitectureDiagram(Long testRunId, MultipartFile file) throws IOException {
        log.info("Saving architecture diagram for test run: {}", testRunId);

        // Validate file
        FileValidationService.ValidationResult validation = fileValidationService.validateArchitectureDiagram(file);
        if (!validation.isValid()) {
            throw new IllegalArgumentException(validation.getErrorMessage());
        }

        // Get test run
        TestRun testRun = testRunRepository.findById(testRunId)
                .orElseThrow(() -> new RuntimeException("Test run not found: " + testRunId));

        // Create directory structure
        String testRunDir = uploadDir + "/test_runs/" + testRunId;
        Path dirPath = Paths.get(testRunDir);
        Files.createDirectories(dirPath);

        // Save file
        String fileName = file.getOriginalFilename();
        String fileExtension = getFileExtension(fileName);
        String storedFileName = "architecture_diagram." + fileExtension;
        Path filePath = dirPath.resolve(storedFileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Extract metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("original_filename", fileName);
        metadata.put("content_type", file.getContentType());

        // Create artifact entity
        TestArtifact artifact = TestArtifact.builder()
                .testRun(testRun)
                .artifactType(TestArtifact.ArtifactType.ARCHITECTURE_DIAGRAM)
                .fileName(fileName)
                .filePath(filePath.toString())
                .fileType(fileExtension)
                .fileSize(file.getSize())
                .mimeType(file.getContentType())
                .metadata(metadata)
                .build();

        // Save to database
        TestArtifact savedArtifact = testArtifactRepository.save(artifact);

        // Update test run flag
        testRun.setHasArchitectureDiagram(true);
        testRunRepository.save(testRun);

        log.info("Architecture diagram saved successfully: {}", savedArtifact.getId());
        return savedArtifact;
    }

    /**
     * Save test cases summary artifact
     */
    @Transactional
    public TestArtifact saveTestCasesSummary(Long testRunId, MultipartFile file) throws IOException {
        log.info("Saving test cases summary for test run: {}", testRunId);

        // Validate file
        FileValidationService.ValidationResult validation = fileValidationService.validateTestCasesSummary(file);
        if (!validation.isValid()) {
            throw new IllegalArgumentException(validation.getErrorMessage());
        }

        // Get test run
        TestRun testRun = testRunRepository.findById(testRunId)
                .orElseThrow(() -> new RuntimeException("Test run not found: " + testRunId));

        // Create directory structure
        String testRunDir = uploadDir + "/test_runs/" + testRunId;
        Path dirPath = Paths.get(testRunDir);
        Files.createDirectories(dirPath);

        // Save file
        String fileName = file.getOriginalFilename();
        String fileExtension = getFileExtension(fileName);
        String storedFileName = "test_cases_summary." + fileExtension;
        Path filePath = dirPath.resolve(storedFileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Extract metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("original_filename", fileName);
        metadata.put("content_type", file.getContentType());

        // Create artifact entity
        TestArtifact artifact = TestArtifact.builder()
                .testRun(testRun)
                .artifactType(TestArtifact.ArtifactType.TEST_CASES_SUMMARY)
                .fileName(fileName)
                .filePath(filePath.toString())
                .fileType(fileExtension)
                .fileSize(file.getSize())
                .mimeType(file.getContentType())
                .metadata(metadata)
                .build();

        // Save to database
        TestArtifact savedArtifact = testArtifactRepository.save(artifact);

        // hasTestCasesSummary removed as per master plan
        // testRun.setHasTestCasesSummary(true);
        testRunRepository.save(testRun);

        log.info("Test cases summary saved successfully: {}", savedArtifact.getId());
        return savedArtifact;
    }

    /**
     * Get all artifacts for a test run
     */
    public List<TestArtifact> getArtifactsByTestRunId(Long testRunId) {
        return testArtifactRepository.findByTestRunId(testRunId);
    }

    /**
     * Get artifact by ID
     */
    public Optional<TestArtifact> getArtifactById(Long id) {
        return testArtifactRepository.findById(id);
    }

    /**
     * Get architecture diagram for test run
     */
    public Optional<TestArtifact> getArchitectureDiagram(Long testRunId) {
        return testArtifactRepository.findByTestRunIdAndArtifactType(
                testRunId, TestArtifact.ArtifactType.ARCHITECTURE_DIAGRAM);
    }

    /**
     * Get test cases summary for test run
     */
    public Optional<TestArtifact> getTestCasesSummary(Long testRunId) {
        return testArtifactRepository.findByTestRunIdAndArtifactType(
                testRunId, TestArtifact.ArtifactType.TEST_CASES_SUMMARY);
    }

    /**
     * Delete artifact
     */
    @Transactional
    public void deleteArtifact(Long id) throws IOException {
        TestArtifact artifact = testArtifactRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Artifact not found: " + id));

        // Delete file from filesystem
        Path filePath = Paths.get(artifact.getFilePath());
        if (Files.exists(filePath)) {
            Files.delete(filePath);
            log.info("Deleted file: {}", filePath);
        }

        // Delete from database
        testArtifactRepository.delete(artifact);

        // Update test run flags
        TestRun testRun = artifact.getTestRun();
        if (artifact.getArtifactType() == TestArtifact.ArtifactType.ARCHITECTURE_DIAGRAM) {
            testRun.setHasArchitectureDiagram(false);
        }
        // hasTestCasesSummary removed as per master plan
        testRunRepository.save(testRun);

        log.info("Artifact deleted successfully: {}", id);
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
     * Get artifact file as byte array
     */
    public byte[] getArtifactFile(Long id) throws IOException {
        TestArtifact artifact = testArtifactRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Artifact not found: " + id));

        Path filePath = Paths.get(artifact.getFilePath());
        if (!Files.exists(filePath)) {
            throw new IOException("File not found: " + filePath);
        }

        return Files.readAllBytes(filePath);
    }
}

// Made with Bob