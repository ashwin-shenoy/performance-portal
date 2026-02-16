package com.hamza.durandhar.performance.service;

import com.hamza.durandhar.performance.entity.*;
import com.hamza.durandhar.performance.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for generating Excel reports from test run data
 * Creates comprehensive Excel workbooks with test results, metrics, and analysis
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelReportGenerationService {

    private final TestRunRepository testRunRepository;
    private final TestMetricRepository testMetricRepository;
    private final TestTransactionRepository testTransactionRepository;
    private final ReportRepository reportRepository;

    /**
     * Generate Excel report for a test run
     *
     * @param testRunId ID of the test run
     * @param generatedBy User who generated the report
     * @return Generated Report entity
     */
    public Report generateExcelReport(Long testRunId, String generatedBy) throws Exception {
        log.info("Generating Excel report for test run ID: {}", testRunId);

        TestRun testRun = testRunRepository.findById(testRunId)
                .orElseThrow(() -> new RuntimeException("Test run not found: " + testRunId));

        // Create temp directory for report generation
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "performance-reports");
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }

        // Generate the Excel workbook
        File excelFile = createExcelWorkbook(tempDir, testRun);

        // Save report metadata to database
        Report report = new Report();
        report.setTestRun(testRun);
        report.setReportType(Report.ReportType.RAW_DATA_CSV);
        report.setFileName(excelFile.getName());
        report.setFilePath(excelFile.getAbsolutePath());
        report.setFileSize(excelFile.length());
        report.setGeneratedBy(generatedBy);
        report.setGeneratedAt(LocalDateTime.now());
        report.setDescription("Excel performance report with detailed metrics and analysis");

        report = reportRepository.save(report);
        log.info("Excel report created successfully: {}", report.getFileName());

        return report;
    }

    /**
     * Create Excel workbook with test results
     */
    private File createExcelWorkbook(File tempDir, TestRun testRun) throws Exception {
        log.info("Creating Excel workbook for test run: {}", testRun.getTestName());

        Workbook workbook = new XSSFWorkbook();

        // Create styles
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle passedStyle = createPassedStyle(workbook);
        CellStyle failedStyle = createFailedStyle(workbook);
        CellStyle warningStyle = createWarningStyle(workbook);

        // Create sheets
        createSummarySheet(workbook, testRun, headerStyle);
        createMetricsSheet(workbook, testRun, headerStyle, passedStyle, failedStyle, warningStyle);
        createTransactionsSheet(workbook, testRun, headerStyle);

        // Save workbook
        String fileName = String.format("%s_%s_Report.xlsx",
                testRun.getTestName().replaceAll("\\s+", "_"),
                testRun.getId());
        File outputFile = new File(tempDir, fileName);

        try (FileOutputStream out = new FileOutputStream(outputFile)) {
            workbook.write(out);
        }

        workbook.close();
        return outputFile;
    }

    /**
     * Create summary sheet with test overview
     */
    private void createSummarySheet(Workbook workbook, TestRun testRun, CellStyle headerStyle) {
        Sheet sheet = workbook.createSheet("Test Summary");

        int rowNum = 0;

        // Title
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Performance Test Summary");
        titleCell.setCellStyle(headerStyle);

        rowNum++; // Empty row

        // Test details
        addDataRow(sheet, rowNum++, "Test Name", testRun.getTestName());
        addDataRow(sheet, rowNum++, "File Type", testRun.getFileType().toString());
        addDataRow(sheet, rowNum++, "Test Date", testRun.getTestDate().toString());
        addDataRow(sheet, rowNum++, "Status", testRun.getStatus().toString());
        
        if (testRun.getCapability() != null) {
            addDataRow(sheet, rowNum++, "Capability", testRun.getCapability().getName());
        }
        
        if (testRun.getDescription() != null) {
            addDataRow(sheet, rowNum++, "Description", testRun.getDescription());
        }

        // Auto-size columns
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    /**
     * Create metrics sheet with performance metrics
     */
    private void createMetricsSheet(Workbook workbook, TestRun testRun, CellStyle headerStyle,
                                     CellStyle passedStyle, CellStyle failedStyle, CellStyle warningStyle) {
        Sheet sheet = workbook.createSheet("Performance Metrics");

        // Header row
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Metric Name", "Value", "Unit", "Threshold", "Status"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Fetch metrics from database
        List<TestMetric> metrics = testMetricRepository.findByTestRunId(testRun.getId());

        int rowNum = 1;
        for (TestMetric metric : metrics) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(metric.getMetricName());
            row.createCell(1).setCellValue(metric.getMetricValue());
            row.createCell(2).setCellValue(metric.getUnit() != null ? metric.getUnit() : "");
            row.createCell(3).setCellValue("N/A"); // Threshold not in entity
            
            Cell statusCell = row.createCell(4);
            String status = "N/A"; // Status not in entity
            statusCell.setCellValue(status);
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * Create transactions sheet with transaction details
     */
    private void createTransactionsSheet(Workbook workbook, TestRun testRun, CellStyle headerStyle) {
        Sheet sheet = workbook.createSheet("Transactions");

        // Header row
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Transaction Name", "Total Count", "Passed", "Failed", 
                           "Avg Response Time (ms)", "Min (ms)", "Max (ms)", "Throughput"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Fetch transactions from database
        List<TestTransaction> transactions = testTransactionRepository.findByTestRunId(testRun.getId());

        int rowNum = 1;
        for (TestTransaction transaction : transactions) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(transaction.getTransactionName());
            row.createCell(1).setCellValue(0); // Total count not in entity
            row.createCell(2).setCellValue(transaction.getSuccess() ? 1 : 0); // Passed count approximation
            row.createCell(3).setCellValue(transaction.getSuccess() ? 0 : 1); // Failed count approximation
            row.createCell(4).setCellValue(transaction.getResponseTime() != null ? transaction.getResponseTime() : 0.0);
            row.createCell(5).setCellValue(0.0); // Min response time not in entity
            row.createCell(6).setCellValue(0.0); // Max response time not in entity
            row.createCell(7).setCellValue(0.0); // Throughput not in entity
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * Helper method to add a data row with label and value
     */
    private void addDataRow(Sheet sheet, int rowNum, String label, String value) {
        Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(value);
    }

    /**
     * Create header cell style
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        
        return style;
    }

    /**
     * Create passed/success cell style
     */
    private CellStyle createPassedStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    /**
     * Create failed/error cell style
     */
    private CellStyle createFailedStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.RED.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    /**
     * Create warning cell style
     */
    private CellStyle createWarningStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }
}

// Made with Bob
