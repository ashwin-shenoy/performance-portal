package com.hamza.durandhar.performance.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.hamza.durandhar.performance.config.JsonbConverter;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TestRun entity representing a performance test execution
 */
@Entity
@Table(name = "test_runs", indexes = {
    @Index(name = "idx_test_run_capability", columnList = "capability_id"),
    @Index(name = "idx_test_run_status", columnList = "status"),
    @Index(name = "idx_test_run_date", columnList = "test_date")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "capability_id", nullable = false)
    @JsonBackReference("capability-testruns")
    @ToString.Exclude
    private Capability capability;

    @Column(name = "test_name", nullable = false, length = 200)
    private String testName;

    @Column(name = "test_date", nullable = false)
    private LocalDateTime testDate;

    @Column(name = "uploaded_by", nullable = false, length = 100)
    private String uploadedBy;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false, length = 20)
    private FileType fileType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TestStatus status;

    @Column(length = 1000)
    private String description;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "total_requests")
    private Long totalRequests;

    @Column(name = "successful_requests")
    private Long successfulRequests;

    @Column(name = "failed_requests")
    private Long failedRequests;

    @Column(name = "avg_response_time")
    private Double avgResponseTime;

    @Column(name = "min_response_time")
    private Double minResponseTime;

    @Column(name = "max_response_time")
    private Double maxResponseTime;

    @Column(name = "percentile_90")
    private Double percentile90;

    @Column(name = "percentile_95")
    private Double percentile95;

    @Column(name = "percentile_99")
    private Double percentile99;

    @Column(name = "throughput")
    private Double throughput;

    @Column(name = "error_rate")
    private Double errorRate;

    @Column(name = "test_duration_seconds")
    private Long testDurationSeconds;

    @Column(name = "virtual_users")
    private Integer virtualUsers;

    @Column(name = "build_number", length = 100)
    private String buildNumber;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /**
     * Capability-specific data stored as JSONB
     * Allows storing different metrics for different capability types.
     *
     * Note: Stored as TEXT in database, application handles JSON serialization
     */
    @Builder.Default
    @Column(name = "capability_specific_data", columnDefinition = "TEXT")
    @Convert(converter = JsonbConverter.class)
    private Map<String, Object> capabilitySpecificData = new HashMap<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder.Default
    @OneToMany(mappedBy = "testRun", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("testrun-reports")
    @ToString.Exclude
    private List<Report> reports = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "testRun", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("testrun-artifacts")
    @ToString.Exclude
    private List<TestArtifact> artifacts = new ArrayList<>();

    // Test cases removed as per master plan
    
    @Builder.Default
    @Column(name = "has_architecture_diagram")
    private Boolean hasArchitectureDiagram = false;

    @Builder.Default
    @Column(name = "has_test_cases_summary")
    private Boolean hasTestCasesSummary = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "test_type", length = 50)
    private TestType testType;

    public enum FileType {
        JTL
    }

    public enum TestStatus {
        UPLOADED,
        PROCESSING,
        COMPLETED,
        FAILED
    }

    public enum TestType {
        JMETER
    }
}

// Made with Bob
