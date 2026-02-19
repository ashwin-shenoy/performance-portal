package com.hamza.performanceportal.performance.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/**
 * Generic service for parsing Excel files (.xls and .xlsx)
 * Automatically detects file format and extracts data
 */
@Service
@Slf4j
public class GenericExcelParser {

    /**
     * Parse Excel file and return structured data
     * 
     * @param filePath Path to Excel file (.xls or .xlsx)
     * @return ExcelData object containing all sheets and their data
     */
    public ExcelData parseExcelFile(String filePath) throws IOException {
        log.info("Parsing Excel file: {}", filePath);
        
        ExcelData excelData = new ExcelData();
        excelData.setFilePath(filePath);
        
        try (FileInputStream fis = new FileInputStream(filePath)) {
            Workbook workbook = createWorkbook(filePath, fis);
            
            // Parse all sheets
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                SheetData sheetData = parseSheet(sheet);
                excelData.addSheet(sheetData);
            }
            
            workbook.close();
        }
        
        log.info("Successfully parsed {} sheets from Excel file", excelData.getSheets().size());
        return excelData;
    }
    
    /**
     * Create appropriate Workbook based on file extension
     */
    private Workbook createWorkbook(String filePath, FileInputStream fis) throws IOException {
        if (filePath.toLowerCase().endsWith(".xlsx")) {
            return new XSSFWorkbook(fis);
        } else if (filePath.toLowerCase().endsWith(".xls")) {
            return new HSSFWorkbook(fis);
        } else {
            throw new IOException("Unsupported file format. Only .xls and .xlsx are supported.");
        }
    }
    
    /**
     * Parse a single sheet
     */
    private SheetData parseSheet(Sheet sheet) {
        SheetData sheetData = new SheetData();
        sheetData.setSheetName(sheet.getSheetName());
        
        if (sheet.getPhysicalNumberOfRows() == 0) {
            log.warn("Sheet '{}' is empty", sheet.getSheetName());
            return sheetData;
        }
        
        // Parse header row
        Row headerRow = sheet.getRow(sheet.getFirstRowNum());
        if (headerRow != null) {
            List<String> headers = parseHeaderRow(headerRow);
            sheetData.setHeaders(headers);
        }
        
        // Parse data rows
        for (int i = sheet.getFirstRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row != null && !isEmptyRow(row)) {
                Map<String, Object> rowData = parseDataRow(row, sheetData.getHeaders());
                sheetData.addRow(rowData);
            }
        }
        
        log.info("Parsed sheet '{}': {} columns, {} rows", 
                sheetData.getSheetName(), 
                sheetData.getHeaders().size(), 
                sheetData.getRows().size());
        
        return sheetData;
    }
    
    /**
     * Parse header row to get column names
     */
    private List<String> parseHeaderRow(Row headerRow) {
        List<String> headers = new ArrayList<>();
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            String header = getCellValueAsString(cell);
            headers.add(header != null ? header.trim() : "Column_" + (i + 1));
        }
        return headers;
    }
    
    /**
     * Parse data row into a map with column names as keys
     */
    private Map<String, Object> parseDataRow(Row row, List<String> headers) {
        Map<String, Object> rowData = new LinkedHashMap<>();
        
        for (int i = 0; i < headers.size(); i++) {
            Cell cell = row.getCell(i);
            String columnName = headers.get(i);
            Object value = getCellValue(cell);
            rowData.put(columnName, value);
        }
        
        return rowData;
    }
    
    /**
     * Get cell value as appropriate Java type
     */
    private Object getCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue();
                } else {
                    double numValue = cell.getNumericCellValue();
                    // Return as Long if it's a whole number, otherwise Double
                    if (numValue == (long) numValue) {
                        return (long) numValue;
                    }
                    return numValue;
                }
            case BOOLEAN:
                return cell.getBooleanCellValue();
            case FORMULA:
                try {
                    return cell.getNumericCellValue();
                } catch (Exception e) {
                    return cell.getStringCellValue();
                }
            case BLANK:
                return null;
            default:
                return null;
        }
    }
    
    /**
     * Get cell value as string
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return null;
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    double numValue = cell.getNumericCellValue();
                    if (numValue == (long) numValue) {
                        return String.valueOf((long) numValue);
                    }
                    return String.valueOf(numValue);
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return String.valueOf(cell.getNumericCellValue());
                } catch (Exception e) {
                    return cell.getStringCellValue();
                }
            case BLANK:
                return null;
            default:
                return null;
        }
    }
    
    /**
     * Check if row is empty
     */
    private boolean isEmptyRow(Row row) {
        for (int i = 0; i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String value = getCellValueAsString(cell);
                if (value != null && !value.trim().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }
    
    /**
     * Data class to hold Excel file data
     */
    public static class ExcelData {
        private String filePath;
        private List<SheetData> sheets = new ArrayList<>();
        
        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
        public List<SheetData> getSheets() { return sheets; }
        public void setSheets(List<SheetData> sheets) { this.sheets = sheets; }
        public void addSheet(SheetData sheet) { this.sheets.add(sheet); }
        
        public SheetData getSheet(int index) {
            return index < sheets.size() ? sheets.get(index) : null;
        }
        
        public SheetData getSheet(String name) {
            return sheets.stream()
                    .filter(s -> s.getSheetName().equals(name))
                    .findFirst()
                    .orElse(null);
        }
    }
    
    /**
     * Data class to hold sheet data
     */
    public static class SheetData {
        private String sheetName;
        private List<String> headers = new ArrayList<>();
        private List<Map<String, Object>> rows = new ArrayList<>();
        
        public String getSheetName() { return sheetName; }
        public void setSheetName(String sheetName) { this.sheetName = sheetName; }
        public List<String> getHeaders() { return headers; }
        public void setHeaders(List<String> headers) { this.headers = headers; }
        public List<Map<String, Object>> getRows() { return rows; }
        public void setRows(List<Map<String, Object>> rows) { this.rows = rows; }
        public void addRow(Map<String, Object> row) { this.rows.add(row); }
        
        public int getRowCount() { return rows.size(); }
        public int getColumnCount() { return headers.size(); }
    }
}

// Made with Bob
