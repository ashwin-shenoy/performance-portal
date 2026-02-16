package com.hamza.durandhar.performance.service;

import com.hamza.durandhar.performance.entity.TestRun;
import com.hamza.durandhar.performance.entity.TestTransaction;
import com.hamza.durandhar.performance.entity.TestMetric;
import com.hamza.durandhar.performance.repository.TestRunRepository;
import com.hamza.durandhar.performance.repository.TestTransactionRepository;
import com.hamza.durandhar.performance.repository.TestMetricRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for fetching and organizing test data
 * Similar to reference implementation's FetchDataMigratedImpl
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TestDataFetchService {

    private final TestRunRepository testRunRepository;
    private final TestTransactionRepository testTransactionRepository;
    private final TestMetricRepository testMetricRepository;

    /**
     * Get test results organized by workload and transaction
     * Similar to reference getTestResults method
     * 
     * @param testRunId Test run ID
     * @return Map of workload name to WorkloadData
     */
    public Map<String, WorkloadData> getTestResults(Long testRunId) {
        log.info("Fetching test results for test run ID: {}", testRunId);

        TestRun testRun = testRunRepository.findById(testRunId)
                .orElseThrow(() -> new RuntimeException("Test run not found: " + testRunId));

        List<TestTransaction> transactions = testTransactionRepository.findByTestRunId(testRunId);
        List<TestMetric> metrics = testMetricRepository.findByTestRunId(testRunId);

        Map<String, WorkloadData> results = new HashMap<>();

        // Group transactions by workload (using test name as workload name)
        String workloadName = testRun.getTestName();
        WorkloadData workload = new WorkloadData();
        workload.setWorkloadName(workloadName);
        workload.setRunId(testRunId.toString());
        workload.setConcurrentUsers(testRun.getVirtualUsers() != null ? testRun.getVirtualUsers().toString() : "0");
        workload.setTestStatus(testRun.getStatus().toString());
        workload.setTestDuration(testRun.getTestDurationSeconds() != null ? testRun.getTestDurationSeconds().toString() : "0");

        // Add resource utilization from metrics
        Map<String, String> resourceUtilization = new HashMap<>();
        for (TestMetric metric : metrics) {
            String metricName = metric.getMetricName();
            String metricValue = String.valueOf(metric.getMetricValue());
            
            if (metricName.toUpperCase().contains("CPU")) {
                resourceUtilization.put("CPU", metricValue);
            } else if (metricName.toUpperCase().contains("MEMORY")) {
                resourceUtilization.put("MEMORY", metricValue);
            } else if (metricName.toUpperCase().contains("DISK")) {
                if (metricName.toUpperCase().contains("READ")) {
                    resourceUtilization.put("DISKREAD", metricValue);
                } else if (metricName.toUpperCase().contains("WRITE")) {
                    resourceUtilization.put("DISKWRITE", metricValue);
                }
            } else if (metricName.toUpperCase().contains("NETWORK") || metricName.toUpperCase().contains("NET")) {
                if (metricName.toUpperCase().contains("SEND")) {
                    resourceUtilization.put("NETSEND", metricValue);
                } else if (metricName.toUpperCase().contains("RECV")) {
                    resourceUtilization.put("NETRECV", metricValue);
                }
            }
        }
        workload.setResourceUtilization(resourceUtilization);

        // Add transactions
        Map<String, TransactionData> transactionMap = new HashMap<>();
        for (TestTransaction transaction : transactions) {
            String transactionName = transaction.getTransactionName();
            if (transactionName == null || transactionName.isEmpty()) {
                transactionName = "None";
            }

            if (!transactionMap.containsKey(transactionName)) {
                TransactionData transData = new TransactionData();
                transData.setTransactionName(transactionName);
                transData.setTransactionsPassed(transaction.getSuccess() ? 1 : 0);
                transData.setTransactionsFailed(transaction.getSuccess() ? 0 : 1);
                transData.setThroughput(calculateThroughput(transaction));
                transData.setResponseTimeSec(transaction.getResponseTime() / 1000.0); // Convert ms to seconds
                transData.setTransactionStatus(transaction.getSuccess() ? "PASSED" : "FAILED");
                
                transactionMap.put(transactionName, transData);
            } else {
                // Aggregate if duplicate transaction name
                TransactionData existing = transactionMap.get(transactionName);
                if (transaction.getSuccess()) {
                    existing.setTransactionsPassed(existing.getTransactionsPassed() + 1);
                } else {
                    existing.setTransactionsFailed(existing.getTransactionsFailed() + 1);
                }
            }
        }
        workload.setTransactionMap(transactionMap);

        results.put(workloadName, workload);
        return results;
    }

    /**
     * Calculate throughput for a transaction
     */
    private double calculateThroughput(TestTransaction transaction) {
        // Simple throughput calculation - can be enhanced
        return 1.0; // Placeholder
    }

    /**
     * WorkloadData class to hold workload information
     * Similar to reference Workload class
     */
    public static class WorkloadData {
        private String workloadName;
        private String runId;
        private String retryId;
        private String testExecutionDate;
        private String testDuration;
        private String concurrentUsers;
        private String testStatus;
        private Map<String, String> resourceUtilization = new HashMap<>();
        private Map<String, TransactionData> transactionMap = new HashMap<>();
        private int isBenchmark;
        private String productBuildNumber;
        private String installedIntFixes;

        // Getters and setters
        public String getWorkloadName() { return workloadName; }
        public void setWorkloadName(String workloadName) { this.workloadName = workloadName; }
        
        public String getRunId() { return runId; }
        public void setRunId(String runId) { this.runId = runId; }
        
        public String getRetryId() { return retryId; }
        public void setRetryId(String retryId) { this.retryId = retryId; }
        
        public String getTestExecutionDate() { return testExecutionDate; }
        public void setTestExecutionDate(String testExecutionDate) { this.testExecutionDate = testExecutionDate; }
        
        public String getTestDuration() { return testDuration; }
        public void setTestDuration(String testDuration) { this.testDuration = testDuration; }
        
        public String getConcurrentUsers() { return concurrentUsers; }
        public void setConcurrentUsers(String concurrentUsers) { this.concurrentUsers = concurrentUsers; }
        
        public String getTestStatus() { return testStatus; }
        public void setTestStatus(String testStatus) { this.testStatus = testStatus; }
        
        public Map<String, String> getResourceUtilization() { return resourceUtilization; }
        public void setResourceUtilization(Map<String, String> resourceUtilization) { this.resourceUtilization = resourceUtilization; }
        
        public Map<String, TransactionData> getTransactionMap() { return transactionMap; }
        public void setTransactionMap(Map<String, TransactionData> transactionMap) { this.transactionMap = transactionMap; }
        
        public int getIsBenchmark() { return isBenchmark; }
        public void setIsBenchmark(int isBenchmark) { this.isBenchmark = isBenchmark; }
        
        public String getProductBuildNumber() { return productBuildNumber; }
        public void setProductBuildNumber(String productBuildNumber) { this.productBuildNumber = productBuildNumber; }
        
        public String getInstalledIntFixes() { return installedIntFixes; }
        public void setInstalledIntFixes(String installedIntFixes) { this.installedIntFixes = installedIntFixes; }
    }

    /**
     * TransactionData class to hold transaction information
     * Similar to reference Transaction class
     */
    public static class TransactionData {
        private String transactionName;
        private int transactionsPassed;
        private int transactionsFailed;
        private double throughput;
        private double responseTimeSec;
        private String transactionStatus;

        // Getters and setters
        public String getTransactionName() { return transactionName; }
        public void setTransactionName(String transactionName) { this.transactionName = transactionName; }
        
        public int getTransactionsPassed() { return transactionsPassed; }
        public void setTransactionsPassed(int transactionsPassed) { this.transactionsPassed = transactionsPassed; }
        
        public int getTransactionsFailed() { return transactionsFailed; }
        public void setTransactionsFailed(int transactionsFailed) { this.transactionsFailed = transactionsFailed; }
        
        public double getThroughput() { return throughput; }
        public void setThroughput(double throughput) { this.throughput = throughput; }
        
        public double getResponseTimeSec() { return responseTimeSec; }
        public void setResponseTimeSec(double responseTimeSec) { this.responseTimeSec = responseTimeSec; }
        
        public String getTransactionStatus() { return transactionStatus; }
        public void setTransactionStatus(String transactionStatus) { this.transactionStatus = transactionStatus; }
    }
}

// Made with Bob