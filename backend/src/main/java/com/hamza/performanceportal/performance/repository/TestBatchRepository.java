package com.hamza.performanceportal.performance.repository;

import com.hamza.performanceportal.performance.entity.TestBatch;
import com.hamza.performanceportal.performance.entity.Capability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for TestBatch entity operations
 */
@Repository
public interface TestBatchRepository extends JpaRepository<TestBatch, Long> {

    /**
     * Find a batch by its unique batch ID
     */
    Optional<TestBatch> findByBatchId(String batchId);

    /**
     * Find all batches for a capability
     */
    List<TestBatch> findByCapability(Capability capability);

    /**
     * Find all batches for a capability with pagination/filtering
     */
    @Query("SELECT tb FROM TestBatch tb WHERE tb.capability.id = :capabilityId ORDER BY tb.createdAt DESC")
    List<TestBatch> findByCapabilityId(@Param("capabilityId") Long capabilityId);

    /**
     * Find batches by status
     */
    List<TestBatch> findByStatus(TestBatch.BatchStatus status);

    /**
     * Find batches created within a date range
     */
    @Query("SELECT tb FROM TestBatch tb WHERE tb.createdAt BETWEEN :startDate AND :endDate ORDER BY tb.createdAt DESC")
    List<TestBatch> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Get count of batches by status
     */
    long countByStatus(TestBatch.BatchStatus status);

    /**
     * Get count of batches by capability
     */
    long countByCapability(Capability capability);
}
