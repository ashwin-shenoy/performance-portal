package com.hamza.durandhar.performance.repository;

import com.hamza.durandhar.performance.entity.TestTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for TestTransaction entity
 * Note: TestTransaction stores individual transaction records, not aggregated data
 */
@Repository
public interface TestTransactionRepository extends JpaRepository<TestTransaction, Long> {

    /**
     * Find all transactions for a specific test run
     */
    List<TestTransaction> findByTestRunId(Long testRunId);

    /**
     * Find transactions by test run ID and transaction name
     */
    List<TestTransaction> findByTestRunIdAndTransactionName(Long testRunId, String transactionName);

    /**
     * Find transactions ordered by response time (descending)
     */
    @Query("SELECT t FROM TestTransaction t WHERE t.testRun.id = :testRunId ORDER BY t.responseTime DESC")
    List<TestTransaction> findByTestRunIdOrderByResponseTimeDesc(@Param("testRunId") Long testRunId);

    /**
     * Find transactions with errors (success = false)
     */
    @Query("SELECT t FROM TestTransaction t WHERE t.testRun.id = :testRunId AND t.success = false")
    List<TestTransaction> findTransactionsWithErrors(@Param("testRunId") Long testRunId);

    /**
     * Find transactions by name pattern
     */
    @Query("SELECT t FROM TestTransaction t WHERE t.testRun.id = :testRunId AND t.transactionName LIKE %:pattern%")
    List<TestTransaction> findByTestRunIdAndTransactionNameContaining(
            @Param("testRunId") Long testRunId,
            @Param("pattern") String pattern);

    /**
     * Count transactions for a test run
     */
    long countByTestRunId(Long testRunId);

    /**
     * Count transactions with errors for a test run
     */
    @Query("SELECT COUNT(t) FROM TestTransaction t WHERE t.testRun.id = :testRunId AND t.success = false")
    long countTransactionsWithErrors(@Param("testRunId") Long testRunId);

    /**
     * Delete all transactions for a test run
     */
    void deleteByTestRunId(Long testRunId);

    /**
     * Find transactions with response time above threshold
     */
    @Query("SELECT t FROM TestTransaction t WHERE t.testRun.id = :testRunId AND t.responseTime > :threshold")
    List<TestTransaction> findTransactionsAboveResponseTimeThreshold(
            @Param("testRunId") Long testRunId,
            @Param("threshold") Long threshold);

    /**
     * Get distinct transaction names for a test run
     */
    @Query("SELECT DISTINCT t.transactionName FROM TestTransaction t WHERE t.testRun.id = :testRunId")
    List<String> findDistinctTransactionNamesByTestRunId(@Param("testRunId") Long testRunId);

    /**
     * Get aggregated statistics for a specific transaction name
     * Returns: [transactionName, count, avgResponseTime, minResponseTime, maxResponseTime, errorCount]
     */
    @Query("SELECT t.transactionName, COUNT(t), AVG(t.responseTime), MIN(t.responseTime), MAX(t.responseTime), " +
           "SUM(CASE WHEN t.success = false THEN 1 ELSE 0 END) " +
           "FROM TestTransaction t WHERE t.testRun.id = :testRunId AND t.transactionName = :transactionName " +
           "GROUP BY t.transactionName")
    Object[] getTransactionStatistics(@Param("testRunId") Long testRunId, @Param("transactionName") String transactionName);

    /**
     * Get aggregated statistics for all transactions grouped by name
     * Returns list of: [transactionName, count, avgResponseTime, minResponseTime, maxResponseTime, errorCount]
     */
    @Query("SELECT t.transactionName, COUNT(t), AVG(t.responseTime), MIN(t.responseTime), MAX(t.responseTime), " +
           "SUM(CASE WHEN t.success = false THEN 1 ELSE 0 END) " +
           "FROM TestTransaction t WHERE t.testRun.id = :testRunId " +
           "GROUP BY t.transactionName " +
           "ORDER BY AVG(t.responseTime) DESC")
    List<Object[]> getAggregatedTransactionStatistics(@Param("testRunId") Long testRunId);
}

// Made with Bob