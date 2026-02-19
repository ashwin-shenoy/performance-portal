package com.hamza.performanceportal.performance.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing a test case defined for a capability.
 * Each capability can have multiple test cases that describe
 * what performance scenarios are tested.
 */
@Entity
@Table(name = "capability_test_cases", indexes = {
    @Index(name = "idx_ctc_capability", columnList = "capability_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CapabilityTestCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "capability_id", nullable = false)
    @JsonBackReference("capability-testcases")
    @ToString.Exclude
    private Capability capability;

    @Column(name = "test_case_name", nullable = false, length = 200)
    private String testCaseName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "expected_behavior", columnDefinition = "TEXT")
    private String expectedBehavior;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    private Priority priority;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum Priority {
        CRITICAL,
        HIGH,
        MEDIUM,
        LOW
    }
}
