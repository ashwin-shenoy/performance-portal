package com.hamza.performanceportal.performance.service;

import com.hamza.performanceportal.performance.dto.*;
import com.hamza.performanceportal.performance.entity.*;
import com.hamza.performanceportal.performance.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing batch test execution and consolidated reporting
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TestBatchService {

    private final TestBatchRepository testBatchRepository;
    private final TestRunRepository testRunRepository;
    private final CapabilityRepository capabilityRepository;
    private final CapabilityTestCaseRepository testCaseRepository;
    private final ReportRepository reportRepository;
    private final PdfConversionService pdfConversionService;

    @Value("${app.reports.dir:/opt/performance-portal/reports}")
    private String reportsDirectory;

    /**
     * Create a new test batch with specified test cases
     */
    @Transactional
    public TestBatchDTO createBatch(TestBatchExecutionRequest request) {
        log.info("Creating test batch: {}", request.getBatchName());

        Capability capability = capabilityRepository.findById(request.getCapabilityId())
                .orElseThrow(() -> new RuntimeException("Capability not found: " + request.getCapabilityId()));

        List<CapabilityTestCase> testCases = new ArrayList<>();
        if (request.getTestCaseIds() != null && !request.getTestCaseIds().isEmpty()) {
            testCases = testCaseRepository.findAllById(request.getTestCaseIds());
        } else if (request.getTestCaseNames() != null && !request.getTestCaseNames().isEmpty()) {
            testCases = testCaseRepository.findByCapabilityAndTestCaseNameIn(capability, request.getTestCaseNames());
        }

        if (testCases.isEmpty()) {
            throw new RuntimeException("No test cases found for batch: " + request.getBatchName());
        }

        TestBatch batch = TestBatch.builder()
                .capability(capability)
                .batchName(request.getBatchName())
                .description(request.getDescription())
                .totalTestCases(testCases.size())
                .completedTestCases(0)
                .failedTestCases(0)
                .status(TestBatch.BatchStatus.PENDING)
                .build();

        TestBatch savedBatch = testBatchRepository.save(batch);
        log.info("Test batch created with ID: {} and Batch ID: {}", savedBatch.getId(), savedBatch.getBatchId());

        return TestBatchDTO.fromEntity(savedBatch);
    }

    /**
     * Execute all test cases in a batch
     */
    @Transactional
    public TestBatchDTO executeBatch(String batchId) {
        log.info("Starting batch execution for batchId: {}", batchId);

        TestBatch batch = testBatchRepository.findByBatchId(batchId)
                .orElseThrow(() -> new RuntimeException("Test batch not found: " + batchId));

        batch.setStatus(TestBatch.BatchStatus.IN_PROGRESS);
        batch.setStartTime(LocalDateTime.now());
        testBatchRepository.save(batch);

        try {
            // In a real scenario, this would trigger test execution via Jenkins, GitHub Actions, or direct API
            // For now, we'll simulate the batch status update
            log.info("Batch {} is now in PROGRESS", batchId);
            
            return TestBatchDTO.fromEntity(batch);
        } catch (Exception e) {
            log.error("Error executing batch: {}", batchId, e);
            batch.setStatus(TestBatch.BatchStatus.FAILED);
            testBatchRepository.save(batch);
            throw new RuntimeException("Batch execution failed: " + e.getMessage(), e);
        }
    }

    /**
     * Update batch status with test run results
     */
    @Transactional
    public void updateBatchWithTestResult(String batchId, Long testRunId, boolean success) {
        log.info("Updating batch {} with test result: testRunId={}, success={}", batchId, testRunId, success);

        TestBatch batch = testBatchRepository.findByBatchId(batchId)
                .orElseThrow(() -> new RuntimeException("Test batch not found: " + batchId));

        TestRun testRun = testRunRepository.findById(testRunId)
                .orElseThrow(() -> new RuntimeException("Test run not found: " + testRunId));

        // Associate test run with batch
        testRun.setTestBatch(batch);
        testRunRepository.save(testRun);

        // Update batch statistics
        batch.setCompletedTestCases(batch.getCompletedTestCases() + 1);
        if (!success) {
            batch.setFailedTestCases(batch.getFailedTestCases() + 1);
        }

        // Check if all tests completed
        if (batch.getCompletedTestCases().equals(batch.getTotalTestCases())) {
            finalizeBatch(batch);
        }

        testBatchRepository.save(batch);
    }

    /**
     * Finalize batch after all tests are completed
     */
    private void finalizeBatch(TestBatch batch) {
        log.info("Finalizing batch: {}", batch.getBatchId());

        batch.setEndTime(LocalDateTime.now());
        batch.setStatus(batch.getFailedTestCases() == 0 
            ? TestBatch.BatchStatus.COMPLETED 
            : TestBatch.BatchStatus.FAILED);
        
        batch.setBatchResult(batch.getFailedTestCases() == 0 ? "PASS" : "FAIL");

        // Calculate overall performance metrics
        calculateBatchMetrics(batch);
    }

    /**
     * Calculate consolidated performance metrics for the batch
     */
    private void calculateBatchMetrics(TestBatch batch) {
        List<TestRun> testRuns = testRunRepository.findByTestBatch(batch);

        if (testRuns.isEmpty()) {
            return;
        }

        // Calculate duration
        if (batch.getStartTime() != null && batch.getEndTime() != null) {
            batch.setTotalDurationSeconds(
                java.time.temporal.ChronoUnit.SECONDS.between(batch.getStartTime(), batch.getEndTime())
            );
        }

        BatchAggregate aggregate = aggregateMetrics(testRuns);
        batch.setPerformanceSummary(buildConsolidatedSummary(aggregate));
    }

    /**
     * Generate consolidated report for a completed batch
     */
    @Transactional
    public void generateConsolidatedReport(String batchId, List<String> reportTypes) {
        log.info("Generating consolidated report for batch: {}", batchId);

        TestBatch batch = testBatchRepository.findByBatchId(batchId)
                .orElseThrow(() -> new RuntimeException("Test batch not found: " + batchId));

        if (!batch.getStatus().equals(TestBatch.BatchStatus.COMPLETED) && 
            !batch.getStatus().equals(TestBatch.BatchStatus.FAILED)) {
            throw new RuntimeException("Can only generate reports for completed batches");
        }

        List<TestRun> testRuns = testRunRepository.findByTestBatch(batch);
        if (testRuns.isEmpty()) {
            throw new RuntimeException("No test runs associated with batch: " + batchId);
        }

        // Generate reports for each specified type
        for (String reportTypeName : reportTypes) {
            try {
                Report.ReportType reportType = resolveReportType(reportTypeName);
                Report report = generateReport(batch, testRuns, reportType);
                batch.getConsolidatedReports().add(report);
            } catch (Exception e) {
                log.error("Failed to generate report type: {}", reportTypeName, e);
            }
        }

        testBatchRepository.save(batch);
        log.info("Consolidated reports generated for batch: {}", batchId);
    }

    /**
     * Generate individual report for batch
     */
    private Report generateReport(TestBatch batch, List<TestRun> testRuns, Report.ReportType reportType) throws Exception {
        log.info("Generating {} report for batch: {}", reportType, batch.getBatchId());

        Path batchReportDir = Paths.get(reportsDirectory, batch.getBatchId());
        Files.createDirectories(batchReportDir);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String baseFileName = String.format("%s_consolidated_%s_%s",
                sanitizeFileName(batch.getBatchName()),
                reportType.name().toLowerCase(),
                timestamp);

        Path reportPath;
        if (reportType == Report.ReportType.TECHNICAL_WORD || reportType == Report.ReportType.EXECUTIVE_WORD) {
            reportPath = batchReportDir.resolve(baseFileName + ".docx");
            writeConsolidatedWordReport(batch, testRuns, reportPath, reportType);
        } else if (reportType == Report.ReportType.TECHNICAL_PDF
                || reportType == Report.ReportType.EXECUTIVE_PDF
                || reportType == Report.ReportType.CAPABILITY_PDF) {
            Path docxPath = batchReportDir.resolve(baseFileName + ".docx");
            reportPath = batchReportDir.resolve(baseFileName + ".pdf");
            writeConsolidatedWordReport(batch, testRuns, docxPath, reportType);
            boolean converted = pdfConversionService.convertDocxToPdf(docxPath.toString(), reportPath.toString());
            if (!converted) {
                throw new RuntimeException("Failed DOCX->PDF conversion for batch report");
            }
        } else if (reportType == Report.ReportType.TECHNICAL_HTML) {
            reportPath = batchReportDir.resolve(baseFileName + ".html");
            writeConsolidatedHtmlReport(batch, testRuns, reportPath);
        } else if (reportType == Report.ReportType.RAW_DATA_CSV) {
            reportPath = batchReportDir.resolve(baseFileName + ".csv");
            writeConsolidatedCsvReport(testRuns, reportPath);
        } else {
            throw new RuntimeException("Unsupported report type: " + reportType);
        }

        // Keep relation with one test run due to non-null test_run_id constraint in reports table
        TestRun representativeRun = testRuns.get(0);

        Report report = Report.builder()
                .testRun(representativeRun)
                .testBatch(batch)
                .reportType(reportType)
                .fileName(reportPath.getFileName().toString())
                .filePath(reportPath.toString())
                .fileSize(Files.size(reportPath))
                .generatedBy("BATCH_SERVICE")
                .description("Consolidated report for batch: " + batch.getBatchName())
                .build();

        return reportRepository.save(report);
    }

    private void writeConsolidatedWordReport(TestBatch batch, List<TestRun> testRuns, Path outputPath, Report.ReportType reportType) throws Exception {
        try (XWPFDocument document = new XWPFDocument(); FileOutputStream out = new FileOutputStream(outputPath.toFile())) {
            XWPFParagraph titleParagraph = document.createParagraph();
            XWPFRun titleRun = titleParagraph.createRun();
            titleRun.setBold(true);
            titleRun.setFontSize(16);
            titleRun.setText("Consolidated Batch Report");

            XWPFParagraph metaParagraph = document.createParagraph();
            XWPFRun metaRun = metaParagraph.createRun();
            metaRun.setText("Batch Name: " + batch.getBatchName());
            metaRun.addBreak();
            metaRun.setText("Batch ID: " + batch.getBatchId());
            metaRun.addBreak();
            metaRun.setText("Capability: " + (batch.getCapability() != null ? batch.getCapability().getName() : "N/A"));
            metaRun.addBreak();
            metaRun.setText("Report Type: " + reportType.name());
            metaRun.addBreak();
            metaRun.setText("Generated At: " + LocalDateTime.now());

            XWPFParagraph summaryParagraph = document.createParagraph();
            XWPFRun summaryRun = summaryParagraph.createRun();
            summaryRun.setBold(true);
            summaryRun.setText("Performance Summary");

            XWPFParagraph dataParagraph = document.createParagraph();
            XWPFRun dataRun = dataParagraph.createRun();
            dataRun.setText(buildConsolidatedSummary(testRuns));

            XWPFParagraph runsParagraph = document.createParagraph();
            XWPFRun runsRun = runsParagraph.createRun();
            runsRun.setBold(true);
            runsRun.setText("Included Test Runs");

            for (TestRun run : testRuns) {
                XWPFParagraph runParagraph = document.createParagraph();
                XWPFRun runLine = runParagraph.createRun();
                runLine.setText(String.format(
                        "- %s | status=%s | avg=%.2f ms | throughput=%.2f req/s | error=%.2f%% | podCpu(avg/max)=%.2f/%.2f%% | podMem(avg/max)=%.2f/%.2f%% | jvmHeap=%.2f%% | jvmGcP95=%.2f ms | jvmCpu=%.2f%%",
                        valueOrDefault(run.getTestName(), "N/A"),
                        run.getStatus() != null ? run.getStatus().name() : "N/A",
                        numberOrDefault(run.getAvgResponseTime()),
                        numberOrDefault(run.getThroughput()),
                        numberOrDefault(run.getErrorRate()),
                        numberOrDefault(run.getPodCpuAvg()),
                        numberOrDefault(run.getPodCpuMax()),
                        numberOrDefault(run.getPodMemoryAvg()),
                        numberOrDefault(run.getPodMemoryMax()),
                        numberOrDefault(run.getJvmHeapUsedPercentAvg()),
                        numberOrDefault(run.getJvmGcPauseMsP95()),
                        numberOrDefault(run.getJvmProcessCpuAvg())
                ));
            }

            document.write(out);
        }
    }

    private void writeConsolidatedHtmlReport(TestBatch batch, List<TestRun> testRuns, Path outputPath) throws Exception {
        StringBuilder html = new StringBuilder();
        html.append("<html><head><title>Batch Report</title></head><body>")
                .append("<h1>Consolidated Batch Report</h1>")
                .append("<p><strong>Batch Name:</strong> ").append(batch.getBatchName()).append("</p>")
                .append("<p><strong>Batch ID:</strong> ").append(batch.getBatchId()).append("</p>")
                .append("<p><strong>Summary:</strong> ").append(buildConsolidatedSummary(testRuns)).append("</p>")
                .append("<h2>Test Runs</h2><ul>");

        for (TestRun run : testRuns) {
            html.append("<li>")
                    .append(valueOrDefault(run.getTestName(), "N/A"))
                    .append(" | status=").append(run.getStatus() != null ? run.getStatus().name() : "N/A")
                    .append(" | avg=").append(String.format("%.2f", numberOrDefault(run.getAvgResponseTime()))).append(" ms")
                    .append(" | throughput=").append(String.format("%.2f", numberOrDefault(run.getThroughput()))).append(" req/s")
                    .append(" | error=").append(String.format("%.2f", numberOrDefault(run.getErrorRate()))).append("%")
                    .append(" | podCpu(avg/max)=").append(String.format("%.2f/%.2f%%", numberOrDefault(run.getPodCpuAvg()), numberOrDefault(run.getPodCpuMax())))
                    .append(" | podMem(avg/max)=").append(String.format("%.2f/%.2f%%", numberOrDefault(run.getPodMemoryAvg()), numberOrDefault(run.getPodMemoryMax())))
                    .append(" | jvmHeap=").append(String.format("%.2f%%", numberOrDefault(run.getJvmHeapUsedPercentAvg())))
                    .append(" | jvmGcP95=").append(String.format("%.2f ms", numberOrDefault(run.getJvmGcPauseMsP95())))
                    .append(" | jvmCpu=").append(String.format("%.2f%%", numberOrDefault(run.getJvmProcessCpuAvg())))
                    .append("</li>");
        }

        html.append("</ul></body></html>");
        Files.writeString(outputPath, html.toString());
    }

    private void writeConsolidatedCsvReport(List<TestRun> testRuns, Path outputPath) throws Exception {
        StringBuilder csv = new StringBuilder("testRunId,testName,status,totalRequests,avgResponseTime,throughput,errorRate\n");
        for (TestRun run : testRuns) {
            csv.append(run.getId() != null ? run.getId() : "")
                    .append(',')
                    .append(escapeCsv(valueOrDefault(run.getTestName(), "")))
                    .append(',')
                    .append(run.getStatus() != null ? run.getStatus().name() : "")
                    .append(',')
                    .append(run.getTotalRequests() != null ? run.getTotalRequests() : 0)
                    .append(',')
                    .append(String.format("%.2f", numberOrDefault(run.getAvgResponseTime())))
                    .append(',')
                    .append(String.format("%.2f", numberOrDefault(run.getThroughput())))
                    .append(',')
                    .append(String.format("%.2f", numberOrDefault(run.getErrorRate())))
                    .append('\n');
        }
        Files.writeString(outputPath, csv.toString());
    }

    private Report.ReportType resolveReportType(String reportType) {
        if (reportType == null || reportType.isBlank()) {
            return Report.ReportType.TECHNICAL_PDF;
        }
        try {
            return Report.ReportType.valueOf(reportType.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("Invalid report type: " + reportType);
        }
    }

    private String buildConsolidatedSummary(List<TestRun> testRuns) {
        return buildConsolidatedSummary(aggregateMetrics(testRuns));
    }

    private String buildConsolidatedSummary(BatchAggregate aggregate) {
        return String.format(
                "Total Requests: %d | Avg Response Time: %.2f ms | Avg Throughput: %.2f req/s | Overall Error Rate: %.2f%% | Pod CPU Avg: %.2f%% | Pod Memory Avg: %.2f%% | JVM Heap Avg: %.2f%% | JVM GC P95 Avg: %.2f ms | JVM CPU Avg: %.2f%%",
                aggregate.totalRequests(),
                aggregate.avgResponseTime(),
                aggregate.avgThroughput(),
                aggregate.avgErrorRate(),
                aggregate.avgPodCpu(),
                aggregate.avgPodMemory(),
                aggregate.avgJvmHeap(),
                aggregate.avgJvmGcP95(),
                aggregate.avgJvmCpu()
        );
    }

    private BatchAggregate aggregateMetrics(List<TestRun> testRuns) {
        long totalRequests = testRuns.stream().mapToLong(tr -> tr.getTotalRequests() != null ? tr.getTotalRequests() : 0L).sum();
        double avgResponseTime = testRuns.stream().mapToDouble(tr -> numberOrDefault(tr.getAvgResponseTime())).average().orElse(0.0);
        double avgThroughput = testRuns.stream().mapToDouble(tr -> numberOrDefault(tr.getThroughput())).average().orElse(0.0);
        double avgErrorRate = testRuns.stream().mapToDouble(tr -> numberOrDefault(tr.getErrorRate())).average().orElse(0.0);
        double avgPodCpu = testRuns.stream().mapToDouble(tr -> numberOrDefault(tr.getPodCpuAvg())).average().orElse(0.0);
        double avgPodMemory = testRuns.stream().mapToDouble(tr -> numberOrDefault(tr.getPodMemoryAvg())).average().orElse(0.0);
        double avgJvmHeap = testRuns.stream().mapToDouble(tr -> numberOrDefault(tr.getJvmHeapUsedPercentAvg())).average().orElse(0.0);
        double avgJvmGcP95 = testRuns.stream().mapToDouble(tr -> numberOrDefault(tr.getJvmGcPauseMsP95())).average().orElse(0.0);
        double avgJvmCpu = testRuns.stream().mapToDouble(tr -> numberOrDefault(tr.getJvmProcessCpuAvg())).average().orElse(0.0);
        return new BatchAggregate(totalRequests, avgResponseTime, avgThroughput, avgErrorRate, avgPodCpu, avgPodMemory, avgJvmHeap, avgJvmGcP95, avgJvmCpu);
    }

    private record BatchAggregate(
            long totalRequests,
            double avgResponseTime,
            double avgThroughput,
            double avgErrorRate,
            double avgPodCpu,
            double avgPodMemory,
            double avgJvmHeap,
            double avgJvmGcP95,
            double avgJvmCpu
    ) {}

    private String sanitizeFileName(String name) {
        if (name == null || name.isBlank()) {
            return "batch_report";
        }
        return name.trim().replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private double numberOrDefault(Double value) {
        return value != null ? value : 0.0;
    }

    private String escapeCsv(String value) {
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    /**
     * Get batch status
     */
    public TestBatchDTO getBatchStatus(String batchId) {
        TestBatch batch = testBatchRepository.findByBatchId(batchId)
                .orElseThrow(() -> new RuntimeException("Test batch not found: " + batchId));

        TestBatchDTO dto = TestBatchDTO.fromEntity(batch);
        
        // Get associated test runs
        List<TestRun> testRuns = testRunRepository.findByTestBatch(batch);
        dto.setTestRuns(testRuns.stream().map(TestBatchRunDTO::fromEntity).collect(Collectors.toList()));

        // Get consolidated reports
        dto.setConsolidatedReports(batch.getConsolidatedReports().stream()
                .map(BatchReportDTO::fromEntity)
                .collect(Collectors.toList()));

        return dto;
    }

    /**
     * Get all batches for a capability
     */
    public List<TestBatchDTO> getBatchesByCapability(Long capabilityId) {
        return testBatchRepository.findByCapabilityId(capabilityId).stream()
                .map(TestBatchDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get file extension based on report type
     */
    private String getFileExtension(String reportType) {
        return switch (reportType.toUpperCase()) {
            case "TECHNICAL_PDF", "EXECUTIVE_PDF", "CAPABILITY_PDF" -> "pdf";
            case "TECHNICAL_WORD", "EXECUTIVE_WORD" -> "docx";
            case "TECHNICAL_HTML" -> "html";
            case "RAW_DATA_CSV" -> "csv";
            default -> "txt";
        };
    }
}
