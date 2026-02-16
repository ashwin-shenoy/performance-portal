package com.hamza.durandhar.performance.repository;

import com.hamza.durandhar.performance.entity.TestMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for TestMetric entity
 */
@Repository
public interface TestMetricRepository extends JpaRepository<TestMetric, Long> {

    /**
     * Find all metrics for a specific test run
     */
    List<TestMetric> findByTestRunId(Long testRunId);

    /**
     * Find metrics by test run ID and metric name
     */
    @Query("SELECT m FROM TestMetric m WHERE m.testRun.id = :testRunId AND m.metricName = :metricName")
    List<TestMetric> findByTestRunIdAndMetricType(@Param("testRunId") Long testRunId, @Param("metricName") String metricName);

    /**
     * Find average metric for a test run
     */
    @Query("SELECT m FROM TestMetric m WHERE m.testRun.id = :testRunId AND m.metricName = 'Average'")
    Optional<TestMetric> findAverageMetricByTestRunId(@Param("testRunId") Long testRunId);

    /**
     * Find metrics with error rate above threshold
     */
    @Query("SELECT m FROM TestMetric m WHERE m.testRun.id = :testRunId AND m.metricName LIKE '%Error%' AND m.metricValue > :threshold")
    List<TestMetric> findMetricsWithHighErrorRate(
            @Param("testRunId") Long testRunId,
            @Param("threshold") Double threshold);

    /**
     * Find metrics with response time above threshold
     */
    @Query("SELECT m FROM TestMetric m WHERE m.testRun.id = :testRunId AND m.metricName LIKE '%Response%' AND m.metricValue > :threshold")
    List<TestMetric> findMetricsWithHighResponseTime(
            @Param("testRunId") Long testRunId,
            @Param("threshold") Double threshold);

    /**
     * Get summary statistics for a test run
     */
    @Query("SELECT MIN(m.metricValue), MAX(m.metricValue), AVG(m.metricValue) " +
           "FROM TestMetric m WHERE m.testRun.id = :testRunId")
    Object[] getSummaryStatistics(@Param("testRunId") Long testRunId);

    /**
     * Delete all metrics for a test run
     */
    void deleteByTestRunId(Long testRunId);

    /**
     * Count metrics for a test run
     */
    long countByTestRunId(Long testRunId);
}

// Made with Bob