package com.hamza.durandhar.performance.repository;

import com.hamza.durandhar.performance.entity.TestArtifact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for TestArtifact entity
 */
@Repository
public interface TestArtifactRepository extends JpaRepository<TestArtifact, Long> {

    /**
     * Find all artifacts for a specific test run
     */
    List<TestArtifact> findByTestRunId(Long testRunId);

    /**
     * Find artifact by test run ID and artifact type
     */
    Optional<TestArtifact> findByTestRunIdAndArtifactType(Long testRunId, TestArtifact.ArtifactType artifactType);

    /**
     * Find all artifacts of a specific type
     */
    List<TestArtifact> findByArtifactType(TestArtifact.ArtifactType artifactType);

    /**
     * Check if artifact exists for test run and type
     */
    boolean existsByTestRunIdAndArtifactType(Long testRunId, TestArtifact.ArtifactType artifactType);

    /**
     * Delete all artifacts for a test run
     */
    void deleteByTestRunId(Long testRunId);

    /**
     * Count artifacts by type
     */
    @Query("SELECT COUNT(ta) FROM TestArtifact ta WHERE ta.artifactType = :artifactType")
    long countByArtifactType(@Param("artifactType") TestArtifact.ArtifactType artifactType);

    /**
     * Get total file size for a test run
     */
    @Query("SELECT SUM(ta.fileSize) FROM TestArtifact ta WHERE ta.testRun.id = :testRunId")
    Long getTotalFileSizeByTestRunId(@Param("testRunId") Long testRunId);
}

// Made with Bob