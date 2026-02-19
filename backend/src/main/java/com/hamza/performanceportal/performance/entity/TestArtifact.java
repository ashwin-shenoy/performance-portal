package com.hamza.performanceportal.performance.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * TestArtifact entity for storing uploaded files
 * (architecture diagrams, test cases summary, performance data)
 */
@Entity
@Table(name = "test_artifacts", indexes = {
    @Index(name = "idx_artifact_test_run", columnList = "test_run_id"),
    @Index(name = "idx_artifact_type", columnList = "artifact_type")
}, uniqueConstraints = {
    @UniqueConstraint(name = "unique_artifact_per_type", columnNames = {"test_run_id", "artifact_type"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestArtifact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_run_id", nullable = false)
    @JsonBackReference("testrun-artifacts")
    @ToString.Exclude
    private TestRun testRun;

    @Enumerated(EnumType.STRING)
    @Column(name = "artifact_type", nullable = false, length = 50)
    private ArtifactType artifactType;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "file_type", nullable = false, length = 50)
    private String fileType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @CreationTimestamp
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    public enum ArtifactType {
        ARCHITECTURE_DIAGRAM,
        TEST_CASES_SUMMARY,
        PERFORMANCE_DATA
    }
}

// Made with Bob
