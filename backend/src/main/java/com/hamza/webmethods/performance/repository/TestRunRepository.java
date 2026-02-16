package com.hamza.durandhar.performance.repository;

import com.hamza.durandhar.performance.entity.TestRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for TestRun entity
 */
@Repository
public interface TestRunRepository extends JpaRepository<TestRun, Long> {

    /**
     * Find test runs by capability ID
     */
    List<TestRun> findByCapabilityId(Long capabilityId);

    /**
     * Find test runs by status
     */
    List<TestRun> findByStatus(TestRun.TestStatus status);

    /**
     * Find test runs by capability ID and status
     */
    List<TestRun> findByCapabilityIdAndStatus(Long capabilityId, TestRun.TestStatus status);

    /**
     * Find test runs by date range
     */
    @Query("SELECT tr FROM TestRun tr WHERE tr.testDate BETWEEN :startDate AND :endDate ORDER BY tr.testDate DESC")
    List<TestRun> findByTestDateBetween(@Param("startDate") LocalDateTime startDate, 
                                        @Param("endDate") LocalDateTime endDate);

    /**
     * Find test runs by capability and date range
     */
    @Query("SELECT tr FROM TestRun tr WHERE tr.capability.id = :capabilityId " +
           "AND tr.testDate BETWEEN :startDate AND :endDate ORDER BY tr.testDate DESC")
    List<TestRun> findByCapabilityIdAndTestDateBetween(@Param("capabilityId") Long capabilityId,
                                                        @Param("startDate") LocalDateTime startDate,
                                                        @Param("endDate") LocalDateTime endDate);

    /**
     * Find test runs by uploaded by user
     */
    List<TestRun> findByUploadedBy(String uploadedBy);

    /**
     * Find test runs by file name
     */
    Optional<TestRun> findByFileName(String fileName);

    /**
     * Find latest test runs for a capability
     */
    @Query("SELECT tr FROM TestRun tr WHERE tr.capability.id = :capabilityId " +
           "ORDER BY tr.testDate DESC")
    List<TestRun> findLatestByCapabilityId(@Param("capabilityId") Long capabilityId);

    /**
     * Find test runs with architecture diagram
     */
    List<TestRun> findByHasArchitectureDiagramTrue();

    /**
     * Find test runs with test cases summary
     */
    List<TestRun> findByHasTestCasesSummaryTrue();

    /**
     * Count test runs by status
     */
    long countByStatus(TestRun.TestStatus status);

    /**
     * Count test runs by capability
     */
    long countByCapabilityId(Long capabilityId);

    /**
     * Find test runs after a specific date
     */
    List<TestRun> findByTestDateAfter(LocalDateTime date);
}

// Made with Bob