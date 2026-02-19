package com.hamza.performanceportal.performance.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Service to generate Word reports from Excel data
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ExcelToWordReportService {

    private final GenericExcelParser excelParser;

    /**
     * Generate Word report from Excel file
     * 
     * @param excelFilePath Path to input Excel file
     * @param outputWordPath Path for output Word document
     * @param reportTitle Title for the report
     * @return Path to generated Word document
     */
    public String generateReport(String excelFilePath, String outputWordPath, String reportTitle) 
            throws IOException {
        
        log.info("Generating Word report from Excel file: {}", excelFilePath);
        
        // Parse Excel file
        GenericExcelParser.ExcelData excelData = excelParser.parseExcelFile(excelFilePath);
        
        // Create Word document
        XWPFDocument document = new XWPFDocument();
        
        // Add cover page
        addCoverPage(document, reportTitle, excelFilePath);
        
        // Add table of contents placeholder
        addTableOfContents(document);
        
        // Add each sheet as a section
        for (GenericExcelParser.SheetData sheet : excelData.getSheets()) {
            addSheetSection(document, sheet);
        }
        
        // Add summary section
        addSummarySection(document, excelData);
        
        // Save document
        try (FileOutputStream out = new FileOutputStream(outputWordPath)) {
            document.write(out);
        }
        document.close();
        
        log.info("Word report generated successfully: {}", outputWordPath);
        return outputWordPath;
    }
    
    /**
     * Add cover page
     */
    private void addCoverPage(XWPFDocument document, String title, String sourceFile) {
        // Title
        XWPFParagraph titlePara = document.createParagraph();
        titlePara.setAlignment(ParagraphAlignment.CENTER);
        titlePara.setSpacingAfter(400);
        
        XWPFRun titleRun = titlePara.createRun();
        titleRun.setText(title);
        titleRun.setBold(true);
        titleRun.setFontSize(28);
        titleRun.setFontFamily("Arial");
        titleRun.addBreak();
        titleRun.addBreak();
        
        // Subtitle
        XWPFParagraph subtitlePara = document.createParagraph();
        subtitlePara.setAlignment(ParagraphAlignment.CENTER);
        subtitlePara.setSpacingAfter(200);
        
        XWPFRun subtitleRun = subtitlePara.createRun();
        subtitleRun.setText("Excel Data Report");
        subtitleRun.setFontSize(18);
        subtitleRun.setFontFamily("Arial");
        subtitleRun.addBreak();
        
        // Source file info
        XWPFParagraph sourcePara = document.createParagraph();
        sourcePara.setAlignment(ParagraphAlignment.CENTER);
        sourcePara.setSpacingAfter(200);
        
        XWPFRun sourceRun = sourcePara.createRun();
        sourceRun.setText("Source: " + sourceFile);
        sourceRun.setFontSize(12);
        sourceRun.setFontFamily("Arial");
        sourceRun.setItalic(true);
        sourceRun.addBreak();
        
        // Date
        XWPFRun dateRun = sourcePara.createRun();
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy HH:mm"));
        dateRun.setText("Generated: " + dateStr);
        dateRun.setFontSize(12);
        dateRun.setFontFamily("Arial");
        dateRun.setItalic(true);
        
        // Page break
        document.createParagraph().createRun().addBreak(BreakType.PAGE);
    }
    
    /**
     * Add table of contents placeholder
     */
    private void addTableOfContents(XWPFDocument document) {
        XWPFParagraph tocPara = document.createParagraph();
        XWPFRun tocRun = tocPara.createRun();
        tocRun.setText("Table of Contents");
        tocRun.setBold(true);
        tocRun.setFontSize(20);
        tocRun.addBreak();
        tocRun.addBreak();
        
        XWPFParagraph tocNote = document.createParagraph();
        XWPFRun noteRun = tocNote.createRun();
        noteRun.setText("(Right-click and select 'Update Field' to generate table of contents)");
        noteRun.setItalic(true);
        noteRun.setFontSize(10);
        
        document.createParagraph().createRun().addBreak(BreakType.PAGE);
    }
    
    /**
     * Add sheet data as a section
     */
    private void addSheetSection(XWPFDocument document, GenericExcelParser.SheetData sheet) {
        // Section heading
        XWPFParagraph heading = document.createParagraph();
        heading.setStyle("Heading1");
        XWPFRun headingRun = heading.createRun();
        headingRun.setText(sheet.getSheetName());
        headingRun.setBold(true);
        headingRun.setFontSize(18);
        headingRun.addBreak();
        
        // Sheet info
        XWPFParagraph info = document.createParagraph();
        XWPFRun infoRun = info.createRun();
        infoRun.setText(String.format("Total Rows: %d | Total Columns: %d", 
                sheet.getRowCount(), sheet.getColumnCount()));
        infoRun.setFontSize(11);
        infoRun.setItalic(true);
        infoRun.addBreak();
        
        // Create table
        if (!sheet.getRows().isEmpty()) {
            createDataTable(document, sheet);
        } else {
            XWPFParagraph emptyNote = document.createParagraph();
            XWPFRun emptyRun = emptyNote.createRun();
            emptyRun.setText("(No data available in this sheet)");
            emptyRun.setItalic(true);
        }
        
        // Add spacing
        document.createParagraph().createRun().addBreak();
    }
    
    /**
     * Create data table from sheet data
     */
    private void createDataTable(XWPFDocument document, GenericExcelParser.SheetData sheet) {
        List<String> headers = sheet.getHeaders();
        List<Map<String, Object>> rows = sheet.getRows();
        
        // Create table
        XWPFTable table = document.createTable();
        table.setWidth("100%");
        
        // Set table width to 100%
        CTTblWidth tblWidth = table.getCTTbl().addNewTblPr().addNewTblW();
        tblWidth.setType(STTblWidth.PCT);
        tblWidth.setW(BigInteger.valueOf(5000));
        
        // Header row
        XWPFTableRow headerRow = table.getRow(0);
        styleHeaderRow(headerRow);
        
        for (int i = 0; i < headers.size(); i++) {
            XWPFTableCell cell = i == 0 ? headerRow.getCell(0) : headerRow.addNewTableCell();
            cell.setText(headers.get(i));
            cell.setColor("4472C4"); // Blue background
            
            // Set text color to white
            XWPFParagraph para = cell.getParagraphs().get(0);
            XWPFRun run = para.getRuns().get(0);
            run.setBold(true);
            run.setColor("FFFFFF");
        }
        
        // Data rows (limit to first 100 rows to avoid huge documents)
        int maxRows = Math.min(rows.size(), 100);
        for (int i = 0; i < maxRows; i++) {
            Map<String, Object> rowData = rows.get(i);
            XWPFTableRow dataRow = table.createRow();
            
            for (int j = 0; j < headers.size(); j++) {
                String header = headers.get(j);
                Object value = rowData.get(header);
                String cellValue = value != null ? value.toString() : "";
                
                XWPFTableCell cell = dataRow.getCell(j);
                cell.setText(cellValue);
                
                // Alternate row colors
                if (i % 2 == 0) {
                    cell.setColor("E7E6E6"); // Light gray
                }
            }
        }
        
        // Add note if rows were truncated
        if (rows.size() > maxRows) {
            XWPFParagraph note = document.createParagraph();
            XWPFRun noteRun = note.createRun();
            noteRun.setText(String.format("Note: Showing first %d of %d rows", maxRows, rows.size()));
            noteRun.setItalic(true);
            noteRun.setFontSize(10);
            noteRun.addBreak();
        }
    }
    
    /**
     * Style header row
     */
    private void styleHeaderRow(XWPFTableRow row) {
        row.setHeight(400);
    }
    
    /**
     * Add summary section
     */
    private void addSummarySection(XWPFDocument document, GenericExcelParser.ExcelData excelData) {
        document.createParagraph().createRun().addBreak(BreakType.PAGE);
        
        // Summary heading
        XWPFParagraph heading = document.createParagraph();
        XWPFRun headingRun = heading.createRun();
        headingRun.setText("Summary");
        headingRun.setBold(true);
        headingRun.setFontSize(18);
        headingRun.addBreak();
        
        // Summary content
        XWPFParagraph summary = document.createParagraph();
        XWPFRun summaryRun = summary.createRun();
        
        summaryRun.setText("Total Sheets: " + excelData.getSheets().size());
        summaryRun.addBreak();
        
        int totalRows = excelData.getSheets().stream()
                .mapToInt(GenericExcelParser.SheetData::getRowCount)
                .sum();
        summaryRun.setText("Total Data Rows: " + totalRows);
        summaryRun.addBreak();
        summaryRun.addBreak();
        
        // Sheet breakdown
        summaryRun.setText("Sheet Breakdown:");
        summaryRun.setBold(true);
        summaryRun.addBreak();
        
        for (GenericExcelParser.SheetData sheet : excelData.getSheets()) {
            XWPFRun sheetRun = summary.createRun();
            sheetRun.setText(String.format("  • %s: %d rows, %d columns",
                    sheet.getSheetName(), sheet.getRowCount(), sheet.getColumnCount()));
            sheetRun.addBreak();
        }
    }
}

// Made with Bob
