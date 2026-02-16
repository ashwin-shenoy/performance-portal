package com.hamza.durandhar.performance.service;

import com.hamza.durandhar.performance.entity.TestRun;
import com.hamza.durandhar.performance.repository.TestRunRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TestRunService {

    private final TestRunRepository testRunRepository;

    @Transactional(readOnly = true)
    public List<TestRun> getAllTestRuns() {
        log.info("Fetching all test runs");
        return testRunRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<TestRun> getTestRunById(Long id) {
        log.info("Fetching test run with id: {}", id);
        return testRunRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<TestRun> getTestRunsByCapabilityId(Long capabilityId) {
        log.info("Fetching test runs for capability: {}", capabilityId);
        return testRunRepository.findByCapabilityId(capabilityId);
    }

    @Transactional(readOnly = true)
    public List<TestRun> getTestRunsByStatus(TestRun.TestStatus status) {
        log.info("Fetching test runs with status: {}", status);
        return testRunRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public List<TestRun> getTestRunsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Fetching test runs between {} and {}", startDate, endDate);
        return testRunRepository.findByTestDateBetween(startDate, endDate);
    }

    @Transactional(readOnly = true)
    public List<TestRun> getTestRunsByUploadedBy(String uploadedBy) {
        log.info("Fetching test runs uploaded by: {}", uploadedBy);
        return testRunRepository.findByUploadedBy(uploadedBy);
    }

    @Transactional
    public TestRun createTestRun(TestRun testRun) {
        log.info("Creating new test run: {}", testRun.getTestName());
        return testRunRepository.save(testRun);
    }

    @Transactional
    public TestRun updateTestRun(Long id, TestRun testRun) {
        log.info("Updating test run with id: {}", id);
        return testRunRepository.findById(id)
                .map(existing -> {
                    existing.setTestName(testRun.getTestName());
                    existing.setDescription(testRun.getDescription());
                    existing.setStatus(testRun.getStatus());
                    existing.setNotes(testRun.getNotes());
                    return testRunRepository.save(existing);
                })
                .orElseThrow(() -> new RuntimeException("Test run not found with id: " + id));
    }

    @Transactional
    public void deleteTestRun(Long id) {
        log.info("Deleting test run with id: {}", id);
        testRunRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public long countByStatus(TestRun.TestStatus status) {
        return testRunRepository.countByStatus(status);
    }

    @Transactional(readOnly = true)
    public long countByCapability(Long capabilityId) {
        return testRunRepository.countByCapabilityId(capabilityId);
    }
}

// Made with Bob
