package com.hamza.durandhar.performance.service;

import com.hamza.durandhar.performance.entity.Report;
import com.hamza.durandhar.performance.entity.TestRun;
import com.hamza.durandhar.performance.entity.TestTransaction;
import com.hamza.durandhar.performance.repository.ReportRepository;
import com.hamza.durandhar.performance.repository.TestRunRepository;
import com.hamza.durandhar.performance.repository.TestTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for creating Word documents for performance test reports
 * Based on BPT CreateDocument implementation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CreateDocumentService {

    private final TestRunRepository testRunRepository;
    private final ReportRepository reportRepository;
    private final TestTransactionRepository testTransactionRepository;
    private final GraphFactory graphFactory;

    @Value("${app.diagrams.dir:/opt/performance-portal/diagrams}")
    private String diagramsDirectory;

    @Value("${app.reports.dir:/opt/performance-portal/reports}")
    private String reportsDirectory;

    /**
     * Create a Word document report for a test run
     *
     * @param testRunId ID of the test run
     * @param generatedBy User who generated the report
     * @return Generated Report entity
     */
    public Report createDocumentReport(Long testRunId, String generatedBy) throws Exception {
        log.info("Creating document report for test run ID: {}", testRunId);

        TestRun testRun = testRunRepository.findById(testRunId)
                .orElseThrow(() -> new RuntimeException("Test run not found: " + testRunId));

        // Use configured reports directory instead of temp
        Path reportDir = Paths.get(reportsDirectory);
        if (!Files.exists(reportDir)) {
            Files.createDirectories(reportDir);
        }

        // Generate the Word document
        File documentFile = createDocument(reportDir.toFile(), testRun);

        // Save report metadata to database
        Report report = new Report();
        report.setTestRun(testRun);
        report.setReportType(Report.ReportType.TECHNICAL_WORD);
        report.setFileName(documentFile.getName());
        report.setFilePath(documentFile.getAbsolutePath());
        report.setFileSize(documentFile.length());
        report.setGeneratedBy(generatedBy);
        report.setGeneratedAt(LocalDateTime.now());
        report.setDescription("Technical performance report generated from JTL test run data");

        report = reportRepository.save(report);
        log.info("Document report created successfully: {}", report.getFileName());

        return report;
    }

    /**
     * Create Word document with test results
     */
    private File createDocument(File tempDir, TestRun testRun) throws Exception {
        log.info("Generating Word document for test run: {}", testRun.getTestName());

        // Load template
        String templatePath = "src/main/resources/templates/technical_report_template.docx";
        XWPFDocument document;
        
        File templateFile = new File(templatePath);
        if (templateFile.exists()) {
            document = new XWPFDocument(new FileInputStream(templateFile));
        } else {
            // Create new document if template doesn't exist
            document = new XWPFDocument();
        }

        // Add title page
        addTitlePage(document, testRun);

        // Add all required sections
        addIntroductionSection(document, testRun);
        addBenchmarkGoalsSection(document, testRun);
        addTestSetupSection(document, testRun);
        addTestInfrastructureSection(document, testRun);
        addTestScenariosSection(document, testRun);
        addPerformanceMetricsSection(document, testRun);
        addTestResultsSection(document, testRun);
        addPerformanceAnalysisSection(document, testRun);
        addConclusionsSection(document, testRun);

        // Save document
        String fileName = String.format("%s_%s_Report.docx", 
            testRun.getTestName().replaceAll("\\s+", "_"),
            testRun.getId());
        File outputFile = new File(tempDir, fileName);

        try (FileOutputStream out = new FileOutputStream(outputFile)) {
            document.write(out);
        }

        document.close();
        return outputFile;
    }

    /**
     * Add title page to document
     */
    private void addTitlePage(XWPFDocument document, TestRun testRun) {
        XWPFParagraph titlePara = document.createParagraph();
        titlePara.setAlignment(ParagraphAlignment.LEFT);
        titlePara.setVerticalAlignment(TextAlignment.CENTER);

        XWPFRun titleRun = titlePara.createRun();
        titleRun.setBold(true);
        titleRun.setFontSize(26);
        titleRun.setFontFamily("Rubik");
        titleRun.setText(testRun.getTestName() + " Performance Report");
        titleRun.addBreak(BreakType.TEXT_WRAPPING);
        titleRun.addBreak(BreakType.TEXT_WRAPPING);

        XWPFRun subtitleRun = titlePara.createRun();
        subtitleRun.setFontSize(18);
        subtitleRun.setFontFamily("Rubik");
        subtitleRun.setText("Performance Technical Report");
    }

    /**
     * Add Introduction section
     */
    private void addIntroductionSection(XWPFDocument document, TestRun testRun) {
        XWPFParagraph heading = document.createParagraph();
        heading.setStyle("Heading1");
        XWPFRun headingRun = heading.createRun();
        headingRun.setText("Introduction");

        XWPFParagraph content = document.createParagraph();
        XWPFRun contentRun = content.createRun();
        contentRun.setText("This technical report presents the performance test results for " + testRun.getTestName() + ". ");
        contentRun.addBreak();
        contentRun.setText("The test was conducted to evaluate system performance under various workload conditions and identify potential bottlenecks.");
        contentRun.addBreak();
        contentRun.addBreak();
    }

    /**
     * Add Benchmark Goals section
     */
    private void addBenchmarkGoalsSection(XWPFDocument document, TestRun testRun) {
        XWPFParagraph heading = document.createParagraph();
        heading.setStyle("Heading1");
        XWPFRun headingRun = heading.createRun();
        headingRun.setText("Benchmark Goals");

        XWPFParagraph content = document.createParagraph();
        XWPFRun contentRun = content.createRun();
        contentRun.setText("The primary objectives of this performance test are:");
        contentRun.addBreak();
        contentRun.setText("• Measure system response time under expected workload");
        contentRun.addBreak();
        contentRun.setText("• Evaluate throughput capacity");
        contentRun.addBreak();
        contentRun.setText("• Identify performance bottlenecks");
        contentRun.addBreak();
        contentRun.setText("• Validate system stability under sustained workload");
        contentRun.addBreak();
        contentRun.setText("• Determine optimal configuration for production deployment");
        contentRun.addBreak();
        contentRun.addBreak();
    }

    /**
     * Add Test Setup section
     */
    private void addTestSetupSection(XWPFDocument document, TestRun testRun) {
        XWPFParagraph heading = document.createParagraph();
        heading.setStyle("Heading1");
        XWPFRun headingRun = heading.createRun();
        headingRun.setText("Test Setup");

        XWPFParagraph content = document.createParagraph();
        XWPFRun contentRun = content.createRun();
        contentRun.setText("Test Configuration:");
        contentRun.addBreak();
        contentRun.setText("Test Name: " + testRun.getTestName());
        contentRun.addBreak();
        contentRun.setText("Test Type: " + testRun.getFileType());
        contentRun.addBreak();
        contentRun.setText("Test Date: " + testRun.getTestDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        contentRun.addBreak();
        if (testRun.getCapability() != null) {
            contentRun.setText("Capability: " + testRun.getCapability().getName());
            contentRun.addBreak();
        }
        if (testRun.getBuildNumber() != null) {
            contentRun.setText("Build Number: " + testRun.getBuildNumber());
            contentRun.addBreak();
        }
        if (testRun.getDescription() != null) {
            contentRun.setText("Description: " + testRun.getDescription());
            contentRun.addBreak();
        }
        contentRun.addBreak();
    }

    /**
     * Add Test Infrastructure section
     */
    private void addTestInfrastructureSection(XWPFDocument document, TestRun testRun) {
        XWPFParagraph heading = document.createParagraph();
        heading.setStyle("Heading1");
        XWPFRun headingRun = heading.createRun();
        headingRun.setText("Test Infrastructure");
        addDefaultInfrastructure(document, testRun);
    }
    
    /**
     * Add default infrastructure details for other capabilities
     */
    private void addDefaultInfrastructure(XWPFDocument document, TestRun testRun) {
        XWPFParagraph content = document.createParagraph();
        XWPFRun contentRun = content.createRun();
        contentRun.setText("Test Tool: Apache JMeter");
        contentRun.addBreak();
        contentRun.setText("Test Date: " + testRun.getTestDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        contentRun.addBreak();
        contentRun.addBreak();
    }

    /**
     * Add Test Scenarios section
     */
    private void addTestScenariosSection(XWPFDocument document, TestRun testRun) {
        XWPFParagraph heading = document.createParagraph();
        heading.setStyle("Heading1");
        XWPFRun headingRun = heading.createRun();
        headingRun.setText("Test Scenarios");

        XWPFParagraph content = document.createParagraph();
        XWPFRun contentRun = content.createRun();
        contentRun.setText("The following test scenarios were executed:");
        contentRun.addBreak();
        contentRun.addBreak();
        
        addDefaultScenarios(contentRun);
    }
    
    /**
     * Add default test scenarios for other capabilities
     */
    private void addDefaultScenarios(XWPFRun contentRun) {
        contentRun.setText("Scenario 1: Baseline Performance");
        contentRun.addBreak();
        contentRun.setText("Objective: Establish baseline performance metrics under expected workload conditions");
        contentRun.addBreak();
        contentRun.addBreak();
        
        contentRun.setText("Scenario 2: Peak Traffic Testing");
        contentRun.addBreak();
        contentRun.setText("Objective: Evaluate system behavior under peak traffic conditions");
        contentRun.addBreak();
        contentRun.addBreak();
        
        contentRun.setText("Scenario 3: Sustained Traffic Testing");
        contentRun.addBreak();
        contentRun.setText("Objective: Validate system stability over extended duration");
        contentRun.addBreak();
        contentRun.addBreak();
    }

    /**
     * Add Performance Metrics section
     */
    private void addPerformanceMetricsSection(XWPFDocument document, TestRun testRun) {
        XWPFParagraph heading = document.createParagraph();
        heading.setStyle("Heading1");
        XWPFRun headingRun = heading.createRun();
        headingRun.setText("Performance Metrics");

        XWPFParagraph content = document.createParagraph();
        XWPFRun contentRun = content.createRun();
        contentRun.setText("Key performance indicators measured during the test:");
        contentRun.addBreak();
        contentRun.addBreak();
        
        contentRun.setText("Response Time Metrics:");
        contentRun.addBreak();
        contentRun.setText("• Average Response Time: Measures typical system response");
        contentRun.addBreak();
        contentRun.setText("• Percentile Metrics (90th, 95th, 99th): Identifies outliers and worst-case scenarios");
        contentRun.addBreak();
        contentRun.addBreak();
        
        contentRun.setText("Throughput Metrics:");
        contentRun.addBreak();
        contentRun.setText("• Requests per Second: System capacity measurement");
        contentRun.addBreak();
        contentRun.setText("• Total Requests: Overall test volume");
        contentRun.addBreak();
        contentRun.addBreak();
        
        contentRun.setText("Reliability Metrics:");
        contentRun.addBreak();
        contentRun.setText("• Error Rate: System stability indicator");
        contentRun.addBreak();
        contentRun.setText("• Success Rate: Overall reliability measurement");
        contentRun.addBreak();
        contentRun.addBreak();
    }

    /**
     * Add detailed statistics table (JMeter Synthesis Report style)
     * Uses per-label statistics stored in capability-specific data
     */
    private void addTransactionStatisticsTable(XWPFDocument document, TestRun testRun) {
        try {
            // Try to get label statistics from capability-specific data
            Map<String, Object> capabilityData = testRun.getCapabilitySpecificData();
            Map<String, Map<String, Object>> labelStats = null;
            
            if (capabilityData != null && capabilityData.containsKey("labelStatistics")) {
                labelStats = (Map<String, Map<String, Object>>) capabilityData.get("labelStatistics");
            }
            
            if (labelStats != null && !labelStats.isEmpty()) {
                // Display JMeter Synthesis Report style statistics table
                addSynthesisReportTable(document, labelStats);
                return;
            }
            
            // Fallback: Check if legacy transaction data exists
            List<TestTransaction> transactions = testTransactionRepository.findByTestRunId(testRun.getId());
            
            if (transactions.isEmpty()) {
                // No data available
                XWPFParagraph note = document.createParagraph();
                XWPFRun noteRun = note.createRun();
                noteRun.setItalic(true);
                noteRun.setFontSize(10);
                noteRun.setText("Note: Detailed per-label statistics are not available. ");
                noteRun.addBreak();
                noteRun.setText("Please re-upload the JTL file to generate detailed statistics.");
                noteRun.addBreak();
                noteRun.addBreak();
                
                log.debug("No label statistics or transaction data found for test run: {}", testRun.getId());
                return;
            }
            
            // Legacy transaction data exists - display it
            log.info("Found legacy transaction data for test run: {}", testRun.getId());
            
            // Calculate statistics per transaction label
            Map<String, TransactionStats> statsMap = calculateTransactionStatistics(transactions);
            
            if (statsMap.isEmpty()) {
                return;
            }
            
            // Add section heading
            XWPFParagraph heading = document.createParagraph();
            heading.setStyle("Heading2");
            XWPFRun headingRun = heading.createRun();
            headingRun.setText("Detailed Transaction Statistics");
            headingRun.addBreak();
            
            // Create comprehensive statistics table
            XWPFTable table = document.createTable(1, 14);
            table.setWidth("100%");
            
            // Header row
            XWPFTableRow headerRow = table.getRow(0);
            setCellText(headerRow.getCell(0), "Label", true);
            setCellText(headerRow.getCell(1), "#Samples", true);
            setCellText(headerRow.getCell(2), "FAIL", true);
            setCellText(headerRow.getCell(3), "Error %", true);
            setCellText(headerRow.getCell(4), "Average", true);
            setCellText(headerRow.getCell(5), "Min", true);
            setCellText(headerRow.getCell(6), "Max", true);
            setCellText(headerRow.getCell(7), "Median", true);
            setCellText(headerRow.getCell(8), "90th pct", true);
            setCellText(headerRow.getCell(9), "95th pct", true);
            setCellText(headerRow.getCell(10), "99th pct", true);
            setCellText(headerRow.getCell(11), "Transactions/s", true);
            setCellText(headerRow.getCell(12), "Received", true);
            setCellText(headerRow.getCell(13), "Sent", true);
            
            // Add "Total" row first
            if (statsMap.containsKey("TOTAL")) {
                addTransactionStatsRow(table, "Total", statsMap.get("TOTAL"));
            }
            
            // Add individual transaction rows (excluding TOTAL)
            statsMap.entrySet().stream()
                .filter(entry -> !"TOTAL".equals(entry.getKey()))
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> addTransactionStatsRow(table, entry.getKey(), entry.getValue()));
            
            document.createParagraph().createRun().addBreak();
            
        } catch (Exception e) {
            log.error("Failed to add transaction statistics table: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Add JMeter Synthesis Report style statistics table
     */
    private void addSynthesisReportTable(XWPFDocument document, Map<String, Map<String, Object>> labelStats) {
        // Add section heading
        XWPFParagraph heading = document.createParagraph();
        heading.setStyle("Heading2");
        XWPFRun headingRun = heading.createRun();
        headingRun.setText("Statistics");
        headingRun.addBreak();
        
        // Create comprehensive statistics table (JMeter Synthesis Report style)
        XWPFTable table = document.createTable(1, 13);
        table.setWidth("100%");
        
        // Header row with grouped columns
        XWPFTableRow headerRow = table.getRow(0);
        
        // Requests section
        setCellText(headerRow.getCell(0), "Label", true);
        setCellText(headerRow.getCell(1), "#Samples", true);
        setCellText(headerRow.getCell(2), "FAIL", true);
        setCellText(headerRow.getCell(3), "Error %", true);
        
        // Response Times section
        setCellText(headerRow.getCell(4), "Average", true);
        setCellText(headerRow.getCell(5), "Min", true);
        setCellText(headerRow.getCell(6), "Max", true);
        setCellText(headerRow.getCell(7), "Median", true);
        setCellText(headerRow.getCell(8), "90th pct", true);
        setCellText(headerRow.getCell(9), "95th pct", true);
        setCellText(headerRow.getCell(10), "99th pct", true);
        
        // Throughput section
        setCellText(headerRow.getCell(11), "Transactions/s", true);
        
        // Network section
        setCellText(headerRow.getCell(12), "Received KB/sec", true);
        
        // Add "Total" row first if it exists
        if (labelStats.containsKey("Total")) {
            addSynthesisReportRow(table, "Total", labelStats.get("Total"));
        }
        
        // Add individual label rows (excluding Total)
        labelStats.entrySet().stream()
            .filter(entry -> !"Total".equals(entry.getKey()))
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> addSynthesisReportRow(table, entry.getKey(), entry.getValue()));
        
        document.createParagraph().createRun().addBreak();
        
        log.info("Added synthesis report table with {} labels", labelStats.size());
    }
    
    /**
     * Add a row to the synthesis report table
     */
    private void addSynthesisReportRow(XWPFTable table, String label, Map<String, Object> stats) {
        XWPFTableRow row = table.createRow();
        
        // Label
        setCellText(row.getCell(0), label, false);
        
        // Executions
        setCellText(row.getCell(1), String.valueOf(getIntValue(stats, "samples")), false);
        setCellText(row.getCell(2), String.valueOf(getIntValue(stats, "failures")), false);
        setCellText(row.getCell(3), String.format("%.2f%%", getDoubleValue(stats, "errorPercent")), false);
        
        // Response Times (ms)
        setCellText(row.getCell(4), String.format("%.2f", getDoubleValue(stats, "average")), false);
        setCellText(row.getCell(5), String.valueOf(getLongValue(stats, "min")), false);
        setCellText(row.getCell(6), String.valueOf(getLongValue(stats, "max")), false);
        setCellText(row.getCell(7), String.format("%.2f", getDoubleValue(stats, "median")), false);
        setCellText(row.getCell(8), String.format("%.2f", getDoubleValue(stats, "percentile90")), false);
        setCellText(row.getCell(9), String.format("%.2f", getDoubleValue(stats, "percentile95")), false);
        setCellText(row.getCell(10), String.format("%.2f", getDoubleValue(stats, "percentile99")), false);
        
        // Throughput
        setCellText(row.getCell(11), String.format("%.2f", getDoubleValue(stats, "throughput")), false);
        
        // Network
        setCellText(row.getCell(12), String.format("%.2f", getDoubleValue(stats, "receivedKBPerSec")), false);
    }
    
    /**
     * Helper methods to safely extract values from stats map
     */
    private int getIntValue(Map<String, Object> stats, String key) {
        Object value = stats.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }
    
    private long getLongValue(Map<String, Object> stats, String key) {
        Object value = stats.get(key);
        if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0L;
    }
    
    private double getDoubleValue(Map<String, Object> stats, String key) {
        Object value = stats.get(key);
        if (value instanceof Double) {
            return (Double) value;
        } else if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }
    
    /**
     * Calculate statistics for each transaction label
     */
    private Map<String, TransactionStats> calculateTransactionStatistics(List<TestTransaction> transactions) {
        Map<String, List<TestTransaction>> groupedByLabel = new java.util.HashMap<>();
        
        // Group transactions by label
        for (TestTransaction tx : transactions) {
            groupedByLabel.computeIfAbsent(tx.getTransactionName(), k -> new ArrayList<>()).add(tx);
        }
        
        Map<String, TransactionStats> statsMap = new java.util.HashMap<>();
        
        // Calculate stats for each label
        for (Map.Entry<String, List<TestTransaction>> entry : groupedByLabel.entrySet()) {
            TransactionStats stats = new TransactionStats();
            List<TestTransaction> txList = entry.getValue();
            
            stats.samples = txList.size();
            stats.failures = txList.stream().filter(tx -> !tx.getSuccess()).count();
            stats.errorPercent = stats.samples > 0 ? (stats.failures * 100.0 / stats.samples) : 0.0;
            
            // Calculate response time statistics
            List<Long> responseTimes = txList.stream()
                .map(TestTransaction::getResponseTime)
                .sorted()
                .collect(java.util.stream.Collectors.toList());
            
            if (!responseTimes.isEmpty()) {
                stats.average = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
                stats.min = responseTimes.get(0);
                stats.max = responseTimes.get(responseTimes.size() - 1);
                stats.median = calculatePercentile(responseTimes, 50);
                stats.percentile90 = calculatePercentile(responseTimes, 90);
                stats.percentile95 = calculatePercentile(responseTimes, 95);
                stats.percentile99 = calculatePercentile(responseTimes, 99);
            }
            
            // Calculate throughput (transactions per second)
            if (!txList.isEmpty()) {
                LocalDateTime firstTimestamp = txList.stream()
                    .map(TestTransaction::getTimestamp)
                    .min(LocalDateTime::compareTo)
                    .orElse(LocalDateTime.now());
                LocalDateTime lastTimestamp = txList.stream()
                    .map(TestTransaction::getTimestamp)
                    .max(LocalDateTime::compareTo)
                    .orElse(LocalDateTime.now());
                
                long durationSeconds = java.time.Duration.between(firstTimestamp, lastTimestamp).getSeconds();
                stats.throughput = durationSeconds > 0 ? (double) stats.samples / durationSeconds : 0.0;
            }
            
            // Network data (placeholder - would need actual data from JTL)
            stats.received = 0.0;
            stats.sent = 0.0;
            
            statsMap.put(entry.getKey(), stats);
        }
        
        // Calculate TOTAL row
        if (!statsMap.isEmpty()) {
            TransactionStats totalStats = new TransactionStats();
            totalStats.samples = transactions.size();
            totalStats.failures = transactions.stream().filter(tx -> !tx.getSuccess()).count();
            totalStats.errorPercent = totalStats.samples > 0 ? (totalStats.failures * 100.0 / totalStats.samples) : 0.0;
            
            List<Long> allResponseTimes = transactions.stream()
                .map(TestTransaction::getResponseTime)
                .sorted()
                .collect(java.util.stream.Collectors.toList());
            
            if (!allResponseTimes.isEmpty()) {
                totalStats.average = allResponseTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
                totalStats.min = allResponseTimes.get(0);
                totalStats.max = allResponseTimes.get(allResponseTimes.size() - 1);
                totalStats.median = calculatePercentile(allResponseTimes, 50);
                totalStats.percentile90 = calculatePercentile(allResponseTimes, 90);
                totalStats.percentile95 = calculatePercentile(allResponseTimes, 95);
                totalStats.percentile99 = calculatePercentile(allResponseTimes, 99);
            }
            
            // Calculate overall throughput
            if (!transactions.isEmpty()) {
                LocalDateTime firstTimestamp = transactions.stream()
                    .map(TestTransaction::getTimestamp)
                    .min(LocalDateTime::compareTo)
                    .orElse(LocalDateTime.now());
                LocalDateTime lastTimestamp = transactions.stream()
                    .map(TestTransaction::getTimestamp)
                    .max(LocalDateTime::compareTo)
                    .orElse(LocalDateTime.now());
                
                long durationSeconds = java.time.Duration.between(firstTimestamp, lastTimestamp).getSeconds();
                totalStats.throughput = durationSeconds > 0 ? (double) totalStats.samples / durationSeconds : 0.0;
            }
            
            totalStats.received = 0.0;
            totalStats.sent = 0.0;
            
            statsMap.put("TOTAL", totalStats);
        }
        
        return statsMap;
    }
    
    /**
     * Calculate percentile value from sorted list
     */
    private double calculatePercentile(List<Long> sortedValues, int percentile) {
        if (sortedValues.isEmpty()) return 0.0;
        
        int index = (int) Math.ceil(percentile / 100.0 * sortedValues.size()) - 1;
        index = Math.max(0, Math.min(index, sortedValues.size() - 1));
        
        return sortedValues.get(index).doubleValue();
    }
    
    /**
     * Add a transaction statistics row to the table
     */
    private void addTransactionStatsRow(XWPFTable table, String label, TransactionStats stats) {
        XWPFTableRow row = table.createRow();
        setCellText(row.getCell(0), label, false);
        setCellText(row.getCell(1), String.valueOf(stats.samples), false);
        setCellText(row.getCell(2), String.valueOf(stats.failures), false);
        setCellText(row.getCell(3), String.format("%.2f%%", stats.errorPercent), false);
        setCellText(row.getCell(4), String.format("%.2f", stats.average), false);
        setCellText(row.getCell(5), String.valueOf(stats.min), false);
        setCellText(row.getCell(6), String.valueOf(stats.max), false);
        setCellText(row.getCell(7), String.format("%.2f", stats.median), false);
        setCellText(row.getCell(8), String.format("%.2f", stats.percentile90), false);
        setCellText(row.getCell(9), String.format("%.2f", stats.percentile95), false);
        setCellText(row.getCell(10), String.format("%.2f", stats.percentile99), false);
        setCellText(row.getCell(11), String.format("%.2f", stats.throughput), false);
        setCellText(row.getCell(12), String.format("%.2f", stats.received), false);
        setCellText(row.getCell(13), String.format("%.2f", stats.sent), false);
    }
    
    /**
     * Inner class to hold transaction statistics
     */
    private static class TransactionStats {
        long samples;
        long failures;
        double errorPercent;
        double average;
        long min;
        long max;
        double median;
        double percentile90;
        double percentile95;
        double percentile99;
        double throughput;
        double received;
        double sent;
    }

    /**
     * Add Test Results section (with actual metrics table)
     */
    private void addTestResultsSection(XWPFDocument document, TestRun testRun) {
        // Add section heading
        XWPFParagraph heading = document.createParagraph();
        heading.setStyle("Heading1");
        XWPFRun headingRun = heading.createRun();
        headingRun.setText("Test Results");

        // Create results table with proper status determination
        XWPFTable table = document.createTable(1, 4);
        
        // Header row
        XWPFTableRow headerRow = table.getRow(0);
        setCellText(headerRow.getCell(0), "Metric", true);
        setCellText(headerRow.getCell(1), "Value", true);
        setCellText(headerRow.getCell(2), "Unit", true);
        setCellText(headerRow.getCell(3), "Status", true);

        // Determine status based on metrics
        String status = determineMetricStatus(testRun);

        // Add actual metrics from TestRun with proper formatting
        addMetricRow(table, "Avg Response Time",
            testRun.getAvgResponseTime() != null ? String.format("%.2f", testRun.getAvgResponseTime()) : "N/A",
            "ms", status);
        
        addMetricRow(table, "Min Response Time",
            testRun.getMinResponseTime() != null ? String.format("%.2f", testRun.getMinResponseTime()) : "N/A",
            "ms", status);
        
        addMetricRow(table, "Max Response Time",
            testRun.getMaxResponseTime() != null ? String.format("%.2f", testRun.getMaxResponseTime()) : "N/A",
            "ms", status);
        
        addMetricRow(table, "Throughput",
            testRun.getThroughput() != null ? String.format("%.2f", testRun.getThroughput()) : "N/A",
            "req/s", status);
        
        addMetricRow(table, "Error Rate",
            testRun.getErrorRate() != null ? String.format("%.2f", testRun.getErrorRate()) : "N/A",
            "%", status);
        
        addMetricRow(table, "Total Requests",
            testRun.getTotalRequests() != null ? testRun.getTotalRequests().toString() : "N/A",
            "requests", status);
        
        addMetricRow(table, "90th Percentile",
            testRun.getPercentile90() != null ? String.format("%.2f", testRun.getPercentile90()) : "N/A",
            "ms", status);
        
        addMetricRow(table, "95th Percentile",
            testRun.getPercentile95() != null ? String.format("%.2f", testRun.getPercentile95()) : "N/A",
            "ms", status);
        
        addMetricRow(table, "99th Percentile",
            testRun.getPercentile99() != null ? String.format("%.2f", testRun.getPercentile99()) : "N/A",
            "ms", status);

        // Add spacing
        document.createParagraph().createRun().addBreak();

        // Add detailed transaction statistics table (if transaction data exists)
        addTransactionStatisticsTable(document, testRun);

        // Add diagrams section
        addDiagramsSection(document, testRun);
    }

    /**
     * Determine overall status based on test metrics
     */
    private String determineMetricStatus(TestRun testRun) {
        // If test run has explicit status, use it
        if (testRun.getStatus() == TestRun.TestStatus.COMPLETED) {
            // Check if metrics indicate success
            if (testRun.getErrorRate() != null && testRun.getErrorRate() > 5.0) {
                return "COMPLETED (High Error Rate)";
            }
            if (testRun.getAvgResponseTime() != null && testRun.getAvgResponseTime() > 1000) {
                return "COMPLETED (Slow Response)";
            }
            return "COMPLETED";
        }
        return testRun.getStatus().toString();
    }

    /**
     * Add Performance Analysis section
     */
    private void addPerformanceAnalysisSection(XWPFDocument document, TestRun testRun) {
        XWPFParagraph heading = document.createParagraph();
        heading.setStyle("Heading1");
        XWPFRun headingRun = heading.createRun();
        headingRun.setText("Performance Analysis");

        XWPFParagraph content = document.createParagraph();
        XWPFRun contentRun = content.createRun();
        contentRun.setText("Analysis of Test Results:");
        contentRun.addBreak();
        contentRun.addBreak();
        
        // Analyze response time
        if (testRun.getAvgResponseTime() != null) {
            contentRun.setText("Response Time Analysis:");
            contentRun.addBreak();
            contentRun.setText("• Average response time: " + String.format("%.2f ms", testRun.getAvgResponseTime()));
            contentRun.addBreak();
            if (testRun.getAvgResponseTime() < 100) {
                contentRun.setText("• Performance is excellent with sub-100ms average response time");
            } else if (testRun.getAvgResponseTime() < 500) {
                contentRun.setText("• Performance is good with acceptable response times");
            } else {
                contentRun.setText("• Performance may need optimization - response times exceed 500ms");
            }
            contentRun.addBreak();
            contentRun.addBreak();
        }
        
        // Analyze throughput
        if (testRun.getThroughput() != null) {
            contentRun.setText("Throughput Analysis:");
            contentRun.addBreak();
            contentRun.setText("• System throughput: " + String.format("%.2f requests/second", testRun.getThroughput()));
            contentRun.addBreak();
            contentRun.addBreak();
        }
        
        // Analyze error rate
        if (testRun.getErrorRate() != null) {
            contentRun.setText("Reliability Analysis:");
            contentRun.addBreak();
            contentRun.setText("• Error rate: " + String.format("%.2f%%", testRun.getErrorRate()));
            contentRun.addBreak();
            if (testRun.getErrorRate() < 1.0) {
                contentRun.setText("• System demonstrates excellent reliability with minimal errors");
            } else if (testRun.getErrorRate() < 5.0) {
                contentRun.setText("• System reliability is acceptable but may benefit from investigation");
            } else {
                contentRun.setText("• High error rate detected - immediate investigation recommended");
            }
            contentRun.addBreak();
            contentRun.addBreak();
        }
    }

    /**
     * Add Conclusions section
     */
    private void addConclusionsSection(XWPFDocument document, TestRun testRun) {
        XWPFParagraph heading = document.createParagraph();
        heading.setStyle("Heading1");
        XWPFRun headingRun = heading.createRun();
        headingRun.setText("Conclusions");

        XWPFParagraph content = document.createParagraph();
        XWPFRun contentRun = content.createRun();
        contentRun.setText("Summary and Recommendations:");
        contentRun.addBreak();
        contentRun.addBreak();
        
        contentRun.setText("Test Summary:");
        contentRun.addBreak();
        contentRun.setText("• Test completed with status: " + testRun.getStatus());
        contentRun.addBreak();
        if (testRun.getTotalRequests() != null) {
            contentRun.setText("• Total requests processed: " + testRun.getTotalRequests());
            contentRun.addBreak();
        }
        contentRun.addBreak();
        
        contentRun.setText("Key Findings:");
        contentRun.addBreak();
        
        // Provide conclusions based on metrics
        boolean performanceGood = true;
        if (testRun.getAvgResponseTime() != null && testRun.getAvgResponseTime() > 500) {
            contentRun.setText("• Response times exceed acceptable thresholds - optimization recommended");
            contentRun.addBreak();
            performanceGood = false;
        }
        if (testRun.getErrorRate() != null && testRun.getErrorRate() > 5.0) {
            contentRun.setText("• Error rate is high - system stability needs improvement");
            contentRun.addBreak();
            performanceGood = false;
        }
        
        if (performanceGood) {
            contentRun.setText("• System performance meets expected benchmarks");
            contentRun.addBreak();
            contentRun.setText("• System is ready for production deployment");
            contentRun.addBreak();
        } else {
            contentRun.setText("• Performance tuning recommended before production deployment");
            contentRun.addBreak();
        }
        
        contentRun.addBreak();
        contentRun.setText("Recommendations:");
        contentRun.addBreak();
        contentRun.setText("• Continue monitoring system performance in production");
        contentRun.addBreak();
        contentRun.setText("• Implement automated performance testing in CI/CD pipeline");
        contentRun.addBreak();
        contentRun.setText("• Review and optimize identified bottlenecks");
        contentRun.addBreak();
    }

    /**
     * Add a metric row to the table
     */
    private void addMetricRow(XWPFTable table, String metric, String value, String unit, String status) {
        XWPFTableRow row = table.createRow();
        setCellText(row.getCell(0), metric, false);
        setCellText(row.getCell(1), value, false);
        setCellText(row.getCell(2), unit, false);
        setCellText(row.getCell(3), status, false);
    }

    /**
     * Add diagrams section with all generated performance diagrams
     */
    private void addDiagramsSection(XWPFDocument document, TestRun testRun) {
        try {
            // Find diagram directory for this test run
            String capabilityName = testRun.getCapability().getName();
            Path capabilityDir = Paths.get(diagramsDirectory, capabilityName);
            
            if (!Files.exists(capabilityDir)) {
                log.warn("Diagram directory not found: {}", capabilityDir);
                return;
            }

            // Find the test run directory (format: testrun_{id}_{timestamp})
            List<Path> testRunDirs = Files.list(capabilityDir)
                .filter(Files::isDirectory)
                .filter(p -> p.getFileName().toString().startsWith("testrun_" + testRun.getId() + "_"))
                .collect(Collectors.toList());

            if (testRunDirs.isEmpty()) {
                log.warn("No diagram directory found for test run ID: {}", testRun.getId());
                return;
            }

            Path diagramDir = testRunDirs.get(0); // Get the most recent one
            log.info("Found diagram directory: {}", diagramDir);

            // Add section heading
            XWPFParagraph heading = document.createParagraph();
            heading.setStyle("Heading1");
            XWPFRun headingRun = heading.createRun();
            headingRun.setText("Performance Diagrams");
            headingRun.addBreak();

            // Add all diagrams
            List<Path> diagrams = Files.list(diagramDir)
                .filter(p -> p.toString().endsWith(".png"))
                .sorted()
                .collect(Collectors.toList());

            for (Path diagramPath : diagrams) {
                addDiagram(document, diagramPath);
            }

            log.info("Added {} diagrams to the report", diagrams.size());

        } catch (Exception e) {
            log.error("Failed to add diagrams to report: {}", e.getMessage(), e);
        }
    }

    /**
     * Add a single diagram to the document
     */
    private void addDiagram(XWPFDocument document, Path diagramPath) {
        try {
            // Add diagram title (extract from filename)
            String fileName = diagramPath.getFileName().toString();
            String title = fileName.replace(".png", "")
                .replaceAll("^\\d+_", "") // Remove leading numbers
                .replace("_", " ")
                .toUpperCase();

            XWPFParagraph titlePara = document.createParagraph();
            titlePara.setAlignment(ParagraphAlignment.LEFT);
            XWPFRun titleRun = titlePara.createRun();
            titleRun.setText(title);
            titleRun.setBold(true);
            titleRun.setFontSize(14);
            titleRun.addBreak();

            // Add the image
            XWPFParagraph imagePara = document.createParagraph();
            imagePara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun imageRun = imagePara.createRun();

            try (FileInputStream imageStream = new FileInputStream(diagramPath.toFile())) {
                imageRun.addPicture(imageStream,
                    XWPFDocument.PICTURE_TYPE_PNG,
                    fileName,
                    Units.toEMU(500), // Width: 500 pixels
                    Units.toEMU(300)); // Height: 300 pixels
            }

            imageRun.addBreak();
            imageRun.addBreak();

        } catch (Exception e) {
            log.error("Failed to add diagram {}: {}", diagramPath, e.getMessage());
        }
    }

    /**
     * Helper method to set cell text and styling
     */
    private void setCellText(XWPFTableCell cell, String text, boolean isHeader) {
        // Clear existing paragraphs to avoid formatting issues
        cell.removeParagraph(0);
        XWPFParagraph para = cell.addParagraph();
        
        XWPFRun run = para.createRun();
        run.setText(text);
        run.setBold(isHeader);
        
        // Explicitly disable strikethrough and other formatting
        run.setStrikeThrough(false);
        run.setDoubleStrikethrough(false);
        
        if (isHeader) {
            cell.setColor("011F3D");
            run.setColor("FFFFFF");
            para.setAlignment(ParagraphAlignment.CENTER);
        } else {
            para.setAlignment(ParagraphAlignment.LEFT);
        }
    }
}

// Made with Bob
