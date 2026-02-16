package com.hamza.durandhar.performance.service;

import com.hamza.durandhar.performance.entity.TestMetric;
import com.hamza.durandhar.performance.entity.TestRun;
import com.hamza.durandhar.performance.repository.TestMetricRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for parsing JMeter results from images using OCR (Tesseract)
 * Supports parsing JMeter aggregate report tables from screenshots
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JMeterImageParser {

    private final TestMetricRepository testMetricRepository;

    /**
     * Parse JMeter results image and save to database
     */
    public ParseResult parseAndSave(TestRun testRun, MultipartFile imageFile) throws IOException, TesseractException {
        log.info("Starting OCR parsing of JMeter results image: {}", imageFile.getOriginalFilename());

        BufferedImage image = null;
        String ocrText = null;
        
        try {
            // Read image
            image = ImageIO.read(imageFile.getInputStream());
            if (image == null) {
                throw new IOException("Failed to read image file");
            }

            // Perform OCR with proper resource management
            Tesseract tesseract = new Tesseract();
            // Set tessdata path if needed (configure in application.yml)
            // tesseract.setDatapath("/usr/share/tesseract-ocr/4.00/tessdata");
            tesseract.setLanguage("eng");
            tesseract.setPageSegMode(1); // Automatic page segmentation with OSD
            tesseract.setOcrEngineMode(1); // Neural nets LSTM engine only

            try {
                ocrText = tesseract.doOCR(image);
                log.debug("OCR extracted text:\n{}", ocrText);
            } catch (TesseractException e) {
                log.error("Tesseract OCR failed: {}", e.getMessage(), e);
                throw e;
            } finally {
                // Clean up image resources
                if (image != null) {
                    image.flush();
                }
            }

            // Parse the OCR text
            List<JMeterRow> rows = parseJMeterTable(ocrText);
            log.info("Parsed {} rows from image", rows.size());

            // Save to database as metrics
            int metricCount = 0;

            for (JMeterRow row : rows) {
                if (row.getLabel() == null || row.getLabel().trim().isEmpty()) {
                    continue;
                }

                try {
                    // Create metrics from parsed row
                    List<TestMetric> metrics = createMetricsFromRow(testRun, row);
                    testMetricRepository.saveAll(metrics);
                    metricCount += metrics.size();
                } catch (Exception e) {
                    log.error("Failed to save metrics for row: {} - {}", row.getLabel(), e.getMessage());
                }
            }

            log.info("Saved {} metrics from {} rows in image", metricCount, rows.size());

            return ParseResult.builder()
                    .metricCount(metricCount)
                    .rowsParsed(rows.size())
                    .parsedRows(rows)
                    .build();
                    
        } catch (OutOfMemoryError e) {
            log.error("Out of memory during OCR processing: {}", e.getMessage());
            System.gc(); // Suggest garbage collection
            throw new IOException("Out of memory during image processing. Image may be too large.", e);
        } catch (Exception e) {
            log.error("Error during OCR processing: {}", e.getMessage(), e);
            throw e;
        } finally {
            // Ensure image is cleaned up
            if (image != null) {
                image.flush();
            }
        }
    }

    /**
     * Parse JMeter table from OCR text
     */
    private List<JMeterRow> parseJMeterTable(String ocrText) {
        List<JMeterRow> rows = new ArrayList<>();
        String[] lines = ocrText.split("\n");

        // Find table header to identify columns
        int headerIndex = findHeaderLine(lines);
        if (headerIndex == -1) {
            log.warn("Could not find JMeter table header in OCR text");
            return rows;
        }

        // Parse data rows
        for (int i = headerIndex + 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                continue;
            }

            try {
                JMeterRow row = parseRow(line);
                if (row != null && row.getLabel() != null) {
                    rows.add(row);
                }
            } catch (Exception e) {
                log.debug("Failed to parse line: {} - {}", line, e.getMessage());
            }
        }

        return rows;
    }

    /**
     * Find the header line in OCR text
     */
    private int findHeaderLine(String[] lines) {
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].toLowerCase();
            if (line.contains("label") && line.contains("samples") && 
                (line.contains("average") || line.contains("avg"))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Parse a single row from the table
     * Expected format: Label | #Samples | FAIL | Error% | Average | Min | Max | Median | 90th pct | 95th pct | 99th pct | Transactions/s | Received | Sent
     */
    private JMeterRow parseRow(String line) {
        // Split by multiple spaces or pipe characters
        String[] parts = line.split("\\s{2,}|\\|");
        
        if (parts.length < 5) {
            return null; // Not enough data
        }

        JMeterRow row = new JMeterRow();
        int index = 0;

        try {
            // Clean and parse each field
            for (String part : parts) {
                part = part.trim();
                if (part.isEmpty()) {
                    continue;
                }

                switch (index) {
                    case 0: // Label
                        row.setLabel(part);
                        break;
                    case 1: // #Samples
                        row.setSamples(parseInteger(part));
                        break;
                    case 2: // FAIL
                        row.setFail(parseInteger(part));
                        break;
                    case 3: // Error %
                        row.setErrorPercent(parseDouble(part));
                        break;
                    case 4: // Average
                        row.setAverage(parseDouble(part));
                        break;
                    case 5: // Min
                        row.setMin(parseDouble(part));
                        break;
                    case 6: // Max
                        row.setMax(parseDouble(part));
                        break;
                    case 7: // Median
                        row.setMedian(parseDouble(part));
                        break;
                    case 8: // 90th percentile
                        row.setPercentile90(parseDouble(part));
                        break;
                    case 9: // 95th percentile
                        row.setPercentile95(parseDouble(part));
                        break;
                    case 10: // 99th percentile
                        row.setPercentile99(parseDouble(part));
                        break;
                    case 11: // Throughput (Transactions/s)
                        row.setThroughput(parseDouble(part));
                        break;
                    case 12: // Received KB/sec
                        row.setReceivedKB(parseDouble(part));
                        break;
                    case 13: // Sent KB/sec
                        row.setSentKB(parseDouble(part));
                        break;
                }
                index++;
            }
        } catch (Exception e) {
            log.debug("Error parsing row: {} - {}", line, e.getMessage());
            return null;
        }

        return row;
    }

    /**
     * Parse integer from string, handling OCR errors
     */
    private Integer parseInteger(String value) {
        if (value == null || value.isEmpty()) {
            return 0;
        }
        
        // Remove common OCR errors
        value = value.replaceAll("[^0-9]", "");
        
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Parse double from string, handling OCR errors
     */
    private Double parseDouble(String value) {
        if (value == null || value.isEmpty()) {
            return 0.0;
        }
        
        // Remove common OCR errors, keep decimal point
        value = value.replaceAll("[^0-9.]", "");
        
        // Handle multiple decimal points (OCR error)
        int firstDot = value.indexOf('.');
        if (firstDot != -1 && value.indexOf('.', firstDot + 1) != -1) {
            value = value.substring(0, value.indexOf('.', firstDot + 1));
        }
        
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     * Create metrics from parsed row
     */
    private List<TestMetric> createMetricsFromRow(TestRun testRun, JMeterRow row) {
        List<TestMetric> metrics = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        String label = row.getLabel();

        // Samples
        metrics.add(createMetric(testRun, label + " - Samples",
                (double) row.getSamples(), "count", now));

        // Failures
        metrics.add(createMetric(testRun, label + " - Failures",
                (double) row.getFail(), "count", now));

        // Error Rate
        metrics.add(createMetric(testRun, label + " - Error Rate",
                row.getErrorPercent(), "%", now));

        // Average Response Time
        metrics.add(createMetric(testRun, label + " - Average Response Time",
                row.getAverage(), "ms", now));

        // Min Response Time
        metrics.add(createMetric(testRun, label + " - Min Response Time",
                row.getMin(), "ms", now));

        // Max Response Time
        metrics.add(createMetric(testRun, label + " - Max Response Time",
                row.getMax(), "ms", now));

        // Median Response Time
        metrics.add(createMetric(testRun, label + " - Median Response Time",
                row.getMedian(), "ms", now));

        // 90th Percentile
        if (row.getPercentile90() != null && row.getPercentile90() > 0) {
            metrics.add(createMetric(testRun, label + " - 90th Percentile",
                    row.getPercentile90(), "ms", now));
        }

        // 95th Percentile
        if (row.getPercentile95() != null && row.getPercentile95() > 0) {
            metrics.add(createMetric(testRun, label + " - 95th Percentile",
                    row.getPercentile95(), "ms", now));
        }

        // 99th Percentile
        if (row.getPercentile99() != null && row.getPercentile99() > 0) {
            metrics.add(createMetric(testRun, label + " - 99th Percentile",
                    row.getPercentile99(), "ms", now));
        }

        // Throughput
        metrics.add(createMetric(testRun, label + " - Throughput",
                row.getThroughput(), "req/sec", now));

        // Received KB/sec
        if (row.getReceivedKB() != null && row.getReceivedKB() > 0) {
            metrics.add(createMetric(testRun, label + " - Received KB/sec",
                    row.getReceivedKB(), "KB/sec", now));
        }

        // Sent KB/sec
        if (row.getSentKB() != null && row.getSentKB() > 0) {
            metrics.add(createMetric(testRun, label + " - Sent KB/sec",
                    row.getSentKB(), "KB/sec", now));
        }

        return metrics;
    }

    /**
     * Helper method to create a metric
     */
    private TestMetric createMetric(TestRun testRun, String name, Double value,
                                   String unit, LocalDateTime timestamp) {
        return TestMetric.builder()
                .testRun(testRun)
                .metricName(name)
                .metricValue(value)
                .unit(unit)
                .timestamp(timestamp)
                .build();
    }

    /**
     * Data class for JMeter row (public for JSON serialization)
     */
    @Data
    public static class JMeterRow {
        private String label;
        private Integer samples;
        private Integer fail;
        private Double errorPercent;
        private Double average;
        private Double min;
        private Double max;
        private Double median;
        private Double percentile90;
        private Double percentile95;
        private Double percentile99;
        private Double throughput;
        private Double receivedKB;
        private Double sentKB;
    }

    /**
     * Parse result
     */
    @Data
    @lombok.Builder
    public static class ParseResult {
        private int metricCount;
        private int rowsParsed;
        private List<JMeterRow> parsedRows;
    }
}

// Made with Bob