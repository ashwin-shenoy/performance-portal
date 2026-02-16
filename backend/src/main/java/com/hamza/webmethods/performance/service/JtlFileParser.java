package com.hamza.durandhar.performance.service;

import com.hamza.durandhar.performance.entity.CapabilityTestCase;
import com.hamza.durandhar.performance.entity.TestRun;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * Service for parsing JMeter JTL files (both XML and CSV formats)
 * Calculates aggregated metrics only (like JMeter HTML report)
 * Does NOT store individual transactions - only summary statistics
 * Generates diagrams during parsing before discarding transaction data
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JtlFileParser {

    private final JtlDiagramGenerator jtlDiagramGenerator;

    /**
     * Parse JTL file, generate diagrams, and calculate aggregated metrics
     * Does NOT store individual transactions - only summary statistics and diagram files
     */
    @Transactional
    public ParseResult parseAndSave(TestRun testRun, MultipartFile file) throws Exception {
        log.info("Parsing JTL file: {}", file.getOriginalFilename());

        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            throw new IllegalArgumentException("File name is null");
        }

        // Parse JTL file and collect transaction data temporarily for diagram generation
        ParsedData parsedData;
        
        // Detect format by reading first few bytes
        BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
        String firstLine = reader.readLine();
        reader.close();

        if (firstLine != null && firstLine.trim().startsWith("<?xml")) {
            // XML format
            parsedData = parseXmlJtl(file);
        } else {
            // CSV format
            parsedData = parseCsvJtl(file);
        }

        // Generate diagrams from transaction data before discarding it
        JtlDiagramGenerator.DiagramGenerationResult diagramResult = null;
        try {
            log.info("Generating diagrams for test run {}", testRun.getId());
            diagramResult = jtlDiagramGenerator.generateAllDiagrams(testRun, parsedData.getTransactionData());
            log.info("Generated {} diagrams", diagramResult.getDiagramCount());
        } catch (Exception e) {
            log.error("Error generating diagrams: {}", e.getMessage(), e);
            // Continue with parsing even if diagram generation fails
        }

        // Calculate aggregated metrics from transaction data
        AggregatedMetrics metrics = calculateAggregatedMetrics(parsedData.getTransactionData());

        // Calculate per-label statistics for detailed reporting
        Map<String, LabelStatistics> labelStats = calculatePerLabelStatistics(parsedData.getTransactionData());

        // Update test run with aggregated statistics only
        updateTestRunWithAggregates(testRun, metrics, labelStats);

        // Transaction data is now discarded (garbage collected)
        log.info("JTL parsing complete: {} total requests processed, {} labels found, aggregated metrics calculated",
                metrics.getTotalRequests(), labelStats.size());

        return new ParseResult(
            (int) metrics.getTotalRequests(),
            0,
            diagramResult != null ? diagramResult.getDiagramCount() : 0,
            diagramResult != null ? diagramResult.getDiagramDirectory() : null
        );
    }

    /**
     * Parse XML format JTL file - creates temporary transaction objects for diagram generation
     */
    private ParsedData parseXmlJtl(MultipartFile file) throws Exception {
        List<TransactionData> transactions = new ArrayList<>();
        
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(file.getInputStream());
        doc.getDocumentElement().normalize();

        NodeList sampleNodes = doc.getElementsByTagName("httpSample");
        if (sampleNodes.getLength() == 0) {
            sampleNodes = doc.getElementsByTagName("sample");
        }

        log.info("Found {} samples in XML JTL", sampleNodes.getLength());

        for (int i = 0; i < sampleNodes.getLength(); i++) {
            Element sample = (Element) sampleNodes.item(i);
            
            try {
                TransactionData transaction = new TransactionData();
                transaction.setTransactionName(sample.getAttribute("lb"));
                transaction.setTimestamp(parseTimestamp(sample.getAttribute("ts")));
                transaction.setResponseTime(Long.parseLong(sample.getAttribute("t")));
                transaction.setLatency(parseLongOrNull(sample.getAttribute("lt")));
                transaction.setConnectTime(parseLongOrNull(sample.getAttribute("ct")));
                transaction.setStatusCode(parseIntOrNull(sample.getAttribute("rc")));
                transaction.setSuccess("true".equals(sample.getAttribute("s")));
                transaction.setErrorMessage(sample.getAttribute("rm"));
                transaction.setThreadName(sample.getAttribute("tn"));
                transaction.setBytesSent(parseLongOrNull(sample.getAttribute("by")));
                transaction.setBytesReceived(parseLongOrNull(sample.getAttribute("ng")));

                transactions.add(transaction);
            } catch (Exception e) {
                log.warn("Error parsing sample at index {}: {}", i, e.getMessage());
            }
        }

        return new ParsedData(transactions);
    }

    /**
     * Parse CSV format JTL file - creates temporary transaction objects for diagram generation
     */
    private ParsedData parseCsvJtl(MultipartFile file) throws Exception {
        List<TransactionData> transactions = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IllegalArgumentException("Empty JTL file");
            }

            // Parse header to get column indices
            String[] headers = headerLine.split(",");
            Map<String, Integer> columnMap = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                columnMap.put(headers[i].trim(), i);
            }

            log.info("CSV JTL columns: {}", String.join(", ", headers));

            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                try {
                    String[] values = line.split(",", -1);
                    
                    TransactionData transaction = new TransactionData();
                    transaction.setTransactionName(getValue(values, columnMap, "label"));
                    transaction.setTimestamp(parseTimestamp(getValue(values, columnMap, "timeStamp")));
                    transaction.setResponseTime(parseLong(getValue(values, columnMap, "elapsed")));
                    transaction.setLatency(parseLongOrNull(getValue(values, columnMap, "Latency")));
                    transaction.setConnectTime(parseLongOrNull(getValue(values, columnMap, "Connect")));
                    transaction.setStatusCode(parseIntOrNull(getValue(values, columnMap, "responseCode")));
                    transaction.setSuccess("true".equals(getValue(values, columnMap, "success")));
                    transaction.setErrorMessage(getValue(values, columnMap, "responseMessage"));
                    transaction.setThreadName(getValue(values, columnMap, "threadName"));
                    transaction.setBytesSent(parseLongOrNull(getValue(values, columnMap, "bytes")));
                    transaction.setBytesReceived(parseLongOrNull(getValue(values, columnMap, "sentBytes")));

                    transactions.add(transaction);
                } catch (Exception e) {
                    log.warn("Error parsing line {}: {}", lineNumber, e.getMessage());
                }
            }
        }

        log.info("Parsed {} transactions from CSV JTL", transactions.size());
        return new ParsedData(transactions);
    }

    /**
     * Calculate aggregated metrics from transaction data
     */
    private AggregatedMetrics calculateAggregatedMetrics(List<TransactionData> transactions) {
        AggregatedMetrics metrics = new AggregatedMetrics();
        
        if (transactions.isEmpty()) {
            return metrics;
        }

        List<Long> responseTimes = new ArrayList<>();
        List<Long> latencies = new ArrayList<>();
        List<LocalDateTime> timestamps = new ArrayList<>();
        
        for (TransactionData transaction : transactions) {
            responseTimes.add(transaction.getResponseTime());
            timestamps.add(transaction.getTimestamp());
            
            if (transaction.isSuccess()) {
                metrics.incrementSuccessCount();
            } else {
                metrics.incrementFailureCount();
            }
            
            if (transaction.getLatency() != null) {
                latencies.add(transaction.getLatency());
            }
            
            if (transaction.getBytesSent() != null) {
                metrics.addBytesSent(transaction.getBytesSent());
            }
            if (transaction.getBytesReceived() != null) {
                metrics.addBytesReceived(transaction.getBytesReceived());
            }
        }
        
        // Sort response times for percentile calculations
        Collections.sort(responseTimes);
        
        // Response time statistics
        metrics.setAvgResponseTime(calculateAverage(responseTimes));
        metrics.setMinResponseTime(responseTimes.get(0).doubleValue());
        metrics.setMaxResponseTime(responseTimes.get(responseTimes.size() - 1).doubleValue());
        metrics.setMedianResponseTime(calculatePercentile(responseTimes, 50));
        metrics.setPercentile90(calculatePercentile(responseTimes, 90));
        metrics.setPercentile95(calculatePercentile(responseTimes, 95));
        metrics.setPercentile99(calculatePercentile(responseTimes, 99));
        
        // Latency statistics
        if (!latencies.isEmpty()) {
            Collections.sort(latencies);
            metrics.setAvgLatency(calculateAverage(latencies));
            metrics.setPercentile95Latency(calculatePercentile(latencies, 95));
        }
        
        // Test duration and throughput
        if (!timestamps.isEmpty()) {
            LocalDateTime minTime = timestamps.stream().min(LocalDateTime::compareTo).orElse(LocalDateTime.now());
            LocalDateTime maxTime = timestamps.stream().max(LocalDateTime::compareTo).orElse(LocalDateTime.now());
            long durationSeconds = java.time.Duration.between(minTime, maxTime).getSeconds();
            
            metrics.setTestDurationSeconds(durationSeconds);
            if (durationSeconds > 0) {
                metrics.setThroughput((double) responseTimes.size() / durationSeconds);
            }
        }
        
        // Error rate
        long total = metrics.getTotalRequests();
        if (total > 0) {
            metrics.setErrorRate((metrics.getFailedRequests() * 100.0) / total);
        }
        
        return metrics;
    }

    /**
     * Calculate per-label statistics for detailed reporting
     */
    private Map<String, LabelStatistics> calculatePerLabelStatistics(List<TransactionData> transactions) {
        Map<String, LabelStatistics> labelStatsMap = new LinkedHashMap<>();
        
        // Group transactions by label
        Map<String, List<TransactionData>> groupedByLabel = transactions.stream()
            .collect(java.util.stream.Collectors.groupingBy(TransactionData::getTransactionName));
        
        // Calculate statistics for each label
        for (Map.Entry<String, List<TransactionData>> entry : groupedByLabel.entrySet()) {
            String label = entry.getKey();
            List<TransactionData> labelTransactions = entry.getValue();
            
            LabelStatistics stats = new LabelStatistics();
            stats.setLabel(label);
            stats.setSamples(labelTransactions.size());
            
            // Count failures
            long failures = labelTransactions.stream().filter(t -> !t.isSuccess()).count();
            stats.setFailures((int) failures);
            stats.setErrorPercent(stats.getSamples() > 0 ? (failures * 100.0 / stats.getSamples()) : 0.0);
            
            // Response time statistics
            List<Long> responseTimes = labelTransactions.stream()
                .map(TransactionData::getResponseTime)
                .sorted()
                .collect(java.util.stream.Collectors.toList());
            
            if (!responseTimes.isEmpty()) {
                stats.setAverage(responseTimes.stream().mapToLong(Long::longValue).average().orElse(0.0));
                stats.setMin(responseTimes.get(0));
                stats.setMax(responseTimes.get(responseTimes.size() - 1));
                stats.setMedian(calculatePercentile(responseTimes, 50));
                stats.setPercentile90(calculatePercentile(responseTimes, 90));
                stats.setPercentile95(calculatePercentile(responseTimes, 95));
                stats.setPercentile99(calculatePercentile(responseTimes, 99));
            }
            
            // Calculate throughput
            if (!labelTransactions.isEmpty()) {
                LocalDateTime firstTimestamp = labelTransactions.stream()
                    .map(TransactionData::getTimestamp)
                    .min(LocalDateTime::compareTo)
                    .orElse(LocalDateTime.now());
                LocalDateTime lastTimestamp = labelTransactions.stream()
                    .map(TransactionData::getTimestamp)
                    .max(LocalDateTime::compareTo)
                    .orElse(LocalDateTime.now());
                
                long durationSeconds = java.time.Duration.between(firstTimestamp, lastTimestamp).getSeconds();
                stats.setThroughput(durationSeconds > 0 ? (double) stats.getSamples() / durationSeconds : 0.0);
            }
            
            // Network data
            long totalBytesSent = labelTransactions.stream()
                .filter(t -> t.getBytesSent() != null)
                .mapToLong(TransactionData::getBytesSent)
                .sum();
            long totalBytesReceived = labelTransactions.stream()
                .filter(t -> t.getBytesReceived() != null)
                .mapToLong(TransactionData::getBytesReceived)
                .sum();
            
            stats.setReceivedKBPerSec(stats.getSamples() > 0 && totalBytesReceived > 0 ?
                (totalBytesReceived / 1024.0) / (stats.getSamples() / stats.getThroughput()) : 0.0);
            stats.setSentKBPerSec(stats.getSamples() > 0 && totalBytesSent > 0 ?
                (totalBytesSent / 1024.0) / (stats.getSamples() / stats.getThroughput()) : 0.0);
            
            labelStatsMap.put(label, stats);
        }
        
        // Calculate "Total" row
        if (!labelStatsMap.isEmpty()) {
            LabelStatistics totalStats = new LabelStatistics();
            totalStats.setLabel("Total");
            totalStats.setSamples(transactions.size());
            
            long totalFailures = transactions.stream().filter(t -> !t.isSuccess()).count();
            totalStats.setFailures((int) totalFailures);
            totalStats.setErrorPercent(totalStats.getSamples() > 0 ?
                (totalFailures * 100.0 / totalStats.getSamples()) : 0.0);
            
            List<Long> allResponseTimes = transactions.stream()
                .map(TransactionData::getResponseTime)
                .sorted()
                .collect(java.util.stream.Collectors.toList());
            
            if (!allResponseTimes.isEmpty()) {
                totalStats.setAverage(allResponseTimes.stream().mapToLong(Long::longValue).average().orElse(0.0));
                totalStats.setMin(allResponseTimes.get(0));
                totalStats.setMax(allResponseTimes.get(allResponseTimes.size() - 1));
                totalStats.setMedian(calculatePercentile(allResponseTimes, 50));
                totalStats.setPercentile90(calculatePercentile(allResponseTimes, 90));
                totalStats.setPercentile95(calculatePercentile(allResponseTimes, 95));
                totalStats.setPercentile99(calculatePercentile(allResponseTimes, 99));
            }
            
            if (!transactions.isEmpty()) {
                LocalDateTime firstTimestamp = transactions.stream()
                    .map(TransactionData::getTimestamp)
                    .min(LocalDateTime::compareTo)
                    .orElse(LocalDateTime.now());
                LocalDateTime lastTimestamp = transactions.stream()
                    .map(TransactionData::getTimestamp)
                    .max(LocalDateTime::compareTo)
                    .orElse(LocalDateTime.now());
                
                long durationSeconds = java.time.Duration.between(firstTimestamp, lastTimestamp).getSeconds();
                totalStats.setThroughput(durationSeconds > 0 ? (double) totalStats.getSamples() / durationSeconds : 0.0);
            }
            
            long totalBytesSent = transactions.stream()
                .filter(t -> t.getBytesSent() != null)
                .mapToLong(TransactionData::getBytesSent)
                .sum();
            long totalBytesReceived = transactions.stream()
                .filter(t -> t.getBytesReceived() != null)
                .mapToLong(TransactionData::getBytesReceived)
                .sum();
            
            totalStats.setReceivedKBPerSec(totalStats.getSamples() > 0 && totalBytesReceived > 0 ?
                (totalBytesReceived / 1024.0) / (totalStats.getSamples() / totalStats.getThroughput()) : 0.0);
            totalStats.setSentKBPerSec(totalStats.getSamples() > 0 && totalBytesSent > 0 ?
                (totalBytesSent / 1024.0) / (totalStats.getSamples() / totalStats.getThroughput()) : 0.0);
            
            // Put Total at the beginning
            Map<String, LabelStatistics> orderedMap = new LinkedHashMap<>();
            orderedMap.put("Total", totalStats);
            orderedMap.putAll(labelStatsMap);
            return orderedMap;
        }
        
        return labelStatsMap;
    }

    /**
     * Update test run with aggregated metrics and per-label statistics
     */
    private void updateTestRunWithAggregates(TestRun testRun, AggregatedMetrics metrics, Map<String, LabelStatistics> labelStats) {
        testRun.setTotalRequests(metrics.getTotalRequests());
        testRun.setSuccessfulRequests(metrics.getSuccessfulRequests());
        testRun.setFailedRequests(metrics.getFailedRequests());
        testRun.setAvgResponseTime(metrics.getAvgResponseTime());
        testRun.setMinResponseTime(metrics.getMinResponseTime());
        testRun.setMaxResponseTime(metrics.getMaxResponseTime());
        testRun.setPercentile90(metrics.getPercentile90());
        testRun.setPercentile95(metrics.getPercentile95());
        testRun.setPercentile99(metrics.getPercentile99());
        testRun.setErrorRate(metrics.getErrorRate());
        testRun.setTestDurationSeconds(metrics.getTestDurationSeconds());
        testRun.setThroughput(metrics.getThroughput());
        
        // Store per-label statistics in capability-specific data (merge by label/test case)
        Map<String, LabelStatistics> matchedLabelStats = filterLabelStatsToTestCases(labelStats, testRun);
        if (!matchedLabelStats.isEmpty()) {
            Map<String, Object> capabilityData = testRun.getCapabilitySpecificData();
            if (capabilityData == null) {
                capabilityData = new HashMap<>();
            }

            Map<String, Object> existingLabelStats = capabilityData.get("labelStatistics") instanceof Map
                    ? new LinkedHashMap<>((Map<String, Object>) capabilityData.get("labelStatistics"))
                    : new LinkedHashMap<>();

            for (Map.Entry<String, LabelStatistics> entry : matchedLabelStats.entrySet()) {
                existingLabelStats.put(entry.getKey(), entry.getValue());
            }
            capabilityData.put("labelStatistics", existingLabelStats);

            Map<String, Object> baselineEvaluation = evaluateBaseline(testRun, matchedLabelStats);
            if (!baselineEvaluation.isEmpty()) {
                Map<String, Object> mergedBaseline = new LinkedHashMap<>();
                Map<String, Object> existingBaseline = capabilityData.get("baselineEvaluation") instanceof Map
                        ? (Map<String, Object>) capabilityData.get("baselineEvaluation")
                        : new LinkedHashMap<>();
                Map<String, Object> existingResults = existingBaseline.get("results") instanceof Map
                        ? new LinkedHashMap<>((Map<String, Object>) existingBaseline.get("results"))
                        : new LinkedHashMap<>();
                Map<String, Object> newResults = baselineEvaluation.get("results") instanceof Map
                        ? (Map<String, Object>) baselineEvaluation.get("results")
                        : new LinkedHashMap<>();

                existingResults.putAll(newResults);
                mergedBaseline.put("baseline", baselineEvaluation.get("baseline"));
                mergedBaseline.put("results", existingResults);
                capabilityData.put("baselineEvaluation", mergedBaseline);
            }

            testRun.setCapabilitySpecificData(capabilityData);
            log.info("Stored statistics for {} labels in capability-specific data", matchedLabelStats.size());
        }
    }

    private Map<String, LabelStatistics> filterLabelStatsToTestCases(
            Map<String, LabelStatistics> labelStats,
            TestRun testRun
    ) {
        if (labelStats == null || labelStats.isEmpty() || testRun.getCapability() == null) {
            return new LinkedHashMap<>();
        }

        List<String> testCaseNames = testRun.getCapability().getTestCases().stream()
                .map(CapabilityTestCase::getTestCaseName)
                .filter(Objects::nonNull)
                .map(name -> name.trim().toLowerCase())
                .toList();

        if (testCaseNames.isEmpty()) {
            return new LinkedHashMap<>();
        }

        Map<String, LabelStatistics> filtered = new LinkedHashMap<>();
        for (Map.Entry<String, LabelStatistics> entry : labelStats.entrySet()) {
            String normalized = entry.getKey() == null ? "" : entry.getKey().trim().toLowerCase();
            if (testCaseNames.contains(normalized)) {
                filtered.put(entry.getKey(), entry.getValue());
            }
        }
        return filtered;
    }

    private Map<String, Object> evaluateBaseline(TestRun testRun, Map<String, LabelStatistics> labelStats) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (testRun.getCapability() == null || testRun.getCapability().getAcceptanceCriteria() == null) {
            return result;
        }

        Object baselineObj = testRun.getCapability().getAcceptanceCriteria().get("baseline");
        if (!(baselineObj instanceof Map)) {
            return result;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> baseline = (Map<String, Object>) baselineObj;
        double p95Max = toDouble(baseline.get("p95MaxMs"));
        double avgMax = toDouble(baseline.get("avgMaxMs"));
        double p90Max = toDouble(baseline.get("p90MaxMs"));
        double throughputMin = toDouble(baseline.get("throughputMin"));

        Map<String, Object> config = new HashMap<>();
        config.put("p95MaxMs", p95Max);
        config.put("avgMaxMs", avgMax);
        config.put("p90MaxMs", p90Max);
        config.put("throughputMin", throughputMin);
        result.put("baseline", config);

        Map<String, Object> labelResults = new LinkedHashMap<>();
        for (Map.Entry<String, LabelStatistics> entry : labelStats.entrySet()) {
            LabelStatistics stats = entry.getValue();
            Map<String, Object> checks = new HashMap<>();

            boolean p95Pass = p95Max <= 0 || stats.getPercentile95() <= p95Max;
            boolean avgPass = avgMax <= 0 || stats.getAverage() <= avgMax;
            boolean p90Pass = p90Max <= 0 || stats.getPercentile90() <= p90Max;
            boolean throughputPass = throughputMin <= 0 || stats.getThroughput() >= throughputMin;

            checks.put("p95Pass", p95Pass);
            checks.put("avgPass", avgPass);
            checks.put("p90Pass", p90Pass);
            checks.put("throughputPass", throughputPass);

            boolean passed = p95Pass && avgPass && p90Pass && throughputPass;

            Map<String, Object> metrics = new HashMap<>();
            metrics.put("p95", stats.getPercentile95());
            metrics.put("avg", stats.getAverage());
            metrics.put("p90", stats.getPercentile90());
            metrics.put("throughput", stats.getThroughput());

            Map<String, Object> evaluation = new HashMap<>();
            evaluation.put("pass", passed);
            evaluation.put("checks", checks);
            evaluation.put("metrics", metrics);
            labelResults.put(entry.getKey(), evaluation);
        }

        result.put("results", labelResults);
        return result;
    }

    private double toDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value == null) {
            return 0.0;
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    // Helper methods

    private double calculateAverage(List<Long> values) {
        return values.stream().mapToLong(Long::longValue).average().orElse(0.0);
    }

    private double calculatePercentile(List<Long> sortedValues, int percentile) {
        if (sortedValues.isEmpty()) {
            return 0.0;
        }
        int index = (int) Math.ceil(percentile / 100.0 * sortedValues.size()) - 1;
        index = Math.max(0, Math.min(index, sortedValues.size() - 1));
        return sortedValues.get(index).doubleValue();
    }

    private LocalDateTime parseTimestamp(String timestamp) {
        try {
            long millis = Long.parseLong(timestamp);
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault());
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }

    private Long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (Exception e) {
            return 0L;
        }
    }

    private Long parseLongOrNull(String value) {
        try {
            return value != null && !value.isEmpty() ? Long.parseLong(value) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private Integer parseIntOrNull(String value) {
        try {
            return value != null && !value.isEmpty() ? Integer.parseInt(value) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String getValue(String[] values, Map<String, Integer> columnMap, String columnName) {
        Integer index = columnMap.get(columnName);
        if (index != null && index < values.length) {
            return values[index];
        }
        return "";
    }

    /**
     * Temporary transaction data class - used only during parsing for diagram generation
     * Not persisted to database
     */
    @Data
    public static class TransactionData {
        private String transactionName;
        private LocalDateTime timestamp;
        private Long responseTime;
        private Long latency;
        private Long connectTime;
        private Integer statusCode;
        private boolean success;
        private String errorMessage;
        private String threadName;
        private Long bytesSent;
        private Long bytesReceived;
    }

    /**
     * Parsed data container - holds temporary transaction data
     */
    @Data
    public static class ParsedData {
        private final List<TransactionData> transactionData;
        
        public ParsedData(List<TransactionData> transactionData) {
            this.transactionData = transactionData;
        }
    }

    /**
     * Aggregated metrics class - holds summary statistics only
     */
    @Data
    public static class AggregatedMetrics {
        private long successfulRequests = 0;
        private long failedRequests = 0;
        private double avgResponseTime = 0.0;
        private double minResponseTime = 0.0;
        private double maxResponseTime = 0.0;
        private double medianResponseTime = 0.0;
        private double percentile90 = 0.0;
        private double percentile95 = 0.0;
        private double percentile99 = 0.0;
        private double avgLatency = 0.0;
        private double percentile95Latency = 0.0;
        private double errorRate = 0.0;
        private double throughput = 0.0;
        private long testDurationSeconds = 0;
        private long totalBytesSent = 0;
        private long totalBytesReceived = 0;
        
        public void incrementSuccessCount() {
            this.successfulRequests++;
        }
        
        public void incrementFailureCount() {
            this.failedRequests++;
        }
        
        public void addBytesSent(long bytes) {
            this.totalBytesSent += bytes;
        }
        
        public void addBytesReceived(long bytes) {
            this.totalBytesReceived += bytes;
        }
        
        public long getTotalRequests() {
            return successfulRequests + failedRequests;
        }
    }

    /**
     * Parse result class
     */
    public static class ParseResult {
        private final int transactionCount;
        private final int metricCount;
        private final int diagramCount;
        private final String diagramDirectory;

        public ParseResult(int transactionCount, int metricCount, int diagramCount, String diagramDirectory) {
            this.transactionCount = transactionCount;
            this.metricCount = metricCount;
            this.diagramCount = diagramCount;
            this.diagramDirectory = diagramDirectory;
        }

        public int getTransactionCount() {
            return transactionCount;
        }

        public int getMetricCount() {
            return metricCount;
        }
        
        public int getDiagramCount() {
            return diagramCount;
        }
        
        public String getDiagramDirectory() {
            return diagramDirectory;
        }
    }
    
    /**
     * Label statistics class - holds per-label performance metrics
     */
    @Data
    static class LabelStatistics {
        private String label;
        private int samples;
        private int failures;
        private double errorPercent;
        private double average;
        private long min;
        private long max;
        private double median;
        private double percentile90;
        private double percentile95;
        private double percentile99;
        private double throughput;
        private double receivedKBPerSec;
        private double sentKBPerSec;
    }
}

// Made with Bob
