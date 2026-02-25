package com.hamza.performanceportal.performance.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * TestBatch entity representing a batch execution of multiple test cases.
 * Groups multiple TestRun instances under a single Batch ID for consolidated reporting.
 */
@Entity
@Table(name = "test_batches", indexes = {
    @Index(name = "idx_batch_capability", columnList = "capability_id"),
    @Index(name = "idx_batch_status", columnList = "status"),
    @Index(name = "idx_batch_batch_id", columnList = "batch_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique batch identifier for grouping multiple test runs
     */
    @Column(name = "batch_id", nullable = false, unique = true, length = 36)
    private String batchId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "capability_id", nullable = false)
    @ToString.Exclude
    private Capability capability;

    /**
     * Batch name - describes what this batch test is about
     */
    @Column(name = "batch_name", nullable = false, length = 255)
    private String batchName;

    /**
     * Description of the batch test execution
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Status of the batch: PENDING, IN_PROGRESS, COMPLETED, FAILED
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private BatchStatus status = BatchStatus.PENDING;

    /**
     * Total number of test cases in this batch
     */
    @Column(name = "total_test_cases", nullable = false)
    private Integer totalTestCases;

    /**
     * Number of completed test cases
     */
    @Column(name = "completed_test_cases")
    @Builder.Default
    private Integer completedTestCases = 0;

    /**
     * Number of failed test cases
     */
    @Column(name = "failed_test_cases")
    @Builder.Default
    private Integer failedTestCases = 0;

    /**
     * Overall pass/fail status of the batch
     */
    @Column(name = "batch_result", length = 20)
    private String batchResult;

    /**
     * Overall performance summary (avg response time, throughput, etc)
     */
    @Column(name = "performance_summary", columnDefinition = "TEXT")
    private String performanceSummary;

    /**
     * Timestamp when batch test started
     */
    @Column(name = "start_time")
    private LocalDateTime startTime;

    /**
     * Timestamp when batch test completed
     */
    @Column(name = "end_time")
    private LocalDateTime endTime;

    /**
     * Total execution time in seconds
     */
    @Column(name = "total_duration_seconds")
    private Long totalDurationSeconds;

    /**
     * Test runs that are part of this batch
     */
    @OneToMany(mappedBy = "testBatch", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference("batch-testruns")
    @ToString.Exclude
    @Builder.Default
    private List<TestRun> testRuns = new ArrayList<>();

    /**
     * Consolidated reports generated for this batch
     */
    @OneToMany(mappedBy = "testBatch", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference("batch-reports")
    @ToString.Exclude
    @Builder.Default
    private List<Report> consolidatedReports = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Initialize batch with unique ID
     */
    @PrePersist
    public void prePersist() {
        if (this.batchId == null) {
            this.batchId = UUID.randomUUID().toString();
        }
    }

    /**
     * Enum for batch execution status
     */
    public enum BatchStatus {
        PENDING,      // Batch created, waiting to start
        IN_PROGRESS,  // Batch test is running
        COMPLETED,    // All tests completed successfully
        FAILED,       // One or more tests failed
        CANCELLED     // Batch was cancelled
    }
}
