package com.hamza.durandhar.performance.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Report entity for storing generated reports
 */
@Entity
@Table(name = "reports", indexes = {
    @Index(name = "idx_report_test_run", columnList = "test_run_id"),
    @Index(name = "idx_report_type", columnList = "report_type")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_run_id", nullable = false)
    @JsonBackReference("testrun-reports")
    @ToString.Exclude
    private TestRun testRun;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false, length = 20)
    private ReportType reportType;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "generated_by", nullable = false, length = 100)
    private String generatedBy;

    @CreationTimestamp
    @Column(name = "generated_at", nullable = false, updatable = false)
    private LocalDateTime generatedAt;

    @Column(length = 500)
    private String description;

    public enum ReportType {
        TECHNICAL_PDF,
        TECHNICAL_HTML,
        TECHNICAL_WORD,
        EXECUTIVE_WORD,
        EXECUTIVE_PDF,
        CAPABILITY_PDF,
        RAW_DATA_CSV
    }
}

// Made with Bob
