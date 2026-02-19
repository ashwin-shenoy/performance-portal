package com.hamza.performanceportal.performance.demo;

import com.hamza.performanceportal.performance.service.ExcelToWordReportService;
import com.hamza.performanceportal.performance.service.GenericExcelParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Demo class showing how to parse Excel and generate Word reports
 * 
 * To run this demo:
 * 1. Uncomment the @Component annotation
 * 2. Update the file paths
 * 3. Run the Spring Boot application
 */
@Slf4j
@RequiredArgsConstructor
// @Component  // Uncomment to enable auto-run on startup
public class ExcelToWordDemo implements CommandLineRunner {

    private final GenericExcelParser excelParser;
    private final ExcelToWordReportService reportService;

    @Override
    public void run(String... args) throws Exception {
        log.info("=== Excel to Word Report Demo ===");
        
        // Example 1: Parse Excel file and inspect data
        String excelFilePath = "/Users/ashwinshenoy/Downloads/IS_12.0.0.0.619-baseline.xls";
        
        log.info("Parsing Excel file: {}", excelFilePath);
        GenericExcelParser.ExcelData excelData = excelParser.parseExcelFile(excelFilePath);
        
        // Print summary
        log.info("Excel file contains {} sheets", excelData.getSheets().size());
        
        for (GenericExcelParser.SheetData sheet : excelData.getSheets()) {
            log.info("Sheet: {} - {} rows, {} columns", 
                    sheet.getSheetName(), 
                    sheet.getRowCount(), 
                    sheet.getColumnCount());
            
            // Print headers
            log.info("  Headers: {}", sheet.getHeaders());
            
            // Print first row as sample
            if (!sheet.getRows().isEmpty()) {
                log.info("  Sample row: {}", sheet.getRows().get(0));
            }
        }
        
        // Example 2: Generate Word report
        String outputPath = "/Users/ashwinshenoy/Downloads/IS_Report.docx";
        String reportTitle = "Integration Server Performance Baseline Report";
        
        log.info("Generating Word report...");
        String generatedPath = reportService.generateReport(excelFilePath, outputPath, reportTitle);
        
        log.info("✓ Word report generated successfully: {}", generatedPath);
        log.info("=== Demo Complete ===");
    }
    
    /**
     * Alternative: Manual usage example (can be called from a controller)
     */
    public void manualExample() throws Exception {
        // Step 1: Parse Excel
        GenericExcelParser.ExcelData data = excelParser.parseExcelFile(
                "/path/to/your/file.xls"
        );
        
        // Step 2: Access parsed data
        GenericExcelParser.SheetData firstSheet = data.getSheet(0);
        log.info("First sheet has {} rows", firstSheet.getRowCount());
        
        // Step 3: Generate Word report
        reportService.generateReport(
                "/path/to/input.xls",
                "/path/to/output.docx",
                "My Report Title"
        );
    }
}

// Made with Bob
