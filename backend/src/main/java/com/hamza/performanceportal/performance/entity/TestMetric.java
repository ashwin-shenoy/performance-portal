package com.hamza.performanceportal.performance.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * TestMetric entity for storing aggregated performance metrics
 */
@Entity
@Table(name = "test_metrics", indexes = {
    @Index(name = "idx_metric_test_run", columnList = "test_run_id"),
    @Index(name = "idx_metric_name", columnList = "metric_name")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_run_id", nullable = false)
    @JsonBackReference
    @ToString.Exclude
    private TestRun testRun;

    @Column(name = "metric_name", nullable = false, length = 100)
    private String metricName;

    @Column(name = "metric_value", nullable = false)
    private Double metricValue;

    @Column(length = 20)
    private String unit;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(length = 500)
    private String description;
}

// Made with Bob
