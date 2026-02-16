package com.hamza.durandhar.performance.service;

import com.hamza.durandhar.performance.entity.Capability;
import com.hamza.durandhar.performance.entity.CapabilityTestCase;
import com.hamza.durandhar.performance.repository.CapabilityRepository;
import com.hamza.durandhar.performance.repository.CapabilityTestCaseRepository;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service class for managing Capability operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CapabilityService {

    private final CapabilityRepository capabilityRepository;
    private final CapabilityTestCaseRepository capabilityTestCaseRepository;

    @Value("${app.uploads.dir:/opt/performance-portal/uploads}")
    private String uploadsDir;

    /**
     * Get all capabilities
     */
    @Transactional(readOnly = true)
    public List<Capability> getAllCapabilities() {
        log.info("Fetching all Hamza durandhar iPaaS capabilities from database");
        List<Capability> capabilities = capabilityRepository.findAll();
        log.debug("Retrieved {} capabilities", capabilities.size());
        return capabilities;
    }

    /**
     * Get test case counts per capability
     */
    @Transactional(readOnly = true)
    public Map<Long, Long> getTestCaseCounts() {
        List<Object[]> rows = capabilityRepository.fetchTestCaseCounts();
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : rows) {
            Long capabilityId = (Long) row[0];
            Long count = (Long) row[1];
            counts.put(capabilityId, count);
        }
        return counts;
    }

    /**
     * Get all active capabilities
     */
    @Transactional(readOnly = true)
    public List<Capability> getActiveCapabilities() {
        log.info("Fetching active Hamza durandhar iPaaS capabilities");
        List<Capability> capabilities = capabilityRepository.findByIsActiveTrue();
        log.debug("Retrieved {} active capabilities for performance testing", capabilities.size());
        return capabilities;
    }

    /**
     * Get capability by ID
     */
    @Transactional(readOnly = true)
    public Optional<Capability> getCapabilityById(Long id) {
        log.info("Fetching Hamza durandhar capability with ID: {}", id);
        Optional<Capability> capability = capabilityRepository.findById(id);
        if (capability.isPresent()) {
            log.debug("Found capability: {}", capability.get().getName());
        } else {
            log.warn("No capability found with ID: {}", id);
        }
        return capability;
    }

    /**
     * Get capability by name
     */
    @Transactional(readOnly = true)
    public Optional<Capability> getCapabilityByName(String name) {
        log.info("Fetching Hamza durandhar capability with name: {}", name);
        Optional<Capability> capability = capabilityRepository.findByName(name);
        if (capability.isPresent()) {
            log.debug("Found capability '{}' with ID: {}", name, capability.get().getId());
        } else {
            log.warn("No capability found with name: {}", name);
        }
        return capability;
    }

    /**
     * Create new capability
     */
    @Transactional
    public Capability createCapability(Capability capability) {
        log.info("Creating new Hamza durandhar capability: {}", capability.getName());
        
        if (capabilityRepository.existsByName(capability.getName())) {
            log.error("Capability '{}' already exists in database", capability.getName());
            throw new IllegalArgumentException("Capability with name '" + capability.getName() + "' already exists");
        }
        
        Capability saved = capabilityRepository.save(capability);
        log.info("Successfully created capability '{}' with ID: {}", saved.getName(), saved.getId());
        return saved;
    }

    /**
     * Update existing capability
     */
    @Transactional
    public Capability updateCapability(Long id, Capability updatedCapability) {
        log.info("Updating Hamza durandhar capability with ID: {}", id);
        
        Capability capability = capabilityRepository.findById(id)
            .orElseThrow(() -> {
                log.error("Capability not found with ID: {}", id);
                return new IllegalArgumentException("Capability not found with ID: " + id);
            });
        
        String oldName = capability.getName();
        capability.setName(updatedCapability.getName());
        capability.setDescription(updatedCapability.getDescription());
        capability.setIsActive(updatedCapability.getIsActive());
        if (updatedCapability.getTestObjective() != null) capability.setTestObjective(updatedCapability.getTestObjective());
        if (updatedCapability.getTestScope() != null) capability.setTestScope(updatedCapability.getTestScope());
        if (updatedCapability.getEnvironmentDetails() != null) capability.setEnvironmentDetails(updatedCapability.getEnvironmentDetails());
        if (updatedCapability.getAcceptanceCriteria() != null && !updatedCapability.getAcceptanceCriteria().isEmpty()) {
            capability.setAcceptanceCriteria(updatedCapability.getAcceptanceCriteria());
        }

        Capability saved = capabilityRepository.save(capability);
        log.info("Successfully updated capability from '{}' to '{}' (ID: {})", oldName, saved.getName(), id);
        return saved;
    }

    /**
     * Delete capability
     */
    @Transactional
    public void deleteCapability(Long id) {
        log.info("Deleting Hamza durandhar capability with ID: {}", id);
        
        Optional<Capability> capability = capabilityRepository.findById(id);
        if (!capability.isPresent()) {
            log.error("Cannot delete - capability not found with ID: {}", id);
            throw new IllegalArgumentException("Capability not found with ID: " + id);
        }
        
        log.warn("Deleting capability '{}' (ID: {})", capability.get().getName(), id);
        capabilityRepository.deleteById(id);
        log.info("Successfully deleted capability with ID: {}", id);
    }

    /**
     * Toggle capability active status
     */
    @Transactional
    public Capability toggleCapabilityStatus(Long id) {
        log.info("Toggling status for Hamza durandhar capability with ID: {}", id);
        
        Capability capability = capabilityRepository.findById(id)
            .orElseThrow(() -> {
                log.error("Capability not found with ID: {}", id);
                return new IllegalArgumentException("Capability not found with ID: " + id);
            });
        
        boolean oldStatus = capability.getIsActive();
        capability.setIsActive(!capability.getIsActive());
        Capability saved = capabilityRepository.save(capability);
        log.info("Toggled capability '{}' status from {} to {} (ID: {})",
                 saved.getName(), oldStatus, saved.getIsActive(), id);
        return saved;
    }
    // ---- Metadata Update ----

    @Transactional
    public Capability updateCapabilityMetadata(Long id, String testObjective, String testScope,
            String environmentDetails, Map<String, Object> acceptanceCriteria) {
        log.info("Updating metadata for capability ID: {}", id);
        Capability capability = capabilityRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Capability not found with ID: " + id));

        if (testObjective != null) capability.setTestObjective(testObjective);
        if (testScope != null) capability.setTestScope(testScope);
        if (environmentDetails != null) capability.setEnvironmentDetails(environmentDetails);
        if (acceptanceCriteria != null && !acceptanceCriteria.isEmpty()) {
            capability.setAcceptanceCriteria(acceptanceCriteria);
        }

        return capabilityRepository.save(capability);
    }

    @Transactional
    public Capability updateCapabilityBaseline(Long id, Map<String, Object> baseline) {
        log.info("Updating baseline metrics for capability ID: {}", id);
        Capability capability = capabilityRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Capability not found with ID: " + id));

        Map<String, Object> acceptanceCriteria = capability.getAcceptanceCriteria();
        if (acceptanceCriteria == null) {
            acceptanceCriteria = new HashMap<>();
        }
        acceptanceCriteria.put("baseline", baseline);
        capability.setAcceptanceCriteria(acceptanceCriteria);
        return capabilityRepository.save(capability);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getCapabilityBaseline(Long id) {
        Capability capability = capabilityRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Capability not found with ID: " + id));

        Map<String, Object> acceptanceCriteria = capability.getAcceptanceCriteria();
        if (acceptanceCriteria == null) {
            return new HashMap<>();
        }

        Object baseline = acceptanceCriteria.get("baseline");
        if (baseline instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> baselineMap = (Map<String, Object>) baseline;
            return baselineMap;
        }

        return new HashMap<>();
    }

    // ---- Architecture Diagram Upload ----

    @Transactional
    public String uploadCapabilityArchitectureDiagram(Long id, MultipartFile file) throws IOException {
        log.info("Uploading architecture diagram for capability ID: {}", id);
        Capability capability = capabilityRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Capability not found with ID: " + id));

        String ext = "";
        String originalName = file.getOriginalFilename();
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf('.'));
        }

        Path dirPath = Paths.get(uploadsDir, "capabilities", String.valueOf(id));
        Files.createDirectories(dirPath);
        Path filePath = dirPath.resolve("architecture" + ext);
        file.transferTo(filePath.toFile());

        capability.setArchitectureDiagramPath(filePath.toString());
        capabilityRepository.save(capability);

        log.info("Architecture diagram saved to: {}", filePath);
        return filePath.toString();
    }

    @Transactional(readOnly = true)
    public Optional<Path> getCapabilityArchitectureDiagramPath(Long id) {
        Optional<Capability> capability = capabilityRepository.findById(id);
        if (capability.isEmpty()) {
            return Optional.empty();
        }

        String storedPath = capability.get().getArchitectureDiagramPath();
        if (storedPath == null || storedPath.isBlank()) {
            return Optional.empty();
        }

        Path filePath = Paths.get(storedPath);
        if (!Files.exists(filePath)) {
            return Optional.empty();
        }

        return Optional.of(filePath);
    }

    @Transactional
    public boolean removeCapabilityArchitectureDiagram(Long id) throws IOException {
        Capability capability = capabilityRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Capability not found with ID: " + id));

        String storedPath = capability.getArchitectureDiagramPath();
        capability.setArchitectureDiagramPath(null);
        capabilityRepository.save(capability);

        if (storedPath == null || storedPath.isBlank()) {
            return false;
        }

        Path filePath = Paths.get(storedPath);
        if (Files.exists(filePath)) {
            Files.delete(filePath);
            return true;
        }

        return false;
    }

    // ---- Test Case CRUD ----

    @Transactional(readOnly = true)
    public List<CapabilityTestCase> getTestCases(Long capabilityId) {
        return capabilityTestCaseRepository.findByCapabilityIdOrderByPriorityAsc(capabilityId);
    }

    @Transactional
    public CapabilityTestCase addTestCase(Long capabilityId, CapabilityTestCase testCase) {
        Capability capability = capabilityRepository.findById(capabilityId)
            .orElseThrow(() -> new IllegalArgumentException("Capability not found with ID: " + capabilityId));
        testCase.setCapability(capability);
        CapabilityTestCase saved = capabilityTestCaseRepository.save(testCase);
        log.info("Added test case '{}' to capability '{}' (ID: {})", saved.getTestCaseName(), capability.getName(), saved.getId());
        return saved;
    }

    @Transactional
    public CapabilityTestCase updateTestCase(Long testCaseId, CapabilityTestCase updated) {
        CapabilityTestCase existing = capabilityTestCaseRepository.findById(testCaseId)
            .orElseThrow(() -> new IllegalArgumentException("Test case not found with ID: " + testCaseId));
        existing.setTestCaseName(updated.getTestCaseName());
        existing.setDescription(updated.getDescription());
        existing.setExpectedBehavior(updated.getExpectedBehavior());
        existing.setPriority(updated.getPriority());
        return capabilityTestCaseRepository.save(existing);
    }

    @Transactional
    public void deleteTestCase(Long testCaseId) {
        if (!capabilityTestCaseRepository.existsById(testCaseId)) {
            throw new IllegalArgumentException("Test case not found with ID: " + testCaseId);
        }
        capabilityTestCaseRepository.deleteById(testCaseId);
        log.info("Deleted test case ID: {}", testCaseId);
    }
}
