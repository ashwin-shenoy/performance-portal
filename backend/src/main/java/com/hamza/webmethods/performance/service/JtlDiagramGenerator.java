package com.hamza.durandhar.performance.service;

import com.hamza.durandhar.performance.entity.TestRun;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for generating performance diagrams from JTL data
 * Creates various charts useful for technical reports
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JtlDiagramGenerator {

    @Value("${app.diagrams.dir:/opt/performance-portal/diagrams}")
    private String diagramsDirectory;

    private static final int CHART_WIDTH = 1200;
    private static final int CHART_HEIGHT = 600;
    private static final Color PRIMARY_COLOR = new Color(0, 123, 255);
    private static final Color SUCCESS_COLOR = new Color(40, 167, 69);
    private static final Color ERROR_COLOR = new Color(220, 53, 69);
    private static final Color WARNING_COLOR = new Color(255, 193, 7);
        private static final Color[] SERIES_COLORS = new Color[] {
            new Color(0, 123, 255),
            new Color(40, 167, 69),
            new Color(255, 193, 7),
            new Color(220, 53, 69),
            new Color(111, 66, 193),
            new Color(23, 162, 184)
        };

    /**
     * Generate all diagrams for a test run from TransactionData (temporary objects)
     */
    public DiagramGenerationResult generateAllDiagrams(TestRun testRun, List<JtlFileParser.TransactionData> transactionData) {
        log.info("Generating diagrams for test run: {}", testRun.getId());

        try {
            // Create diagram directory for this test run
            String diagramDir = createDiagramDirectory(testRun);
            List<String> generatedFiles = new ArrayList<>();

            // Generate various diagrams from temporary transaction data
            // Continue generating remaining diagrams even if one fails
            tryGenerateDiagram(() -> generatedFiles.add(generateResponseTimeOverTimeChart(testRun, transactionData, diagramDir)), "Response Time Over Time");
            tryGenerateDiagram(() -> generatedFiles.add(generateResponseTimeDistributionChart(testRun, transactionData, diagramDir)), "Response Time Distribution");
            tryGenerateDiagram(() -> generatedFiles.add(generateThroughputOverTimeChart(testRun, transactionData, diagramDir)), "Throughput Over Time");
            tryGenerateDiagram(() -> generatedFiles.add(generateErrorRateChart(testRun, transactionData, diagramDir)), "Error Rate");
            tryGenerateDiagram(() -> generatedFiles.add(generateTransactionSummaryChart(testRun, transactionData, diagramDir)), "Transaction Summary");
            tryGenerateDiagram(() -> generatedFiles.add(generatePercentileComparisonChart(testRun, transactionData, diagramDir)), "Percentile Comparison");
            tryGenerateDiagram(() -> generatedFiles.add(generateLatencyAnalysisChart(testRun, transactionData, diagramDir)), "Latency Analysis");
            tryGenerateDiagram(() -> generatedFiles.add(generateThreadActivityChart(testRun, transactionData, diagramDir)), "Thread Activity");
            tryGenerateDiagram(() -> generatedFiles.add(generateErrorsByLabelChart(testRun, transactionData, diagramDir)), "Errors By Label");
            tryGenerateDiagram(() -> generatedFiles.add(generateResponseTimeByLabelOverTimeChart(testRun, transactionData, diagramDir)), "Response Time By Label Over Time");
            tryGenerateDiagram(() -> generatedFiles.add(generateResponseTimePercentilesByLabelChart(testRun, transactionData, diagramDir)), "Response Time Percentiles By Label");
            tryGenerateDiagram(() -> generatedFiles.add(generateThroughputByLabelOverTimeChart(testRun, transactionData, diagramDir)), "Throughput By Label Over Time");
            tryGenerateDiagram(() -> generatedFiles.add(generateSlaThresholdBandChart(testRun, transactionData, diagramDir)), "SLA Threshold Band");

            log.info("Generated {} diagrams for test run {}", generatedFiles.size(), testRun.getId());
            return new DiagramGenerationResult(generatedFiles.size(), generatedFiles, diagramDir);

        } catch (Exception e) {
            log.error("Error generating diagrams for test run {}: {}", testRun.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to generate diagrams: " + e.getMessage(), e);
        }
    }

    /**
     * Create diagram directory for test run
     */
    private String createDiagramDirectory(TestRun testRun) throws IOException {
        Path baseDir = Paths.get(diagramsDirectory);
        if (!Files.exists(baseDir)) {
            Files.createDirectories(baseDir);
        }

        // Create capability-specific directory
        String capabilityName = testRun.getCapability() != null ? 
                testRun.getCapability().getName().replaceAll("[^a-zA-Z0-9]", "_") : "Unknown";
        Path capabilityDir = baseDir.resolve(capabilityName);
        if (!Files.exists(capabilityDir)) {
            Files.createDirectories(capabilityDir);
        }

        // Create test run specific directory
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String dirName = String.format("testrun_%d_%s", testRun.getId(), timestamp);
        Path testRunDir = capabilityDir.resolve(dirName);
        Files.createDirectories(testRunDir);

        log.info("Created diagram directory: {}", testRunDir.toAbsolutePath());
        return testRunDir.toAbsolutePath().toString();
    }

    /**
     * Helper to safely generate a diagram - continues on failure
     */
    private void tryGenerateDiagram(DiagramTask task, String chartName) {
        try {
            task.run();
        } catch (Exception e) {
            log.warn("Failed to generate {} chart: {}", chartName, e.getMessage());
        }
    }

    @FunctionalInterface
    private interface DiagramTask {
        void run() throws Exception;
    }

    /**
     * 1. Response Time Over Time Chart
     * Uses per-second averaging to handle duplicate timestamps and large datasets
     */
    private String generateResponseTimeOverTimeChart(TestRun testRun, List<JtlFileParser.TransactionData> transactions, String outputDir) throws IOException {
        log.info("Generating Response Time Over Time chart...");

        // Aggregate response times per second to avoid duplicate timestamp issues
        Map<Long, List<Long>> responseTimesPerSecond = new TreeMap<>();
        for (JtlFileParser.TransactionData transaction : transactions) {
            long epochSecond = transaction.getTimestamp().atZone(ZoneId.systemDefault()).toEpochSecond();
            responseTimesPerSecond.computeIfAbsent(epochSecond, k -> new ArrayList<>())
                    .add(transaction.getResponseTime());
        }

        XYSeries series = new XYSeries("Avg Response Time");
        long startTime = responseTimesPerSecond.keySet().stream().min(Long::compareTo).orElse(0L);

        for (Map.Entry<Long, List<Long>> entry : responseTimesPerSecond.entrySet()) {
            long relativeTime = entry.getKey() - startTime;
            double avgResponseTime = entry.getValue().stream().mapToLong(Long::longValue).average().orElse(0.0);
            series.add(relativeTime, avgResponseTime);
        }

        XYSeriesCollection dataset = new XYSeriesCollection(series);

        JFreeChart chart = ChartFactory.createXYLineChart(
                "Response Time Over Time (Per-Second Average)",
                "Time (seconds)",
                "Response Time (ms)",
                dataset,
                PlotOrientation.VERTICAL,
                false,
                true,
                false
        );

        customizeXYChart(chart, PRIMARY_COLOR);

        String filename = "01_response_time_over_time.png";
        File outputFile = new File(outputDir, filename);
        ChartUtils.saveChartAsPNG(outputFile, chart, CHART_WIDTH, CHART_HEIGHT);
        log.info("Generated: {}", filename);
        return outputFile.getAbsolutePath();
    }

    /**
     * 2. Response Time Distribution Chart (Histogram)
     */
    private String generateResponseTimeDistributionChart(TestRun testRun, List<JtlFileParser.TransactionData> transactions, String outputDir) throws IOException {
        log.info("Generating Response Time Distribution chart...");

        // Create buckets for response times
        Map<String, Long> distribution = new TreeMap<>();
        int bucketSize = 100; // 100ms buckets

        for (JtlFileParser.TransactionData transaction : transactions) {
            long responseTime = transaction.getResponseTime();
            int bucket = (int) (responseTime / bucketSize) * bucketSize;
            String bucketLabel = bucket + "-" + (bucket + bucketSize) + "ms";
            distribution.merge(bucketLabel, 1L, Long::sum);
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        distribution.forEach((bucket, count) -> dataset.addValue(count, "Requests", bucket));

        JFreeChart chart = ChartFactory.createBarChart(
                "Response Time Distribution",
                "Response Time Range",
                "Number of Requests",
                dataset,
                PlotOrientation.VERTICAL,
                false,
                true,
                false
        );

        customizeBarChart(chart, PRIMARY_COLOR);

        String filename = "02_response_time_distribution.png";
        File outputFile = new File(outputDir, filename);
        ChartUtils.saveChartAsPNG(outputFile, chart, CHART_WIDTH, CHART_HEIGHT);
        log.info("Generated: {}", filename);
        return outputFile.getAbsolutePath();
    }

    /**
     * 3. Throughput Over Time Chart
     */
    private String generateThroughputOverTimeChart(TestRun testRun, List<JtlFileParser.TransactionData> transactions, String outputDir) throws IOException {
        log.info("Generating Throughput Over Time chart...");

        // Calculate throughput per second
        Map<Long, Long> throughputPerSecond = new TreeMap<>();
        
        for (JtlFileParser.TransactionData transaction : transactions) {
            long secondTimestamp = transaction.getTimestamp().atZone(ZoneId.systemDefault()).toEpochSecond();
            throughputPerSecond.merge(secondTimestamp, 1L, Long::sum);
        }

        XYSeries series = new XYSeries("Throughput");
        long startTime = throughputPerSecond.keySet().stream().min(Long::compareTo).orElse(0L);
        
        throughputPerSecond.forEach((timestamp, count) -> {
            long relativeTime = timestamp - startTime;
            series.add(relativeTime, count);
        });

        XYSeriesCollection dataset = new XYSeriesCollection(series);

        JFreeChart chart = ChartFactory.createXYLineChart(
                "Throughput Over Time",
                "Time (seconds)",
                "Requests per Second",
                dataset,
                PlotOrientation.VERTICAL,
                false,
                true,
                false
        );

        customizeXYChart(chart, SUCCESS_COLOR);

        String filename = "03_throughput_over_time.png";
        File outputFile = new File(outputDir, filename);
        ChartUtils.saveChartAsPNG(outputFile, chart, CHART_WIDTH, CHART_HEIGHT);
        log.info("Generated: {}", filename);
        return outputFile.getAbsolutePath();
    }

    /**
     * 4. Error Rate Chart
     */
    private String generateErrorRateChart(TestRun testRun, List<JtlFileParser.TransactionData> transactions, String outputDir) throws IOException {
        log.info("Generating Error Rate chart...");

        long successCount = transactions.stream()
                .filter(JtlFileParser.TransactionData::isSuccess)
                .count();
        long failureCount = transactions.size() - successCount;

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        dataset.addValue(successCount, "Count", "Successful");
        dataset.addValue(failureCount, "Count", "Failed");

        JFreeChart chart = ChartFactory.createBarChart(
                "Success vs Failure Rate",
                "Status",
                "Number of Requests",
                dataset,
                PlotOrientation.VERTICAL,
                false,
                true,
                false
        );

        CategoryPlot plot = chart.getCategoryPlot();
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, SUCCESS_COLOR);
        renderer.setSeriesPaint(1, ERROR_COLOR);
        
        customizeBarChart(chart, SUCCESS_COLOR);

        String filename = "04_error_rate.png";
        File outputFile = new File(outputDir, filename);
        ChartUtils.saveChartAsPNG(outputFile, chart, CHART_WIDTH, CHART_HEIGHT);
        log.info("Generated: {}", filename);
        return outputFile.getAbsolutePath();
    }

    /**
     * 5. Transaction Summary Chart (Top 10 transactions by avg response time)
     */
    private String generateTransactionSummaryChart(TestRun testRun, List<JtlFileParser.TransactionData> transactions, String outputDir) throws IOException {
        log.info("Generating Transaction Summary chart...");

        // Calculate average response time per transaction name
        Map<String, Double> avgResponseTimes = transactions.stream()
                .collect(Collectors.groupingBy(
                        JtlFileParser.TransactionData::getTransactionName,
                        Collectors.averagingLong(JtlFileParser.TransactionData::getResponseTime)
                ));

        // Get top 10 slowest transactions
        List<Map.Entry<String, Double>> top10 = avgResponseTimes.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toList());

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (Map.Entry<String, Double> entry : top10) {
            String transactionName = entry.getKey().length() > 30 ? 
                    entry.getKey().substring(0, 27) + "..." : entry.getKey();
            dataset.addValue(entry.getValue(), "Avg Response Time", transactionName);
        }

        JFreeChart chart = ChartFactory.createBarChart(
                "Top 10 Slowest Transactions",
                "Transaction Name",
                "Average Response Time (ms)",
                dataset,
                PlotOrientation.HORIZONTAL,
                false,
                true,
                false
        );

        customizeBarChart(chart, WARNING_COLOR);

        String filename = "05_transaction_summary.png";
        File outputFile = new File(outputDir, filename);
        ChartUtils.saveChartAsPNG(outputFile, chart, CHART_WIDTH, CHART_HEIGHT);
        log.info("Generated: {}", filename);
        return outputFile.getAbsolutePath();
    }

    /**
     * 6. Percentile Comparison Chart
     */
    private String generatePercentileComparisonChart(TestRun testRun, List<JtlFileParser.TransactionData> transactions, String outputDir) throws IOException {
        log.info("Generating Percentile Comparison chart...");

        List<Long> responseTimes = transactions.stream()
                .map(JtlFileParser.TransactionData::getResponseTime)
                .sorted()
                .collect(Collectors.toList());

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        dataset.addValue(calculatePercentile(responseTimes, 50), "Response Time", "50th (Median)");
        dataset.addValue(calculatePercentile(responseTimes, 75), "Response Time", "75th");
        dataset.addValue(calculatePercentile(responseTimes, 90), "Response Time", "90th");
        dataset.addValue(calculatePercentile(responseTimes, 95), "Response Time", "95th");
        dataset.addValue(calculatePercentile(responseTimes, 99), "Response Time", "99th");

        JFreeChart chart = ChartFactory.createBarChart(
                "Response Time Percentiles",
                "Percentile",
                "Response Time (ms)",
                dataset,
                PlotOrientation.VERTICAL,
                false,
                true,
                false
        );

        customizeBarChart(chart, PRIMARY_COLOR);

        String filename = "06_percentile_comparison.png";
        File outputFile = new File(outputDir, filename);
        ChartUtils.saveChartAsPNG(outputFile, chart, CHART_WIDTH, CHART_HEIGHT);
        log.info("Generated: {}", filename);
        return outputFile.getAbsolutePath();
    }

    /**
     * 7. Latency Analysis Chart
     */
    private String generateLatencyAnalysisChart(TestRun testRun, List<JtlFileParser.TransactionData> transactions, String outputDir) throws IOException {
        log.info("Generating Latency Analysis chart...");

        List<JtlFileParser.TransactionData> transactionsWithLatency = transactions.stream()
                .filter(t -> t.getLatency() != null && t.getLatency() > 0)
                .collect(Collectors.toList());

        if (transactionsWithLatency.isEmpty()) {
            log.warn("No latency data available, skipping latency chart");
            // Create empty chart
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            dataset.addValue(0, "No Data", "Latency");
            
            JFreeChart chart = ChartFactory.createBarChart(
                    "Latency Analysis (No Data Available)",
                    "Metric",
                    "Time (ms)",
                    dataset,
                    PlotOrientation.VERTICAL,
                    false,
                    true,
                    false
            );
            
            String filename = "07_latency_analysis.png";
            File outputFile = new File(outputDir, filename);
            ChartUtils.saveChartAsPNG(outputFile, chart, CHART_WIDTH, CHART_HEIGHT);
            return outputFile.getAbsolutePath();
        }

        double avgLatency = transactionsWithLatency.stream()
                .mapToLong(JtlFileParser.TransactionData::getLatency)
                .average()
                .orElse(0.0);

        double avgResponseTime = transactionsWithLatency.stream()
                .mapToLong(JtlFileParser.TransactionData::getResponseTime)
                .average()
                .orElse(0.0);

        double avgConnectTime = transactionsWithLatency.stream()
                .filter(t -> t.getConnectTime() != null)
                .mapToLong(JtlFileParser.TransactionData::getConnectTime)
                .average()
                .orElse(0.0);

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        dataset.addValue(avgLatency, "Time", "Latency");
        dataset.addValue(avgConnectTime, "Time", "Connect Time");
        dataset.addValue(avgResponseTime - avgLatency, "Time", "Processing Time");

        JFreeChart chart = ChartFactory.createBarChart(
                "Average Latency Breakdown",
                "Metric",
                "Time (ms)",
                dataset,
                PlotOrientation.VERTICAL,
                false,
                true,
                false
        );

        customizeBarChart(chart, PRIMARY_COLOR);

        String filename = "07_latency_analysis.png";
        File outputFile = new File(outputDir, filename);
        ChartUtils.saveChartAsPNG(outputFile, chart, CHART_WIDTH, CHART_HEIGHT);
        log.info("Generated: {}", filename);
        return outputFile.getAbsolutePath();
    }

    /**
     * 8. Thread Activity Chart
     */
    private String generateThreadActivityChart(TestRun testRun, List<JtlFileParser.TransactionData> transactions, String outputDir) throws IOException {
        log.info("Generating Thread Activity chart...");

        // Count requests per thread
        Map<String, Long> threadActivity = transactions.stream()
                .filter(t -> t.getThreadName() != null)
                .collect(Collectors.groupingBy(
                        JtlFileParser.TransactionData::getThreadName,
                        Collectors.counting()
                ));

        // Limit to top 15 threads
        List<Map.Entry<String, Long>> topThreads = threadActivity.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(15)
                .collect(Collectors.toList());

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (Map.Entry<String, Long> entry : topThreads) {
            String threadName = entry.getKey().length() > 20 ? 
                    entry.getKey().substring(0, 17) + "..." : entry.getKey();
            dataset.addValue(entry.getValue(), "Requests", threadName);
        }

        JFreeChart chart = ChartFactory.createBarChart(
                "Thread Activity (Top 15 Threads)",
                "Thread Name",
                "Number of Requests",
                dataset,
                PlotOrientation.HORIZONTAL,
                false,
                true,
                false
        );

        customizeBarChart(chart, SUCCESS_COLOR);

        String filename = "08_thread_activity.png";
        File outputFile = new File(outputDir, filename);
        ChartUtils.saveChartAsPNG(outputFile, chart, CHART_WIDTH, CHART_HEIGHT);
        log.info("Generated: {}", filename);
        return outputFile.getAbsolutePath();
    }

    /**
     * 9. Errors By Label Chart
     */
    private String generateErrorsByLabelChart(TestRun testRun, List<JtlFileParser.TransactionData> transactions, String outputDir) throws IOException {
        log.info("Generating Errors By Label chart...");

        Map<String, Long> totalByLabel = new HashMap<>();
        Map<String, Long> failuresByLabel = new HashMap<>();

        for (JtlFileParser.TransactionData transaction : transactions) {
            String label = normalizeLabel(transaction.getTransactionName());
            totalByLabel.merge(label, 1L, Long::sum);
            if (!transaction.isSuccess()) {
                failuresByLabel.merge(label, 1L, Long::sum);
            }
        }

        List<String> topLabels = failuresByLabel.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (topLabels.isEmpty()) {
            topLabels = totalByLabel.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(10)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (String label : topLabels) {
            long total = totalByLabel.getOrDefault(label, 0L);
            long failed = failuresByLabel.getOrDefault(label, 0L);
            double errorRate = total == 0 ? 0.0 : (failed * 100.0) / total;
            dataset.addValue(errorRate, "Error Rate (%)", trimLabel(label, 25));
        }

        JFreeChart chart = ChartFactory.createBarChart(
                "Error Rate by Label",
                "Label",
                "Error Rate (%)",
                dataset,
                PlotOrientation.VERTICAL,
                false,
                true,
                false
        );

        customizeBarChart(chart, ERROR_COLOR);

        String filename = "09_errors_by_label.png";
        File outputFile = new File(outputDir, filename);
        ChartUtils.saveChartAsPNG(outputFile, chart, CHART_WIDTH, CHART_HEIGHT);
        log.info("Generated: {}", filename);
        return outputFile.getAbsolutePath();
    }

    /**
     * 10. Response Time By Label Over Time Chart
     */
    private String generateResponseTimeByLabelOverTimeChart(TestRun testRun, List<JtlFileParser.TransactionData> transactions, String outputDir) throws IOException {
        log.info("Generating Response Time By Label Over Time chart...");

        List<String> topLabels = getTopLabels(transactions, 5);
        if (topLabels.isEmpty()) {
            return createEmptyChart(outputDir, "10_response_time_by_label_over_time.png", "Response Time by Label Over Time (No Data)");
        }

        Map<String, Map<Long, List<Long>>> responseTimesByLabelSecond = new HashMap<>();
        for (JtlFileParser.TransactionData transaction : transactions) {
            String label = normalizeLabel(transaction.getTransactionName());
            if (!topLabels.contains(label)) {
                continue;
            }
            long epochSecond = transaction.getTimestamp().atZone(ZoneId.systemDefault()).toEpochSecond();
            responseTimesByLabelSecond
                    .computeIfAbsent(label, k -> new TreeMap<>())
                    .computeIfAbsent(epochSecond, k -> new ArrayList<>())
                    .add(transaction.getResponseTime());
        }

        long startTime = responseTimesByLabelSecond.values().stream()
                .flatMap(map -> map.keySet().stream())
                .min(Long::compareTo)
                .orElse(0L);

        XYSeriesCollection dataset = new XYSeriesCollection();
        for (String label : topLabels) {
            Map<Long, List<Long>> perSecond = responseTimesByLabelSecond.getOrDefault(label, new TreeMap<>());
            XYSeries series = new XYSeries(trimLabel(label, 20));
            for (Map.Entry<Long, List<Long>> entry : perSecond.entrySet()) {
                long relativeTime = entry.getKey() - startTime;
                double avg = entry.getValue().stream().mapToLong(Long::longValue).average().orElse(0.0);
                series.add(relativeTime, avg);
            }
            dataset.addSeries(series);
        }

        JFreeChart chart = ChartFactory.createXYLineChart(
                "Response Time by Label Over Time",
                "Time (seconds)",
                "Response Time (ms)",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        customizeMultiSeriesXYChart(chart);

        String filename = "10_response_time_by_label_over_time.png";
        File outputFile = new File(outputDir, filename);
        ChartUtils.saveChartAsPNG(outputFile, chart, CHART_WIDTH, CHART_HEIGHT);
        log.info("Generated: {}", filename);
        return outputFile.getAbsolutePath();
    }

    /**
     * 11. Response Time Percentiles By Label Chart
     */
    private String generateResponseTimePercentilesByLabelChart(TestRun testRun, List<JtlFileParser.TransactionData> transactions, String outputDir) throws IOException {
        log.info("Generating Response Time Percentiles By Label chart...");

        List<String> topLabels = getTopLabels(transactions, 8);
        if (topLabels.isEmpty()) {
            return createEmptyChart(outputDir, "11_response_time_percentiles_by_label.png", "Response Time Percentiles by Label (No Data)");
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        for (String label : topLabels) {
            List<Long> responseTimes = transactions.stream()
                    .filter(t -> normalizeLabel(t.getTransactionName()).equals(label))
                    .map(JtlFileParser.TransactionData::getResponseTime)
                    .sorted()
                    .collect(Collectors.toList());

            double p50 = calculatePercentile(responseTimes, 50);
            double p95 = calculatePercentile(responseTimes, 95);
            double p99 = calculatePercentile(responseTimes, 99);

            String trimmed = trimLabel(label, 20);
            dataset.addValue(p50, "P50", trimmed);
            dataset.addValue(p95, "P95", trimmed);
            dataset.addValue(p99, "P99", trimmed);
        }

        JFreeChart chart = ChartFactory.createBarChart(
                "Response Time Percentiles by Label",
                "Label",
                "Response Time (ms)",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        customizeBarChart(chart, PRIMARY_COLOR);

        String filename = "11_response_time_percentiles_by_label.png";
        File outputFile = new File(outputDir, filename);
        ChartUtils.saveChartAsPNG(outputFile, chart, CHART_WIDTH, CHART_HEIGHT);
        log.info("Generated: {}", filename);
        return outputFile.getAbsolutePath();
    }

    /**
     * 12. Throughput By Label Over Time Chart
     */
    private String generateThroughputByLabelOverTimeChart(TestRun testRun, List<JtlFileParser.TransactionData> transactions, String outputDir) throws IOException {
        log.info("Generating Throughput By Label Over Time chart...");

        List<String> topLabels = getTopLabels(transactions, 5);
        if (topLabels.isEmpty()) {
            return createEmptyChart(outputDir, "12_throughput_by_label_over_time.png", "Throughput by Label Over Time (No Data)");
        }

        Map<String, Map<Long, Long>> throughputByLabelSecond = new HashMap<>();
        for (JtlFileParser.TransactionData transaction : transactions) {
            String label = normalizeLabel(transaction.getTransactionName());
            if (!topLabels.contains(label)) {
                continue;
            }
            long epochSecond = transaction.getTimestamp().atZone(ZoneId.systemDefault()).toEpochSecond();
            throughputByLabelSecond
                    .computeIfAbsent(label, k -> new TreeMap<>())
                    .merge(epochSecond, 1L, Long::sum);
        }

        long startTime = throughputByLabelSecond.values().stream()
                .flatMap(map -> map.keySet().stream())
                .min(Long::compareTo)
                .orElse(0L);

        XYSeriesCollection dataset = new XYSeriesCollection();
        for (String label : topLabels) {
            Map<Long, Long> perSecond = throughputByLabelSecond.getOrDefault(label, new TreeMap<>());
            XYSeries series = new XYSeries(trimLabel(label, 20));
            for (Map.Entry<Long, Long> entry : perSecond.entrySet()) {
                long relativeTime = entry.getKey() - startTime;
                series.add(relativeTime, entry.getValue());
            }
            dataset.addSeries(series);
        }

        JFreeChart chart = ChartFactory.createXYLineChart(
                "Throughput by Label Over Time",
                "Time (seconds)",
                "Requests per Second",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        customizeMultiSeriesXYChart(chart);

        String filename = "12_throughput_by_label_over_time.png";
        File outputFile = new File(outputDir, filename);
        ChartUtils.saveChartAsPNG(outputFile, chart, CHART_WIDTH, CHART_HEIGHT);
        log.info("Generated: {}", filename);
        return outputFile.getAbsolutePath();
    }

    /**
     * 13. SLA Threshold Band (P95 vs Target)
     */
    private String generateSlaThresholdBandChart(TestRun testRun, List<JtlFileParser.TransactionData> transactions, String outputDir) throws IOException {
        log.info("Generating SLA Threshold Band chart...");

        Map<Long, List<Long>> responseTimesPerMinute = new TreeMap<>();
        for (JtlFileParser.TransactionData transaction : transactions) {
            long epochMinute = transaction.getTimestamp().atZone(ZoneId.systemDefault()).toEpochSecond() / 60;
            responseTimesPerMinute.computeIfAbsent(epochMinute, k -> new ArrayList<>())
                    .add(transaction.getResponseTime());
        }

        if (responseTimesPerMinute.isEmpty()) {
            return createEmptyChart(outputDir, "13_p95_sla_band.png", "P95 SLA Band (No Data)");
        }

        long startMinute = responseTimesPerMinute.keySet().stream().min(Long::compareTo).orElse(0L);
        double slaThreshold = resolveP95ThresholdMs(testRun);

        XYSeries p95Series = new XYSeries("P95 Response Time");
        XYSeries slaSeries = new XYSeries("SLA Threshold");

        for (Map.Entry<Long, List<Long>> entry : responseTimesPerMinute.entrySet()) {
            long relativeMinute = entry.getKey() - startMinute;
            List<Long> values = entry.getValue().stream().sorted().collect(Collectors.toList());
            double p95 = calculatePercentile(values, 95);
            p95Series.add(relativeMinute, p95);
            slaSeries.add(relativeMinute, slaThreshold);
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(p95Series);
        dataset.addSeries(slaSeries);

        JFreeChart chart = ChartFactory.createXYLineChart(
                "P95 Response Time vs SLA Threshold",
                "Time (minutes)",
                "Response Time (ms)",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        customizeMultiSeriesXYChart(chart);

        String filename = "13_p95_sla_band.png";
        File outputFile = new File(outputDir, filename);
        ChartUtils.saveChartAsPNG(outputFile, chart, CHART_WIDTH, CHART_HEIGHT);
        log.info("Generated: {}", filename);
        return outputFile.getAbsolutePath();
    }

    // Helper methods

    private void customizeChart(JFreeChart chart, Color color) {
        chart.setBackgroundPaint(Color.WHITE);
        chart.getTitle().setFont(new Font("SansSerif", Font.BOLD, 18));
        
        if (chart.getPlot() instanceof XYPlot) {
            XYPlot plot = chart.getXYPlot();
            plot.setBackgroundPaint(Color.WHITE);
            plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
            plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
            
            XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
            renderer.setSeriesPaint(0, color);
            renderer.setSeriesStroke(0, new BasicStroke(2.0f));
            plot.setRenderer(renderer);
        }
    }

    private void customizeMultiSeriesXYChart(JFreeChart chart) {
        customizeChart(chart, PRIMARY_COLOR);
        if (chart.getPlot() instanceof XYPlot) {
            XYPlot plot = chart.getXYPlot();
            XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
            for (int i = 0; i < plot.getDataset().getSeriesCount(); i++) {
                renderer.setSeriesPaint(i, SERIES_COLORS[i % SERIES_COLORS.length]);
                renderer.setSeriesShapesVisible(i, false);
            }
            plot.setRenderer(renderer);
        }
    }

    private List<String> getTopLabels(List<JtlFileParser.TransactionData> transactions, int limit) {
        return transactions.stream()
                .collect(Collectors.groupingBy(
                        t -> normalizeLabel(t.getTransactionName()),
                        Collectors.counting()
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private String normalizeLabel(String label) {
        if (label == null || label.trim().isEmpty()) {
            return "Unknown";
        }
        return label.trim();
    }

    private String trimLabel(String label, int maxLength) {
        if (label.length() <= maxLength) {
            return label;
        }
        return label.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private double calculatePercentile(List<Long> sortedValues, int percentile) {
        if (sortedValues == null || sortedValues.isEmpty()) {
            return 0.0;
        }
        int index = (int) Math.ceil(percentile / 100.0 * sortedValues.size()) - 1;
        index = Math.max(0, Math.min(index, sortedValues.size() - 1));
        return sortedValues.get(index);
    }

    private double resolveP95ThresholdMs(TestRun testRun) {
        double defaultThreshold = 2000.0;
        try {
            if (testRun.getCapability() == null || testRun.getCapability().getAcceptanceCriteria() == null) {
                return defaultThreshold;
            }

            Object criteriaObj = testRun.getCapability().getAcceptanceCriteria().get("criteria");
            if (!(criteriaObj instanceof List)) {
                return defaultThreshold;
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> criteriaList = (List<Map<String, Object>>) criteriaObj;
            for (Map<String, Object> criterion : criteriaList) {
                Object metricObj = criterion.get("metric");
                if (metricObj == null) {
                    continue;
                }
                String metric = metricObj.toString().toLowerCase();
                if (metric.contains("p95") || metric.contains("95")) {
                    Object thresholdObj = criterion.get("threshold");
                    if (thresholdObj != null) {
                        double threshold = toDouble(thresholdObj);
                        if (threshold > 0) {
                            return threshold;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to resolve SLA threshold, using default: {}", e.getMessage());
        }
        return defaultThreshold;
    }

    private double toDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private String createEmptyChart(String outputDir, String filename, String title) throws IOException {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        dataset.addValue(0, "No Data", "No Data");
        JFreeChart chart = ChartFactory.createBarChart(
                title,
                "Metric",
                "Value",
                dataset,
                PlotOrientation.VERTICAL,
                false,
                true,
                false
        );
        File outputFile = new File(outputDir, filename);
        ChartUtils.saveChartAsPNG(outputFile, chart, CHART_WIDTH, CHART_HEIGHT);
        log.info("Generated: {}", filename);
        return outputFile.getAbsolutePath();
    }

    private void customizeXYChart(JFreeChart chart, Color color) {
        chart.setBackgroundPaint(Color.WHITE);
        chart.getTitle().setFont(new Font("SansSerif", Font.BOLD, 18));
        
        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesPaint(0, color);
        renderer.setSeriesStroke(0, new BasicStroke(2.0f));
        renderer.setSeriesShapesVisible(0, false);
        plot.setRenderer(renderer);
    }

    private void customizeBarChart(JFreeChart chart, Color color) {
        chart.setBackgroundPaint(Color.WHITE);
        chart.getTitle().setFont(new Font("SansSerif", Font.BOLD, 18));
        
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, color);
        renderer.setBarPainter(new org.jfree.chart.renderer.category.StandardBarPainter());
        
        CategoryAxis domainAxis = plot.getDomainAxis();
        domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
    }

    /**
     * Result class for diagram generation
     */
    public static class DiagramGenerationResult {
        private final int diagramCount;
        private final List<String> diagramPaths;
        private final String diagramDirectory;

        public DiagramGenerationResult(int diagramCount, List<String> diagramPaths, String diagramDirectory) {
            this.diagramCount = diagramCount;
            this.diagramPaths = diagramPaths;
            this.diagramDirectory = diagramDirectory;
        }

        public int getDiagramCount() {
            return diagramCount;
        }

        public List<String> getDiagramPaths() {
            return diagramPaths;
        }

        public String getDiagramDirectory() {
            return diagramDirectory;
        }
    }
}

// Made with Bob