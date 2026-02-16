package com.hamza.durandhar.performance.entity;

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
 * Capability entity representing durandhar capabilities
 */
@Entity
@Table(name = "capabilities")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Capability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "test_objective", columnDefinition = "TEXT")
    private String testObjective;

    @Column(name = "test_scope", columnDefinition = "TEXT")
    private String testScope;

    @Column(name = "environment_details", columnDefinition = "TEXT")
    private String environmentDetails;

    @Builder.Default
    @Column(name = "acceptance_criteria", columnDefinition = "TEXT")
    @Convert(converter = JsonbConverter.class)
    private Map<String, Object> acceptanceCriteria = new HashMap<>();

    @Column(name = "architecture_diagram_path", length = 500)
    private String architectureDiagramPath;

    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder.Default
    @OneToMany(mappedBy = "capability", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("capability-testruns")
    @ToString.Exclude
    private List<TestRun> testRuns = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "capability", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("capability-testcases")
    @ToString.Exclude
    private List<CapabilityTestCase> testCases = new ArrayList<>();
}

// Made with Bob
