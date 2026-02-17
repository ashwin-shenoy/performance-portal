package com.hamza.durandhar.performance.service;

import com.hamza.durandhar.performance.entity.*;
import com.hamza.durandhar.performance.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.util.Units;
import org.apache.poi.wp.usermodel.HeaderFooterType;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.xmlbeans.XmlCursor;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for generating professional Word document performance test reports
 * using the Hamza ReportTemplate2.docx corporate template.
 *
 * Template structure (5 sections):
 *   Section 0: Cover page (Hamza branding in headers)
 *   Section 1: Copyright / legal page
 *   Section 2-4: Main content pages (Heading1/Heading2/Heading3 numbered styles,
 *                BodyText, ListParagraphHamzaBulleted, Hamza Aptos 11pt)
 *
 * The generator opens the template, clears the body content, then writes
 * all 11 report sections using the template's built-in styles so headers,
 * footers, logos, and formatting are preserved.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WordReportGenerationService {

    private final TestRunRepository testRunRepository;
    private final TestArtifactRepository testArtifactRepository;
    private final TestMetricRepository testMetricRepository;
    private final TestTransactionRepository testTransactionRepository;
    private final ReportRepository reportRepository;
    private final PdfConversionService pdfConversionService;
    private final CapabilityTestCaseRepository capabilityTestCaseRepository;

    @Value("${app.reports.dir:/opt/performance-portal/reports}")
    private String reportsDirectory;

    @Value("${app.reports.template-dir:/opt/performance-portal/templates}")
    private String templatesDirectory;

    @Value("${app.diagrams.dir:/opt/performance-portal/diagrams}")
    private String diagramsDirectory;

    // ========================================================================
    // Template style names (must match ReportTemplate2.docx)
    // ========================================================================
    private static final String STYLE_HEADING1 = "Heading1";
    private static final String STYLE_HEADING2 = "Heading2";
    private static final String STYLE_HEADING3 = "Heading3";
    private static final String STYLE_BODY_TEXT = "BodyText";
    private static final String STYLE_LIST_BULLET = "ListParagraphHamzaBulleted";
    private static final String STYLE_LIST_PARAGRAPH = "ListParagraph";
    private static final String STYLE_FOOTER = "Footer";
    private static final String STYLE_TOC1 = "TOC1";
    private static final String STYLE_TOC2 = "TOC2";
    private static final String STYLE_NORMAL = "Normal";

    // Table styling colors
    private static final String COLOR_HEADER_BG = "1F4E79";
    private static final String COLOR_HEADER_TEXT = "FFFFFF";
    private static final String COLOR_ROW_EVEN = "D6E4F0";
    private static final String COLOR_ROW_ODD = "FFFFFF";
    private static final String COLOR_SUCCESS = "27AE60";
    private static final String COLOR_DANGER = "E74C3C";
    private static final String COLOR_WARNING = "F39C12";
    private static final String COLOR_ACCENT = "2C3E50";
    private static final String FONT_FAMILY = "Aptos";

    private static final String TEMPLATE_FILE = "templates/ReportTemplate2.docx";

    // ========================================================================
    // PUBLIC API
    // ========================================================================

    @Transactional
    public Report generateWordReport(Long testRunId, String generatedBy, boolean includeBaseline, boolean includeRegression) throws Exception {
        log.info("Generating Word report for test run ID: {}", testRunId);

        TestRun testRun = testRunRepository.findById(testRunId)
                .orElseThrow(() -> new RuntimeException("Test run not found: " + testRunId));

        List<String> missingFields = getMissingCoverFields(testRun);
        if (!missingFields.isEmpty()) {
            throw new IllegalStateException("Missing required fields for cover page: " + String.join(", ", missingFields));
        }

        Path reportDir = Paths.get(reportsDirectory);
        if (!Files.exists(reportDir)) {
            Files.createDirectories(reportDir);
        }

        // Open the Hamza corporate template
        XWPFDocument document = loadTemplate();
        Capability capability = testRun.getCapability();

        // Preserve cover page (sect 0), copyright (sect 1), TOC (sect 2); clear only main content placeholder
        // Headers, footers, logos remain untouched
        clearBodyContentPreservingTemplateStructure(document);

        // Add capability title to cover page area
        addCoverPageTitle(document, capability, testRun);

        // Do NOT add static TOC - template's TOC field is preserved and will update when user right-clicks "Update Field"
        addPageBreak(document);

        // 1. Executive Summary (with dynamic SLA verdict)
        addExecutiveSummary(document, testRun);
        addPageBreak(document);

        // 2. Test Objective & Scope
        addTestObjectiveAndScope(document, testRun);
        addPageBreak(document);

        // 3. Test Environment & Architecture
        addTestEnvironmentAndArchitecture(document, testRun);
        addPageBreak(document);

        // 4. Test Cases
        addTestCasesSection(document, testRun);
        addPageBreak(document);

        // 5. Test Configuration
        addTestConfiguration(document, testRun);
        addPageBreak(document);

        // 6. Detailed Performance Metrics
        addDetailedMetricsTable(document, testRun);
        addPageBreak(document);

        // 7. Per-Transaction Breakdown
        addPerLabelStatisticsTable(document, testRun);
        addPageBreak(document);

        // 8. Acceptance Criteria / SLAs
        addAcceptanceCriteriaSection(document, testRun);
        addPageBreak(document);

        // 9. Results vs Acceptance Criteria
        addResultsVsAcceptanceCriteria(document, testRun);
        addPageBreak(document);

        // 9.5 Baseline Evaluation (per test case label)
        if (includeBaseline) {
            addBaselineEvaluationSection(document, testRun);
            addPageBreak(document);
        }

        // 10. Performance Diagrams
        if (addJtlDiagramsSection(document, testRun)) {
            addPageBreak(document);
        }

        // 11. Conclusion & Recommendations
        addConclusionSection(document, testRun, includeRegression);

        // Save document
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = String.format("JMeter_Report_%s_%s.docx",
                testRun.getTestName().replaceAll("\\s+", "_"), timestamp);
        String filePath = reportDir.resolve(fileName).toString();

        document.enforceUpdateFields();
        try (FileOutputStream out = new FileOutputStream(filePath)) {
            document.write(out);
        }
        document.close();

        Report report = Report.builder()
                .testRun(testRun)
                .reportType(Report.ReportType.TECHNICAL_WORD)
                .fileName(fileName)
                .filePath(filePath)
                .fileSize(Files.size(Paths.get(filePath)))
                .generatedBy(generatedBy)
                .description("JMeter performance report for " + testRun.getTestName())
                .build();

        log.info("Word report generated successfully: {}", filePath);
        return reportRepository.save(report);
    }

    @Transactional
    public Report generatePdfReport(Long testRunId, String generatedBy, boolean includeBaseline, boolean includeRegression) throws Exception {
        log.info("Generating PDF report for test run ID: {}", testRunId);
        Report wordReport = generateWordReport(testRunId, generatedBy, includeBaseline, includeRegression);
        String pdfFilePath = pdfConversionService.generatePdfFilePath(wordReport.getFilePath());
        boolean success = pdfConversionService.convertDocxToPdf(wordReport.getFilePath(), pdfFilePath);
        if (!success) {
            throw new RuntimeException("Failed to convert Word report to PDF");
        }
        String pdfFileName = wordReport.getFileName().replace(".docx", ".pdf");
        Report pdfReport = Report.builder()
                .testRun(wordReport.getTestRun())
                .reportType(Report.ReportType.TECHNICAL_PDF)
                .fileName(pdfFileName)
                .filePath(pdfFilePath)
                .fileSize(Files.size(Paths.get(pdfFilePath)))
                .generatedBy(generatedBy)
                .description("PDF performance report for " + wordReport.getTestRun().getTestName())
                .build();
        return reportRepository.save(pdfReport);
    }

    @Transactional
    public Report generateBothReports(Long testRunId, String generatedBy, boolean includeBaseline, boolean includeRegression) throws Exception {
        Report wordReport = generateWordReport(testRunId, generatedBy, includeBaseline, includeRegression);
        String pdfFilePath = pdfConversionService.generatePdfFilePath(wordReport.getFilePath());
        boolean success = pdfConversionService.convertDocxToPdf(wordReport.getFilePath(), pdfFilePath);
        if (!success) {
            log.warn("PDF conversion failed, returning Word report only");
            return wordReport;
        }
        String pdfFileName = wordReport.getFileName().replace(".docx", ".pdf");
        Report pdfReport = Report.builder()
                .testRun(wordReport.getTestRun())
                .reportType(Report.ReportType.TECHNICAL_PDF)
                .fileName(pdfFileName)
                .filePath(pdfFilePath)
                .fileSize(Files.size(Paths.get(pdfFilePath)))
                .generatedBy(generatedBy)
                .description("PDF performance report for " + wordReport.getTestRun().getTestName())
                .build();
        return reportRepository.save(pdfReport);
    }

    public List<String> getMissingCoverFields(Long testRunId) {
        TestRun testRun = testRunRepository.findById(testRunId)
                .orElseThrow(() -> new RuntimeException("Test run not found: " + testRunId));
        return getMissingCoverFields(testRun);
    }

    private List<String> getMissingCoverFields(TestRun testRun) {
        List<String> missing = new ArrayList<>();
        Capability capability = testRun.getCapability();

        if (capability == null) {
            missing.add("Capability");
            return missing;
        }

        if (isBlank(capability.getName())) missing.add("Capability name");
        if (isBlank(testRun.getTestName())) missing.add("Test name");
        if (testRun.getTestDate() == null) missing.add("Test date");
        if (isBlank(capability.getDescription())) missing.add("Capability description");
        if (isBlank(capability.getTestObjective())) missing.add("Test objective");
        if (isBlank(capability.getTestScope())) missing.add("Test scope");
        if (isBlank(capability.getEnvironmentDetails())) missing.add("Environment details");

        return missing;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    // ========================================================================
    // TEMPLATE LOADING
    // ========================================================================

    /**
     * Load the Hamza corporate template from classpath resources.
     * Falls back to the templates directory on disk if not found in classpath.
     */
    private XWPFDocument loadTemplate() throws IOException {
        // Try classpath first
        try {
            ClassPathResource resource = new ClassPathResource(TEMPLATE_FILE);
            if (resource.exists()) {
                log.info("Loading report template from classpath: {}", TEMPLATE_FILE);
                return new XWPFDocument(resource.getInputStream());
            }
        } catch (Exception e) {
            log.debug("Template not found in classpath, trying disk: {}", e.getMessage());
        }

        // Try disk
        Path diskPath = Paths.get(templatesDirectory, "ReportTemplate2.docx");
        if (Files.exists(diskPath)) {
            log.info("Loading report template from disk: {}", diskPath);
            return new XWPFDocument(new FileInputStream(diskPath.toFile()));
        }

        log.warn("Hamza template not found, creating blank document");
        return new XWPFDocument();
    }

    /**
     * Preserve cover page (section 0), copyright (section 1), and TOC (section 2).
     * Headers, footers, logos, and template structure remain untouched.
     * Remove only main content placeholder (section 3+) to replace with report data.
     */
    private void clearBodyContentPreservingTemplateStructure(XWPFDocument document) {
        List<IBodyElement> elements = document.getBodyElements();
        if (elements.isEmpty()) {
            log.debug("No body elements to clear");
            return;
        }

        int sectPrCount = 0;
        int lastIndexToKeep = -1;
        for (int i = 0; i < elements.size(); i++) {
            IBodyElement el = elements.get(i);
            if (el instanceof XWPFParagraph) {
                CTP ctP = ((XWPFParagraph) el).getCTP();
                if (ctP != null && ctP.isSetPPr() && ctP.getPPr().isSetSectPr()) {
                    sectPrCount++;
                    lastIndexToKeep = i;
                    if (sectPrCount >= 3) {
                        break;
                    }
                }
            }
        }

        int removeFrom;
        if (sectPrCount == 0) {
            removeFrom = 0;
            log.debug("No section breaks; clearing all body content (template may be single-section)");
        } else if (sectPrCount >= 3) {
            removeFrom = lastIndexToKeep + 1;
            log.debug("Preserved cover, copyright, TOC (3 sections); clearing main content from index {}", removeFrom);
        } else {
            removeFrom = elements.size();
            log.debug("Preserved first {} sections; no removal", sectPrCount);
        }
        for (int i = elements.size() - 1; i >= removeFrom; i--) {
            document.removeBodyElement(i);
        }
    }

    // ========================================================================
    // COVER PAGE UPDATE
    // ========================================================================

    /**
     * Update cover page (section 0) with capability details
     * Modifies the document to include capability name at the start
     */
    private void updateCoverPageWithCapability(XWPFDocument document, Capability capability, TestRun testRun) {
        try {
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            
            if (paragraphs.isEmpty()) {
                log.info("No paragraphs found; skipping cover page update");
                return;
            }

            // Update first paragraph with capability title
            XWPFParagraph firstPara = paragraphs.get(0);
            if (firstPara.getRuns().size() == 0) {
                XWPFRun run = firstPara.createRun();
                run.setText(capability.getName() + " Performance Test Report");
                run.setBold(true);
                run.setFontSize(28);
                run.setFontFamily(FONT_FAMILY);
                run.setColor("1F4E79");
            } else {
                // Replace text in first run
                XWPFRun run = firstPara.getRuns().get(0);
                run.setText(capability.getName() + " Performance Test Report");
                run.setBold(true);
                run.setFontSize(28);
                run.setFontFamily(FONT_FAMILY);
                run.setColor("1F4E79");
            }
            
            firstPara.setAlignment(ParagraphAlignment.CENTER);
            applyStyle(firstPara, STYLE_NORMAL);

            // Add test name as second paragraph
            XWPFParagraph testNamePara = document.createParagraph();
            testNamePara.setAlignment(ParagraphAlignment.CENTER);
            applyStyle(testNamePara, STYLE_NORMAL);
            
            XWPFRun testNameRun = testNamePara.createRun();
            testNameRun.setText(testRun.getTestName());
            testNameRun.setFontSize(14);
            testNameRun.setFontFamily(FONT_FAMILY);
            testNameRun.setColor("333333");

            // Add capability description if available
            if (capability.getDescription() != null && !capability.getDescription().isBlank()) {
                XWPFParagraph descPara = document.createParagraph();
                descPara.setAlignment(ParagraphAlignment.CENTER);
                applyStyle(descPara, STYLE_NORMAL);
                
                XWPFRun descRun = descPara.createRun();
                descRun.setText(capability.getDescription());
                descRun.setFontSize(11);
                descRun.setFontFamily(FONT_FAMILY);
                descRun.setColor("666666");
                descRun.setItalic(true);
            }

            // Add test date
            XWPFParagraph datePara = document.createParagraph();
            datePara.setAlignment(ParagraphAlignment.CENTER);
            applyStyle(datePara, STYLE_NORMAL);
            
            XWPFRun dateRun = datePara.createRun();
            String formattedDate = testRun.getTestDate() != null 
                ? testRun.getTestDate().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"))
                : "N/A";
            dateRun.setText("Test Date: " + formattedDate);
            dateRun.setFontSize(11);
            dateRun.setFontFamily(FONT_FAMILY);
            dateRun.setColor("555555");

            log.info("Cover page updated with capability: {}", capability.getName());
        } catch (Exception e) {
            log.warn("Error updating cover page: {}", e.getMessage());
            // Don't fail the report generation; just log the warning
        }
    }

    // ========================================================================
    // COVER PAGE TITLE
    // ========================================================================

    private void addCoverPageTitle(XWPFDocument document, Capability capability, TestRun testRun) {
        try {
            XmlCursor coverCursor = getCoverInsertCursor(document);
            String capabilityName = capability.getName() != null ? capability.getName() : "";
            String reportTitle = "Performance Test Report " + capabilityName;

                LocalDateTime coverDate = testRun.getTestDate();
                if (coverDate == null && testRun.getCreatedAt() != null) {
                    coverDate = testRun.getCreatedAt();
                }
                String formattedDate = coverDate != null
                    ? coverDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                    : "N/A";
            String buildNumber = testRun.getBuildNumber() != null && !testRun.getBuildNumber().isBlank()
                    ? testRun.getBuildNumber()
                    : "N/A";

            XWPFParagraph titlePara = document.insertNewParagraph(coverCursor);
            titlePara.setAlignment(ParagraphAlignment.LEFT);
            XWPFRun titleRun = titlePara.createRun();
            titleRun.setText(reportTitle.trim());
            titleRun.setBold(true);
            titleRun.setFontSize(30);
            titleRun.setFontFamily(FONT_FAMILY);
            titleRun.setColor("1F4E78");

            titleRun.addBreak();
            XWPFRun dateRun = titlePara.createRun();
            dateRun.setText(formattedDate);
            dateRun.setFontSize(12);
            dateRun.setFontFamily(FONT_FAMILY);
            dateRun.setColor("333333");

            dateRun.addBreak();
            XWPFRun buildRun = titlePara.createRun();
            buildRun.setText("Build: " + buildNumber);
            buildRun.setFontSize(12);
            buildRun.setFontFamily(FONT_FAMILY);
            buildRun.setColor("333333");

            log.info("Cover page title added: {}", capability.getName());
        } catch (Exception e) {
            log.warn("Error adding cover page title: {}", e.getMessage());
        }
    }

    private XmlCursor getCoverInsertCursor(XWPFDocument document) {
        List<IBodyElement> elements = document.getBodyElements();
        for (IBodyElement element : elements) {
            if (element instanceof XWPFParagraph paragraph) {
                CTP ctP = paragraph.getCTP();
                if (ctP != null && ctP.isSetPPr() && ctP.getPPr().isSetSectPr()) {
                    return ctP.newCursor();
                }
            }
        }
        if (!document.getParagraphs().isEmpty()) {
            return document.getParagraphs().get(0).getCTP().newCursor();
        }
        return document.getDocument().getBody().newCursor();
    }

    private void insertSpacerParagraph(XWPFDocument document, XmlCursor cursor, ParagraphAlignment alignment) {
        XWPFParagraph spacer = document.insertNewParagraph(cursor);
        spacer.setAlignment(alignment);
        spacer.createRun().setText("");
    }


    // ========================================================================
    // 1. EXECUTIVE SUMMARY (dynamic SLA verdict)
    // ========================================================================

    private void addExecutiveSummary(XWPFDocument document, TestRun testRun) {
        addHeading1(document, "Executive Summary");

        addBodyParagraph(document, String.format(
            "This report presents the performance test results for \"%s\" " +
            "under the %s capability. The test was executed on %s " +
            "and processed a total of %s requests over %s.",
            testRun.getTestName(),
            testRun.getCapability().getName(),
            testRun.getTestDate().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' HH:mm")),
            formatNumber(testRun.getTotalRequests()),
            formatDuration(testRun.getTestDurationSeconds())
        ));
        addEmptyLine(document);

        // KPI Summary table (Hamza template style: header + data rows, TableGrid)
        XWPFTable kpiTable = document.createTable(4, 4);
        setFixedTableLayout(kpiTable, 4);
        applyTableStyle(kpiTable, "TableGrid");
        preventRowSplitting(kpiTable);
        // Row 0: headers
        styleHeaderCell(kpiTable.getRow(0).getCell(0), "Total Requests");
        styleHeaderCell(kpiTable.getRow(0).getCell(1), "Avg Response Time");
        styleHeaderCell(kpiTable.getRow(0).getCell(2), "Throughput");
        styleHeaderCell(kpiTable.getRow(0).getCell(3), "Error Rate");
        // Row 1: values
        XWPFTableRow row1 = kpiTable.getRow(1);
        setCellText(row1.getCell(0), formatNumber(testRun.getTotalRequests()), 11, false, COLOR_ACCENT);
        setCellText(row1.getCell(1), formatMs(testRun.getAvgResponseTime()), 11, false, COLOR_ACCENT);
        setCellText(row1.getCell(2), testRun.getThroughput() != null ? String.format("%.2f req/s", testRun.getThroughput()) : "N/A", 11, false, COLOR_ACCENT);
        setCellText(row1.getCell(3), testRun.getErrorRate() != null ? String.format("%.2f%%", testRun.getErrorRate()) : "N/A", 11, false, COLOR_ACCENT);
        for (XWPFTableCell c : row1.getTableCells()) { setCellShading(c, COLOR_ROW_EVEN); }
        // Row 2: headers (P90, P95, P99, Duration)
        styleHeaderCell(kpiTable.getRow(2).getCell(0), "P90 Response Time");
        styleHeaderCell(kpiTable.getRow(2).getCell(1), "P95 Response Time");
        styleHeaderCell(kpiTable.getRow(2).getCell(2), "P99 Response Time");
        styleHeaderCell(kpiTable.getRow(2).getCell(3), "Duration");
        // Row 3: values
        XWPFTableRow row3 = kpiTable.getRow(3);
        setCellText(row3.getCell(0), formatMs(testRun.getPercentile90()), 11, false, COLOR_ACCENT);
        setCellText(row3.getCell(1), formatMs(testRun.getPercentile95()), 11, false, COLOR_ACCENT);
        setCellText(row3.getCell(2), formatMs(testRun.getPercentile99()), 11, false, COLOR_ACCENT);
        setCellText(row3.getCell(3), formatDuration(testRun.getTestDurationSeconds()), 11, false, COLOR_ACCENT);
        for (XWPFTableCell c : row3.getTableCells()) { setCellShading(c, COLOR_ROW_ODD); }

        addEmptyLine(document);

        // Dynamic Pass/Fail verdict
        Capability capability = testRun.getCapability();
        boolean hasCriteria = capability.getAcceptanceCriteria() != null
                && !capability.getAcceptanceCriteria().isEmpty()
                && capability.getAcceptanceCriteria().containsKey("criteria");

        boolean passed;
        String verdictReason;
        if (hasCriteria) {
            SlaEvaluationResult slaResult = evaluateAllCriteria(testRun);
            passed = slaResult.allPassed;
            verdictReason = passed
                ? String.format("  (All %d SLA criteria met)", slaResult.totalCriteria)
                : String.format("  (%d of %d SLA criteria failed)", slaResult.failedCriteria, slaResult.totalCriteria);
        } else {
            passed = testRun.getErrorRate() != null && testRun.getErrorRate() < 1.0;
            double errorRate = testRun.getErrorRate() != null ? testRun.getErrorRate() : 0.0;
            verdictReason = passed
                ? String.format("  (Error rate %.2f%% is below 1%% threshold)", errorRate)
                : String.format("  (Error rate %.2f%% exceeds 1%% threshold)", errorRate);
        }

        XWPFParagraph verdictPara = document.createParagraph();
        applyStyle(verdictPara, STYLE_BODY_TEXT);
        XWPFRun lbl = verdictPara.createRun();
        lbl.setText("Overall Result: ");
        lbl.setBold(true); lbl.setFontSize(12); lbl.setFontFamily(FONT_FAMILY);

        XWPFRun val = verdictPara.createRun();
        val.setText(passed ? "PASS" : "FAIL");
        val.setBold(true); val.setFontSize(14); val.setFontFamily(FONT_FAMILY);
        val.setColor(passed ? COLOR_SUCCESS : COLOR_DANGER);

        XWPFRun reason = verdictPara.createRun();
        reason.setText(verdictReason);
        reason.setFontSize(10); reason.setFontFamily(FONT_FAMILY); reason.setColor("666666");

        addEmptyLine(document);
    }

    // ========================================================================
    // 2. TEST OBJECTIVE & SCOPE
    // ========================================================================

    private void addTestObjectiveAndScope(XWPFDocument document, TestRun testRun) {
        addHeading1(document, "Test Objective & Scope");

        Capability capability = testRun.getCapability();
        boolean hasObjective = capability.getTestObjective() != null && !capability.getTestObjective().isBlank();
        boolean hasScope = capability.getTestScope() != null && !capability.getTestScope().isBlank();

        if (!hasObjective && !hasScope) {
            addBodyParagraph(document, "No test objective or scope has been defined for this capability. "
                + "Configure test metadata via the Capability Management API to enrich future reports.");
            return;
        }

        if (hasObjective) {
            addHeading2(document, "Test Objective");
            addBodyParagraph(document, capability.getTestObjective());
            if (hasScope) {
                addEmptyLine(document);
            }
        }

        if (hasScope) {
            addHeading2(document, "Test Scope");
            String[] scopeLines = capability.getTestScope().split("\\n");
            if (scopeLines.length > 1) {
                for (String line : scopeLines) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty()) {
                        trimmed = trimmed.replaceAll("^[-•*]\\s*", "");
                        addBulletItem(document, trimmed);
                    }
                }
            } else {
                addBodyParagraph(document, capability.getTestScope());
            }
        }
        addEmptyLine(document);
    }

    // ========================================================================
    // 3. TEST ENVIRONMENT & ARCHITECTURE
    // ========================================================================

    private void addTestEnvironmentAndArchitecture(XWPFDocument document, TestRun testRun) {
        addHeading1(document, "Test Environment & Architecture");

        Capability capability = testRun.getCapability();
        boolean hasEnvDetails = capability.getEnvironmentDetails() != null && !capability.getEnvironmentDetails().isBlank();
        boolean hasArchDiagram = capability.getArchitectureDiagramPath() != null
                && !capability.getArchitectureDiagramPath().isBlank();

        if (!hasEnvDetails && !hasArchDiagram) {
            addBodyParagraph(document, "No environment details or architecture diagram have been defined for this capability.");
            return;
        }

        if (hasEnvDetails) {
            addHeading2(document, "Environment Details");
            String[] envLines = capability.getEnvironmentDetails().split("\\n");
            if (envLines.length > 1) {
                for (String line : envLines) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty()) {
                        trimmed = trimmed.replaceAll("^[-•*]\\s*", "");
                        addBulletItem(document, trimmed);
                    }
                }
            } else {
                addBodyParagraph(document, capability.getEnvironmentDetails());
            }
        }

        if (hasArchDiagram) {
            addHeading2(document, "Architecture Diagram");
            Path diagramPath = Paths.get(capability.getArchitectureDiagramPath());
            if (Files.exists(diagramPath)) {
                try (FileInputStream is = new FileInputStream(diagramPath.toFile())) {
                    XWPFParagraph p = document.createParagraph();
                    p.setAlignment(ParagraphAlignment.CENTER);
                    XWPFRun run = p.createRun();
                    String fileName = diagramPath.getFileName().toString();
                    String ext = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf('.') + 1) : "png";
                    run.addPicture(is, getPictureType(ext), fileName,
                            Units.toEMU(460), Units.toEMU(300));
                } catch (Exception e) {
                    log.error("Failed to insert capability architecture diagram: {}", e.getMessage());
                    addBodyParagraph(document, "[Architecture diagram could not be loaded]");
                }
            } else {
                log.warn("Architecture diagram file not found: {}", diagramPath);
                addBodyParagraph(document, "[Architecture diagram file not found at configured path]");
            }
        }
        addEmptyLine(document);
    }

    // ========================================================================
    // 4. TEST CASES
    // ========================================================================

    private void addTestCasesSection(XWPFDocument document, TestRun testRun) {
        addHeading1(document, "Test Cases");

        Capability capability = testRun.getCapability();
        List<CapabilityTestCase> testCases = capabilityTestCaseRepository
                .findByCapabilityIdOrderByPriorityAsc(capability.getId());

        if (testCases.isEmpty()) {
            addBodyParagraph(document, "No test cases have been defined for this capability. "
                + "Add test cases via the Capability Management API to populate this section.");
            return;
        }

        addBodyParagraph(document, String.format(
            "The following %d test cases define the performance scenarios tested for the %s capability.",
            testCases.size(), capability.getName()));
        addEmptyLine(document);

        String[] headers = {"#", "Test Case", "Description", "Expected Behavior", "Priority"};
        XWPFTable table = document.createTable(testCases.size() + 1, headers.length);
        setFixedTableLayout(table, headers.length);
        preventRowSplitting(table);

        XWPFTableRow headerRow = table.getRow(0);
        for (int c = 0; c < headers.length; c++) {
            styleHeaderCell(headerRow.getCell(c), headers[c]);
        }

        for (int i = 0; i < testCases.size(); i++) {
            CapabilityTestCase tc = testCases.get(i);
            XWPFTableRow row = table.getRow(i + 1);
            String bg = (i % 2 == 0) ? COLOR_ROW_EVEN : COLOR_ROW_ODD;

            setCellText(row.getCell(0), String.valueOf(i + 1), 9, false, "333333");
            setCellText(row.getCell(1), tc.getTestCaseName(), 9, true, COLOR_ACCENT);
            setCellText(row.getCell(2), tc.getDescription() != null ? tc.getDescription() : "—", 9, false, "333333");
            setCellText(row.getCell(3), tc.getExpectedBehavior() != null ? tc.getExpectedBehavior() : "—", 9, false, "333333");
            setCellText(row.getCell(4), tc.getPriority().name(), 9, true, getPriorityColor(tc.getPriority()));

            for (int c = 0; c < headers.length; c++) {
                setCellShading(row.getCell(c), bg);
            }
        }
        addEmptyLine(document);
    }

    // ========================================================================
    // 5. TEST CONFIGURATION
    // ========================================================================

    private void addTestConfiguration(XWPFDocument document, TestRun testRun) {
        addHeading1(document, "Test Configuration");

        String[][] data = {
            {"Test Name", testRun.getTestName()},
            {"Capability", testRun.getCapability().getName()},
            {"Build Number", testRun.getBuildNumber() != null ? testRun.getBuildNumber() : "N/A"},
            {"Test Tool", "Apache JMeter"},
            {"Test Date", testRun.getTestDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))},
            {"Test Duration", formatDuration(testRun.getTestDurationSeconds())},
            {"Virtual Users (Threads)", testRun.getVirtualUsers() != null ? testRun.getVirtualUsers().toString() : "Auto-detected from JTL"},
            {"File Type", testRun.getFileType() != null ? testRun.getFileType().toString() : "N/A"},
            {"Description", testRun.getDescription() != null ? testRun.getDescription() : "N/A"},
        };

        XWPFTable table = document.createTable(data.length, 2);
        setFixedTableLayout(table, 2);
        preventRowSplitting(table);
        for (int i = 0; i < data.length; i++) {
            XWPFTableRow row = table.getRow(i);
            setCellText(row.getCell(0), data[i][0], 10, true, COLOR_ACCENT);
            setCellText(row.getCell(1), data[i][1], 10, false, "333333");
            String bg = (i % 2 == 0) ? COLOR_ROW_EVEN : COLOR_ROW_ODD;
            setCellShading(row.getCell(0), bg);
            setCellShading(row.getCell(1), bg);
        }
        addEmptyLine(document);
    }

    // ========================================================================
    // 6. DETAILED METRICS TABLE
    // ========================================================================

    private void addDetailedMetricsTable(XWPFDocument document, TestRun testRun) {
        addHeading1(document, "Detailed Performance Metrics");
        addBodyParagraph(document, "Comprehensive view of all measured performance indicators from the test execution.");
        addEmptyLine(document);

        String[][] data = {
            {"Total Requests", formatNumber(testRun.getTotalRequests())},
            {"Successful Requests", formatNumber(testRun.getSuccessfulRequests())},
            {"Failed Requests", formatNumber(testRun.getFailedRequests())},
            {"Error Rate", testRun.getErrorRate() != null ? String.format("%.4f%%", testRun.getErrorRate()) : "N/A"},
            {"", ""},
            {"Average Response Time", formatMs(testRun.getAvgResponseTime())},
            {"Minimum Response Time", formatMs(testRun.getMinResponseTime())},
            {"Maximum Response Time", formatMs(testRun.getMaxResponseTime())},
            {"P90 Response Time", formatMs(testRun.getPercentile90())},
            {"P95 Response Time", formatMs(testRun.getPercentile95())},
            {"P99 Response Time", formatMs(testRun.getPercentile99())},
            {"", ""},
            {"Throughput", testRun.getThroughput() != null ? String.format("%.2f requests/sec", testRun.getThroughput()) : "N/A"},
            {"Test Duration", formatDuration(testRun.getTestDurationSeconds())},
            {"Concurrent Threads", testRun.getVirtualUsers() != null ? testRun.getVirtualUsers().toString() : "N/A"},
        };

        XWPFTable table = document.createTable(data.length + 1, 2);
        setFixedTableLayout(table, 2);
        preventRowSplitting(table);
        styleHeaderCell(table.getRow(0).getCell(0), "Metric");
        styleHeaderCell(table.getRow(0).getCell(1), "Value");

        for (int i = 0; i < data.length; i++) {
            XWPFTableRow row = table.getRow(i + 1);
            if (data[i][0].isEmpty()) {
                setCellText(row.getCell(0), "", 9, false, "999999");
                setCellText(row.getCell(1), "", 9, false, "999999");
                setCellShading(row.getCell(0), "F0F0F0");
                setCellShading(row.getCell(1), "F0F0F0");
            } else {
                setCellText(row.getCell(0), data[i][0], 10, true, COLOR_ACCENT);
                setCellText(row.getCell(1), data[i][1], 10, false, "333333");
                String bg = (i % 2 == 0) ? COLOR_ROW_EVEN : COLOR_ROW_ODD;
                setCellShading(row.getCell(0), bg);
                setCellShading(row.getCell(1), bg);
            }
        }
        addEmptyLine(document);
    }

    // ========================================================================
    // 7. PER-LABEL STATISTICS TABLE
    // ========================================================================

    @SuppressWarnings("unchecked")
    private void addPerLabelStatisticsTable(XWPFDocument document, TestRun testRun) {
        addHeading1(document, "Per-Transaction Breakdown");

        Map<String, Object> capData = testRun.getCapabilitySpecificData();
        if (capData == null || !capData.containsKey("labelStatistics")) {
            addBodyParagraph(document, "Per-transaction statistics are not available for this test run.");
            return;
        }

        addBodyParagraph(document, "Performance metrics broken down by individual transaction/request type (label) from the JMeter test plan.");
        addBodyParagraph(document, "Only labels that match configured test cases are included.");
        addEmptyLine(document);

        Object labelStatsObj = capData.get("labelStatistics");
        if (!(labelStatsObj instanceof Map)) {
            addBodyParagraph(document, "Label statistics data format is not recognized.");
            return;
        }
        Map<String, Object> labelStatsMap = (Map<String, Object>) labelStatsObj;
        Map<String, Object> filteredLabelStats = filterLabelsToTestCases(labelStatsMap, testRun.getCapability());
        if (filteredLabelStats.isEmpty()) {
            addBodyParagraph(document, "No label statistics match the configured test cases.");
            return;
        }

        String[] headers = {"Label", "Samples", "Failures", "Error %", "Avg (ms)", "Min (ms)",
                "Max (ms)", "P90 (ms)", "P95 (ms)", "P99 (ms)", "Throughput"};

        XWPFTable table = document.createTable(filteredLabelStats.size() + 1, headers.length);
        setFixedTableLayout(table, headers.length);
        preventRowSplitting(table);

        XWPFTableRow headerRow = table.getRow(0);
        for (int c = 0; c < headers.length; c++) {
            styleHeaderCell(headerRow.getCell(c), headers[c]);
        }

        int rowIdx = 1;
        for (Map.Entry<String, Object> entry : filteredLabelStats.entrySet()) {
            String label = entry.getKey();
            if (!(entry.getValue() instanceof Map)) continue;
            Map<String, Object> stats = (Map<String, Object>) entry.getValue();
            XWPFTableRow row = table.getRow(rowIdx);
            boolean isTotal = "Total".equals(label);
            String color = isTotal ? COLOR_ACCENT : "333333";

            setCellText(row.getCell(0), label, 8, isTotal, color);
            setCellText(row.getCell(1), fmtStat(stats, "samples"), 8, isTotal, color);
            setCellText(row.getCell(2), fmtStat(stats, "failures"), 8, isTotal, color);
            setCellText(row.getCell(3), fmtStatPct(stats, "errorPercent"), 8, isTotal, color);
            setCellText(row.getCell(4), fmtStatDec(stats, "average"), 8, isTotal, color);
            setCellText(row.getCell(5), fmtStat(stats, "min"), 8, isTotal, color);
            setCellText(row.getCell(6), fmtStat(stats, "max"), 8, isTotal, color);
            setCellText(row.getCell(7), fmtStatDec(stats, "percentile90"), 8, isTotal, color);
            setCellText(row.getCell(8), fmtStatDec(stats, "percentile95"), 8, isTotal, color);
            setCellText(row.getCell(9), fmtStatDec(stats, "percentile99"), 8, isTotal, color);
            setCellText(row.getCell(10), fmtStatDec(stats, "throughput"), 8, isTotal, color);

            String bgColor = isTotal ? "E8F0FE" : ((rowIdx % 2 == 0) ? COLOR_ROW_EVEN : COLOR_ROW_ODD);
            for (int c = 0; c < headers.length; c++) { setCellShading(row.getCell(c), bgColor); }
            rowIdx++;
        }
        addEmptyLine(document);
    }

    // ========================================================================
    // 8. ACCEPTANCE CRITERIA / SLAs
    // ========================================================================

    @SuppressWarnings("unchecked")
    private void addAcceptanceCriteriaSection(XWPFDocument document, TestRun testRun) {
        addHeading1(document, "Acceptance Criteria (SLAs)");

        Capability capability = testRun.getCapability();
        Map<String, Object> ac = capability.getAcceptanceCriteria();

        if (ac == null || ac.isEmpty() || !ac.containsKey("criteria")) {
            addBodyParagraph(document, "No acceptance criteria (SLAs) have been defined for this capability. "
                + "The default threshold of error rate < 1% will be used for pass/fail determination.");
            return;
        }

        addBodyParagraph(document, String.format(
            "The following Service Level Agreements (SLAs) define the acceptance criteria for the %s capability. "
            + "Test results are evaluated against each criterion to determine overall compliance.",
            capability.getName()));
        addEmptyLine(document);

        List<Map<String, Object>> criteria = (List<Map<String, Object>>) ac.get("criteria");

        String[] headers = {"#", "Metric", "Operator", "Threshold", "Unit"};
        XWPFTable table = document.createTable(criteria.size() + 1, headers.length);
        setFixedTableLayout(table, headers.length);
        preventRowSplitting(table);
        preventRowSplitting(table);

        XWPFTableRow headerRow = table.getRow(0);
        for (int c = 0; c < headers.length; c++) {
            styleHeaderCell(headerRow.getCell(c), headers[c]);
        }

        for (int i = 0; i < criteria.size(); i++) {
            Map<String, Object> criterion = criteria.get(i);
            XWPFTableRow row = table.getRow(i + 1);
            String bg = (i % 2 == 0) ? COLOR_ROW_EVEN : COLOR_ROW_ODD;

            String metric = (String) criterion.getOrDefault("metric", "—");
            String operator = (String) criterion.getOrDefault("operator", "—");
            Object thresholdObj = criterion.get("threshold");
            String threshold = thresholdObj != null ? thresholdObj.toString() : "—";
            String unit = (String) criterion.getOrDefault("unit", "");

            setCellText(row.getCell(0), String.valueOf(i + 1), 9, false, "333333");
            setCellText(row.getCell(1), getMetricDisplayName(metric), 9, true, COLOR_ACCENT);
            setCellText(row.getCell(2), operator, 9, false, "333333");
            setCellText(row.getCell(3), threshold, 9, false, "333333");
            setCellText(row.getCell(4), unit, 9, false, "333333");

            for (int c = 0; c < headers.length; c++) { setCellShading(row.getCell(c), bg); }
        }
        addEmptyLine(document);
    }

    // ========================================================================
    // 9. RESULTS VS ACCEPTANCE CRITERIA
    // ========================================================================

    @SuppressWarnings("unchecked")
    private void addResultsVsAcceptanceCriteria(XWPFDocument document, TestRun testRun) {
        addHeading1(document, "Results vs Acceptance Criteria");

        Capability capability = testRun.getCapability();
        Map<String, Object> ac = capability.getAcceptanceCriteria();

        if (ac == null || ac.isEmpty() || !ac.containsKey("criteria")) {
            addBodyParagraph(document, "No acceptance criteria defined. Using default error rate threshold of 1%.");
            addEmptyLine(document);

            double errorRate = testRun.getErrorRate() != null ? testRun.getErrorRate() : 0.0;
            boolean passed = errorRate < 1.0;

            String[] headers = {"Metric", "SLA Threshold", "Actual Value", "Result"};
            XWPFTable table = document.createTable(2, headers.length);
            setFixedTableLayout(table, headers.length);
            preventRowSplitting(table);
            XWPFTableRow headerRow = table.getRow(0);
            for (int c = 0; c < headers.length; c++) { styleHeaderCell(headerRow.getCell(c), headers[c]); }

            XWPFTableRow row = table.getRow(1);
            setCellText(row.getCell(0), "Error Rate", 10, true, COLOR_ACCENT);
            setCellText(row.getCell(1), "< 1.00%", 10, false, "333333");
            setCellText(row.getCell(2), String.format("%.2f%%", errorRate), 10, false, "333333");
            setCellText(row.getCell(3), passed ? "PASS" : "FAIL", 10, true, passed ? COLOR_SUCCESS : COLOR_DANGER);
            for (int c = 0; c < headers.length; c++) { setCellShading(row.getCell(c), COLOR_ROW_EVEN); }
            addEmptyLine(document);
            return;
        }

        addBodyParagraph(document, "Comparison of actual test results against the defined acceptance criteria for this capability.");
        addEmptyLine(document);

        List<Map<String, Object>> criteria = (List<Map<String, Object>>) ac.get("criteria");

        String[] headers = {"Metric", "SLA Threshold", "Actual Value", "Result"};
        XWPFTable table = document.createTable(criteria.size() + 1, headers.length);
        setFixedTableLayout(table, headers.length);

        XWPFTableRow headerRow = table.getRow(0);
        for (int c = 0; c < headers.length; c++) { styleHeaderCell(headerRow.getCell(c), headers[c]); }

        int passCount = 0;
        int failCount = 0;

        for (int i = 0; i < criteria.size(); i++) {
            Map<String, Object> criterion = criteria.get(i);
            XWPFTableRow row = table.getRow(i + 1);
            String bg = (i % 2 == 0) ? COLOR_ROW_EVEN : COLOR_ROW_ODD;

            String metric = (String) criterion.getOrDefault("metric", "unknown");
            String operator = (String) criterion.getOrDefault("operator", "<=");
            double threshold = toDouble(criterion.get("threshold"));
            String unit = (String) criterion.getOrDefault("unit", "");

            Double actualValue = getMetricValue(testRun, metric);
            boolean passed = actualValue != null && evaluateCriterion(actualValue, operator, threshold);

            if (passed) { passCount++; } else { failCount++; }

            setCellText(row.getCell(0), getMetricDisplayName(metric), 10, true, COLOR_ACCENT);
            setCellText(row.getCell(1), operator + " " + formatMetricValue(threshold, unit), 10, false, "333333");
            setCellText(row.getCell(2), actualValue != null ? formatMetricValue(actualValue, unit) : "N/A", 10, false, "333333");
            setCellText(row.getCell(3), passed ? "PASS" : "FAIL", 10, true, passed ? COLOR_SUCCESS : COLOR_DANGER);

            for (int c = 0; c < headers.length; c++) { setCellShading(row.getCell(c), bg); }
        }

        addEmptyLine(document);

        // SLA compliance summary
        boolean allPassed = failCount == 0;
        XWPFParagraph summaryPara = document.createParagraph();
        applyStyle(summaryPara, STYLE_BODY_TEXT);
        XWPFRun summaryLabel = summaryPara.createRun();
        summaryLabel.setText("SLA Compliance: ");
        summaryLabel.setBold(true); summaryLabel.setFontSize(12); summaryLabel.setFontFamily(FONT_FAMILY);

        XWPFRun summaryValue = summaryPara.createRun();
        summaryValue.setText(allPassed ? "FULLY COMPLIANT" : "NON-COMPLIANT");
        summaryValue.setBold(true); summaryValue.setFontSize(14); summaryValue.setFontFamily(FONT_FAMILY);
        summaryValue.setColor(allPassed ? COLOR_SUCCESS : COLOR_DANGER);

        XWPFRun summaryDetail = summaryPara.createRun();
        summaryDetail.setText(String.format("  (%d passed, %d failed out of %d criteria)",
                passCount, failCount, criteria.size()));
        summaryDetail.setFontSize(10); summaryDetail.setFontFamily(FONT_FAMILY); summaryDetail.setColor("666666");

        addEmptyLine(document);
    }

    // ========================================================================
    // 9.5 BASELINE EVALUATION
    // ========================================================================

    @SuppressWarnings("unchecked")
    private void addBaselineEvaluationSection(XWPFDocument document, TestRun testRun) {
        addHeading1(document, "Baseline Evaluation (Per Test Case)");

        Map<String, Object> capData = testRun.getCapabilitySpecificData();
        if (capData == null || !capData.containsKey("baselineEvaluation")) {
            addBodyParagraph(document, "No baseline evaluation is available for this test run.");
            return;
        }

        Object baselineEvalObj = capData.get("baselineEvaluation");
        if (!(baselineEvalObj instanceof Map)) {
            addBodyParagraph(document, "Baseline evaluation data format is not recognized.");
            return;
        }

        Map<String, Object> baselineEval = (Map<String, Object>) baselineEvalObj;
        Map<String, Object> baseline = baselineEval.get("baseline") instanceof Map
                ? (Map<String, Object>) baselineEval.get("baseline")
                : new HashMap<>();

        addBodyParagraph(document, "Each label is evaluated against the capability baseline metrics.");
        addBodyParagraph(document, "Only labels that match configured test cases are included.");
        addEmptyLine(document);

        String[][] baselineRows = {
            {"P95 max (ms)", formatBaselineValue(baseline.get("p95MaxMs"))},
            {"Avg max (ms)", formatBaselineValue(baseline.get("avgMaxMs"))},
            {"P90 max (ms)", formatBaselineValue(baseline.get("p90MaxMs"))},
            {"Throughput min (req/s)", formatBaselineValue(baseline.get("throughputMin"))}
        };

        XWPFTable baselineTable = document.createTable(baselineRows.length + 1, 2);
        setFixedTableLayout(baselineTable, 2);
        preventRowSplitting(baselineTable);
        styleHeaderCell(baselineTable.getRow(0).getCell(0), "Metric");
        styleHeaderCell(baselineTable.getRow(0).getCell(1), "Threshold");

        for (int i = 0; i < baselineRows.length; i++) {
            XWPFTableRow row = baselineTable.getRow(i + 1);
            setCellText(row.getCell(0), baselineRows[i][0], 10, true, COLOR_ACCENT);
            setCellText(row.getCell(1), baselineRows[i][1], 10, false, "333333");
            String bg = (i % 2 == 0) ? COLOR_ROW_EVEN : COLOR_ROW_ODD;
            setCellShading(row.getCell(0), bg);
            setCellShading(row.getCell(1), bg);
        }

        addEmptyLine(document);

        Map<String, Object> results = baselineEval.get("results") instanceof Map
            ? (Map<String, Object>) baselineEval.get("results")
            : new HashMap<>();
        Map<String, Object> filteredResults = filterLabelsToTestCases(results, testRun.getCapability());

        if (filteredResults.isEmpty()) {
            addBodyParagraph(document, "No baseline results match the configured test cases.");
            return;
        }

        String[] headers = {"Label", "P90 (ms)", "P95 (ms)", "Avg (ms)", "Throughput", "Result"};
        XWPFTable table = document.createTable(filteredResults.size() + 1, headers.length);
        setFixedTableLayout(table, headers.length);
        preventRowSplitting(table);

        XWPFTableRow headerRow = table.getRow(0);
        for (int c = 0; c < headers.length; c++) {
            styleHeaderCell(headerRow.getCell(c), headers[c]);
        }

        int rowIdx = 1;
        for (Map.Entry<String, Object> entry : filteredResults.entrySet()) {
            if (!(entry.getValue() instanceof Map)) {
                continue;
            }
            Map<String, Object> eval = (Map<String, Object>) entry.getValue();
            Map<String, Object> metrics = eval.get("metrics") instanceof Map
                    ? (Map<String, Object>) eval.get("metrics")
                    : new HashMap<>();
            boolean passed = Boolean.TRUE.equals(eval.get("pass"));

            XWPFTableRow row = table.getRow(rowIdx);
            String label = entry.getKey();
            setCellText(row.getCell(0), label, 8, "Total".equals(label), COLOR_ACCENT);
            setCellText(row.getCell(1), formatMs(toDouble(metrics.get("p90"))), 8, false, "333333");
            setCellText(row.getCell(2), formatMs(toDouble(metrics.get("p95"))), 8, false, "333333");
            setCellText(row.getCell(3), formatMs(toDouble(metrics.get("avg"))), 8, false, "333333");
            setCellText(row.getCell(4), formatThroughput(metrics.get("throughput")), 8, false, "333333");
            setCellText(row.getCell(5), passed ? "PASS" : "FAIL", 8, true, passed ? COLOR_SUCCESS : COLOR_DANGER);

            String bg = (rowIdx % 2 == 0) ? COLOR_ROW_EVEN : COLOR_ROW_ODD;
            for (int c = 0; c < headers.length; c++) {
                setCellShading(row.getCell(c), bg);
            }
            rowIdx++;
        }

        addEmptyLine(document);
    }

    // ========================================================================
    // 10. PERFORMANCE DIAGRAMS
    // ========================================================================

    private boolean addJtlDiagramsSection(XWPFDocument document, TestRun testRun) {
        try {
            String capabilityName = testRun.getCapability().getName().replaceAll("[^a-zA-Z0-9]", "_");
            Path capabilityDir = Paths.get(diagramsDirectory, capabilityName);
            if (!Files.exists(capabilityDir)) {
                log.warn("Diagram directory not found: {}", capabilityDir);
                return false;
            }

            List<Path> testRunDirs = Files.list(capabilityDir)
                .filter(Files::isDirectory)
                .filter(p -> p.getFileName().toString().startsWith("testrun_" + testRun.getId() + "_"))
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());

            if (testRunDirs.isEmpty()) {
                log.warn("No diagram directory for test run ID: {}", testRun.getId());
                return false;
            }
            Path diagramDir = testRunDirs.get(0);

            addHeading1(document, "Performance Diagrams");
            addBodyParagraph(document, "The following charts visualize the performance characteristics observed during the test execution.");
            addEmptyLine(document);

            List<Path> diagrams = Files.list(diagramDir)
                .filter(p -> p.toString().endsWith(".png"))
                .sorted()
                .collect(Collectors.toList());

            if (diagrams.isEmpty()) {
                addBodyParagraph(document, "No performance diagrams were generated for this test run.");
                addEmptyLine(document);
                return true;
            }

            for (Path diagramPath : diagrams) { addJtlDiagram(document, diagramPath); }
            log.info("Added {} JTL diagrams to the report", diagrams.size());
            return true;
        } catch (Exception e) {
            log.error("Failed to add JTL diagrams: {}", e.getMessage(), e);
            return false;
        }
    }

    private void addJtlDiagram(XWPFDocument document, Path diagramPath) {
        try {
            String fileName = diagramPath.getFileName().toString();
            String title = Arrays.stream(fileName.replace(".png", "").replaceAll("^\\d+_", "").split("_"))
                .map(w -> w.isEmpty() ? w : Character.toUpperCase(w.charAt(0)) + w.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));

            addHeading2(document, title);

            XWPFParagraph imagePara = document.createParagraph();
            imagePara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun imageRun = imagePara.createRun();
            try (FileInputStream imageStream = new FileInputStream(diagramPath.toFile())) {
                imageRun.addPicture(imageStream, XWPFDocument.PICTURE_TYPE_PNG, fileName,
                    Units.toEMU(460), Units.toEMU(230));
            }
            addEmptyLine(document);
        } catch (Exception e) {
            log.error("Failed to add diagram {}: {}", diagramPath, e.getMessage());
        }
    }

    private String formatBaselineValue(Object value) {
        double numeric = toDouble(value);
        if (numeric <= 0) {
            return "Not set";
        }
        return String.format("%.2f", numeric);
    }

    private String formatThroughput(Object value) {
        double numeric = toDouble(value);
        if (numeric <= 0) {
            return "N/A";
        }
        return String.format("%.2f req/s", numeric);
    }

    // ========================================================================
    // 11. CONCLUSION & RECOMMENDATIONS
    // ========================================================================

    @SuppressWarnings("unchecked")
    private void addConclusionSection(XWPFDocument document, TestRun testRun, boolean includeRegression) {
        addHeading1(document, "Conclusion & Recommendations");

        Capability capability = testRun.getCapability();
        boolean hasCriteria = capability.getAcceptanceCriteria() != null
                && !capability.getAcceptanceCriteria().isEmpty()
                && capability.getAcceptanceCriteria().containsKey("criteria");

        addHeading2(document, "Key Findings");
        List<String> findings = new ArrayList<>();
        findings.add(String.format("The test processed %s total requests with an error rate of %s.",
                formatNumber(testRun.getTotalRequests()),
                testRun.getErrorRate() != null ? String.format("%.4f%%", testRun.getErrorRate()) : "N/A"));
        if (testRun.getAvgResponseTime() != null) {
            findings.add(String.format("Average response time was %.2f ms, with P95 of %s and P99 of %s.",
                    testRun.getAvgResponseTime(), formatMs(testRun.getPercentile95()), formatMs(testRun.getPercentile99())));
        }
        if (testRun.getThroughput() != null) {
            findings.add(String.format("The system achieved a throughput of %.2f requests/second over %s.",
                    testRun.getThroughput(), formatDuration(testRun.getTestDurationSeconds())));
        }
        if (hasCriteria) {
            SlaEvaluationResult slaResult = evaluateAllCriteria(testRun);
            findings.add(String.format("SLA compliance: %d of %d criteria met (%s).",
                    slaResult.totalCriteria - slaResult.failedCriteria,
                    slaResult.totalCriteria,
                    slaResult.allPassed ? "FULLY COMPLIANT" : "NON-COMPLIANT"));
        }
        for (String f : findings) { addBulletItem(document, f); }
        addEmptyLine(document);

        addHeading2(document, "Automated Checks");
        List<String> checks = new ArrayList<>();

        SlaEvaluationResult slaResult = evaluateAllCriteria(testRun);
        if (slaResult.totalCriteria > 0) {
            checks.add(String.format("SLA summary: %s (%d of %d criteria met).",
                    slaResult.allPassed ? "PASS" : "FAIL",
                    slaResult.totalCriteria - slaResult.failedCriteria,
                    slaResult.totalCriteria));
        }

        List<String> latencyAlerts = getLatencyAlerts(testRun);
        if (latencyAlerts.isEmpty()) {
            checks.add("Latency distribution: within expected range.");
        } else {
            checks.addAll(latencyAlerts);
        }

        BaselineSummary baselineSummary = getBaselineSummary(testRun);
        if (baselineSummary.hasBaseline) {
            if (baselineSummary.failedLabels.isEmpty()) {
                checks.add("Baseline variance: all labels within thresholds.");
            } else {
                checks.add(String.format("Baseline variance: %d labels failed (%s).",
                        baselineSummary.failedLabels.size(),
                        String.join(", ", baselineSummary.failedLabels)));
            }
        }

        if (includeRegression) {
            RegressionSummary regressionSummary = getRegressionSummary(testRun);
            if (regressionSummary.hasPrevious) {
                if (regressionSummary.issues.isEmpty()) {
                    checks.add(String.format("Regression check: no regressions vs test run #%d.",
                            regressionSummary.previousRunId));
                } else {
                    checks.add(String.format("Regression check vs test run #%d: %s",
                            regressionSummary.previousRunId, String.join("; ", regressionSummary.issues)));
                }
            }
        }

        for (String check : checks) { addBulletItem(document, check); }
        addEmptyLine(document);

        addHeading2(document, "Recommendations");
        List<String> recs = new ArrayList<>();

        if (hasCriteria) {
            SlaEvaluationResult recSlaResult = evaluateAllCriteria(testRun);
            if (!recSlaResult.allPassed) {
                recs.add("Investigate and remediate SLA violations identified in the Results vs Acceptance Criteria section.");
                for (String failedMetric : recSlaResult.failedMetrics) {
                    recs.add(String.format("Review %s — exceeded the defined SLA threshold.", getMetricDisplayName(failedMetric)));
                }
            }
        } else {
            boolean lowError = testRun.getErrorRate() != null && testRun.getErrorRate() < 1.0;
            if (!lowError) { recs.add("Investigate and resolve the root cause of the elevated error rate."); }
        }

        if (testRun.getPercentile99() != null && testRun.getPercentile99() > 1000) {
            recs.add("P99 response time exceeds 1 second. Consider optimizing slow endpoints or adding caching.");
        }
        if (testRun.getPercentile99() != null && testRun.getAvgResponseTime() != null &&
                testRun.getPercentile99() > testRun.getAvgResponseTime() * 5) {
            recs.add("Large gap between average and P99 response times indicates latency spikes. Review resource bottlenecks.");
        }
        if (testRun.getThroughput() != null && testRun.getThroughput() < 100) {
            recs.add("Throughput below 100 req/s. Consider scaling infrastructure or optimizing backend processing.");
        }
        if (recs.isEmpty()) {
            recs.add("System performance is within acceptable limits. Continue monitoring with increased workload scenarios.");
            recs.add("Consider running additional workload profiles to identify breaking points.");
        }
        for (String rec : recs) { addBulletItem(document, rec); }
        addEmptyLine(document);
    }

    // ========================================================================
    // SLA EVALUATION HELPERS
    // ========================================================================

    @SuppressWarnings("unchecked")
    private SlaEvaluationResult evaluateAllCriteria(TestRun testRun) {
        Capability capability = testRun.getCapability();
        Map<String, Object> ac = capability.getAcceptanceCriteria();

        if (ac == null || !ac.containsKey("criteria")) {
            boolean passed = testRun.getErrorRate() != null && testRun.getErrorRate() < 1.0;
            SlaEvaluationResult result = new SlaEvaluationResult();
            result.totalCriteria = 1;
            result.failedCriteria = passed ? 0 : 1;
            result.allPassed = passed;
            if (!passed) result.failedMetrics.add("errorRate");
            return result;
        }

        List<Map<String, Object>> criteria = (List<Map<String, Object>>) ac.get("criteria");
        SlaEvaluationResult result = new SlaEvaluationResult();
        result.totalCriteria = criteria.size();

        for (Map<String, Object> criterion : criteria) {
            String metric = (String) criterion.getOrDefault("metric", "unknown");
            String operator = (String) criterion.getOrDefault("operator", "<=");
            double threshold = toDouble(criterion.get("threshold"));

            Double actual = getMetricValue(testRun, metric);
            boolean passed = actual != null && evaluateCriterion(actual, operator, threshold);

            if (!passed) {
                result.failedCriteria++;
                result.failedMetrics.add(metric);
            }
        }

        result.allPassed = result.failedCriteria == 0;
        return result;
    }

    private Double getMetricValue(TestRun testRun, String metricName) {
        if (metricName == null) return null;
        return switch (metricName.toLowerCase()) {
            case "errorrate", "error_rate" -> testRun.getErrorRate();
            case "avgresponsetime", "avg_response_time", "averageresponsetime" -> testRun.getAvgResponseTime();
            case "minresponsetime", "min_response_time" -> testRun.getMinResponseTime();
            case "maxresponsetime", "max_response_time" -> testRun.getMaxResponseTime();
            case "percentile90", "p90" -> testRun.getPercentile90();
            case "percentile95", "p95" -> testRun.getPercentile95();
            case "percentile99", "p99" -> testRun.getPercentile99();
            case "throughput" -> testRun.getThroughput();
            case "totalrequests", "total_requests" -> testRun.getTotalRequests() != null ? testRun.getTotalRequests().doubleValue() : null;
            case "failedrequests", "failed_requests" -> testRun.getFailedRequests() != null ? testRun.getFailedRequests().doubleValue() : null;
            default -> {
                log.warn("Unknown metric name for SLA evaluation: {}", metricName);
                yield null;
            }
        };
    }

    private boolean evaluateCriterion(Double actual, String operator, double threshold) {
        if (actual == null) return false;
        return switch (operator) {
            case "<=" -> actual <= threshold;
            case ">=" -> actual >= threshold;
            case "<" -> actual < threshold;
            case ">" -> actual > threshold;
            case "==" -> Math.abs(actual - threshold) < 0.0001;
            case "!=" -> Math.abs(actual - threshold) >= 0.0001;
            default -> {
                log.warn("Unknown operator for SLA evaluation: {}", operator);
                yield false;
            }
        };
    }

    private String getMetricDisplayName(String metric) {
        if (metric == null) return "Unknown";
        return switch (metric.toLowerCase()) {
            case "errorrate", "error_rate" -> "Error Rate";
            case "avgresponsetime", "avg_response_time", "averageresponsetime" -> "Avg Response Time";
            case "minresponsetime", "min_response_time" -> "Min Response Time";
            case "maxresponsetime", "max_response_time" -> "Max Response Time";
            case "percentile90", "p90" -> "P90 Response Time";
            case "percentile95", "p95" -> "P95 Response Time";
            case "percentile99", "p99" -> "P99 Response Time";
            case "throughput" -> "Throughput";
            case "totalrequests", "total_requests" -> "Total Requests";
            case "failedrequests", "failed_requests" -> "Failed Requests";
            default -> metric;
        };
    }

    private String formatMetricValue(Double value, String unit) {
        if (value == null) return "N/A";
        if (unit == null || unit.isEmpty()) return String.format("%.2f", value);
        return switch (unit.toLowerCase()) {
            case "%" -> String.format("%.2f%%", value);
            case "ms" -> formatMs(value);
            case "s" -> String.format("%.2f s", value);
            case "req/s", "requests/sec", "requests/second" -> String.format("%.2f req/s", value);
            default -> String.format("%.2f %s", value, unit);
        };
    }

    private double toDouble(Object obj) {
        if (obj == null) return 0.0;
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        try { return Double.parseDouble(obj.toString()); } catch (NumberFormatException e) { return 0.0; }
    }

    private String getPriorityColor(CapabilityTestCase.Priority priority) {
        if (priority == null) return "333333";
        return switch (priority) {
            case CRITICAL -> COLOR_DANGER;
            case HIGH -> COLOR_WARNING;
            case MEDIUM -> COLOR_HEADER_BG;
            case LOW -> COLOR_SUCCESS;
        };
    }

    private Map<String, Object> filterLabelsToTestCases(Map<String, Object> labelData, Capability capability) {
        if (labelData == null || labelData.isEmpty() || capability == null) {
            return new LinkedHashMap<>();
        }

        List<CapabilityTestCase> testCases = capabilityTestCaseRepository
            .findByCapabilityIdOrderByPriorityAsc(capability.getId());
        if (testCases.isEmpty()) {
            return new LinkedHashMap<>();
        }

        Set<String> allowedLabels = testCases.stream()
            .map(tc -> normalizeLabel(tc.getTestCaseName()))
            .filter(label -> !label.isEmpty())
            .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<String, Object> filtered = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : labelData.entrySet()) {
            String normalized = normalizeLabel(entry.getKey());
            if (allowedLabels.contains(normalized)) {
                filtered.put(entry.getKey(), entry.getValue());
            }
        }
        return filtered;
    }

    private String normalizeLabel(String label) {
        if (label == null) {
            return "";
        }
        return label.trim().toLowerCase();
    }

    private List<String> getLatencyAlerts(TestRun testRun) {
        List<String> alerts = new ArrayList<>();
        Double p99 = testRun.getPercentile99();
        Double p95 = testRun.getPercentile95();
        Double avg = testRun.getAvgResponseTime();

        if (p99 != null && p95 != null && p99 > p95 * 2) {
            alerts.add(String.format("Latency distribution alert: P99 (%s) is > 2x P95 (%s).",
                    formatMs(p99), formatMs(p95)));
        }
        if (p99 != null && avg != null && p99 > avg * 5) {
            alerts.add(String.format("Latency distribution alert: P99 (%s) is > 5x Avg (%s).",
                    formatMs(p99), formatMs(avg)));
        }
        return alerts;
    }

    private BaselineSummary getBaselineSummary(TestRun testRun) {
        Map<String, Object> capData = testRun.getCapabilitySpecificData();
        if (capData == null || !capData.containsKey("baselineEvaluation")) {
            return new BaselineSummary(false, List.of());
        }

        Object baselineEvalObj = capData.get("baselineEvaluation");
        if (!(baselineEvalObj instanceof Map)) {
            return new BaselineSummary(false, List.of());
        }

        Map<String, Object> baselineEval = (Map<String, Object>) baselineEvalObj;
        Map<String, Object> results = baselineEval.get("results") instanceof Map
                ? (Map<String, Object>) baselineEval.get("results")
                : new HashMap<>();

        Map<String, Object> filteredResults = filterLabelsToTestCases(results, testRun.getCapability());
        List<String> failedLabels = new ArrayList<>();
        for (Map.Entry<String, Object> entry : filteredResults.entrySet()) {
            if (entry.getValue() instanceof Map eval && Boolean.FALSE.equals(eval.get("pass"))) {
                failedLabels.add(entry.getKey());
            }
        }

        List<String> trimmed = failedLabels.size() > 5
                ? failedLabels.subList(0, 5)
                : failedLabels;
        return new BaselineSummary(true, trimmed);
    }

    private RegressionSummary getRegressionSummary(TestRun testRun) {
        List<TestRun> runs = testRunRepository.findLatestByCapabilityId(testRun.getCapability().getId());
        TestRun previous = null;
        for (TestRun run : runs) {
            if (!run.getId().equals(testRun.getId())) {
                previous = run;
                break;
            }
        }

        if (previous == null) {
            return new RegressionSummary(false, null, List.of());
        }

        List<String> issues = new ArrayList<>();
        Double avg = testRun.getAvgResponseTime();
        Double prevAvg = previous.getAvgResponseTime();
        Double p95 = testRun.getPercentile95();
        Double prevP95 = previous.getPercentile95();
        Double throughput = testRun.getThroughput();
        Double prevThroughput = previous.getThroughput();
        Double errorRate = testRun.getErrorRate();
        Double prevErrorRate = previous.getErrorRate();

        if (avg != null && prevAvg != null && avg > prevAvg * 1.10) {
            issues.add(String.format("Avg latency +%.1f%%", percentChange(avg, prevAvg)));
        }
        if (p95 != null && prevP95 != null && p95 > prevP95 * 1.10) {
            issues.add(String.format("P95 latency +%.1f%%", percentChange(p95, prevP95)));
        }
        if (throughput != null && prevThroughput != null && throughput < prevThroughput * 0.90) {
            issues.add(String.format("Throughput -%.1f%%", percentChange(prevThroughput, throughput)));
        }
        if (errorRate != null && prevErrorRate != null && errorRate - prevErrorRate > 0.5) {
            issues.add(String.format("Error rate +%.2f%%", errorRate - prevErrorRate));
        }

        return new RegressionSummary(true, previous.getId(), issues);
    }

    private double percentChange(double current, double baseline) {
        if (baseline == 0) {
            return 0.0;
        }
        return ((current - baseline) / baseline) * 100.0;
    }

    private static class BaselineSummary {
        private final boolean hasBaseline;
        private final List<String> failedLabels;

        private BaselineSummary(boolean hasBaseline, List<String> failedLabels) {
            this.hasBaseline = hasBaseline;
            this.failedLabels = failedLabels;
        }
    }

    private static class RegressionSummary {
        private final boolean hasPrevious;
        private final Long previousRunId;
        private final List<String> issues;

        private RegressionSummary(boolean hasPrevious, Long previousRunId, List<String> issues) {
            this.hasPrevious = hasPrevious;
            this.previousRunId = previousRunId;
            this.issues = issues;
        }
    }

    private static class SlaEvaluationResult {
        boolean allPassed = true;
        int totalCriteria = 0;
        int failedCriteria = 0;
        List<String> failedMetrics = new ArrayList<>();
    }

    // ========================================================================
    // TEMPLATE-AWARE TEXT HELPERS (use built-in styles)
    // ========================================================================

    private void addHeading1(XWPFDocument document, String text) {
        XWPFParagraph p = document.createParagraph();
        p.setStyle(STYLE_HEADING1);
        keepWithNext(p);
        XWPFRun r = p.createRun();
        r.setText(text);
        r.setFontFamily(FONT_FAMILY);
    }

    private void addHeading2(XWPFDocument document, String text) {
        XWPFParagraph p = document.createParagraph();
        p.setStyle(STYLE_HEADING2);
        keepWithNext(p);
        XWPFRun r = p.createRun();
        r.setText(text);
        r.setFontFamily(FONT_FAMILY);
    }

    private void addBodyParagraph(XWPFDocument document, String text) {
        XWPFParagraph p = document.createParagraph();
        applyStyle(p, STYLE_BODY_TEXT);
        XWPFRun r = p.createRun();
        r.setText(text);
        r.setFontSize(11);
        r.setFontFamily(FONT_FAMILY);
        r.setColor("000000");
    }

    private void addBulletItem(XWPFDocument document, String text) {
        XWPFParagraph p = document.createParagraph();
        applyStyle(p, STYLE_LIST_BULLET);
        XWPFRun r = p.createRun();
        r.setText(text);
        r.setFontSize(11);
        r.setFontFamily(FONT_FAMILY);
        r.setColor("000000");
    }

    private void addEmptyLine(XWPFDocument document) {
        XWPFParagraph p = document.createParagraph();
        applyStyle(p, STYLE_NORMAL);
    }

    private void addPageBreak(XWPFDocument document) {
        XWPFParagraph p = document.createParagraph();
        p.createRun().addBreak(BreakType.PAGE);
    }

    private void keepWithNext(XWPFParagraph paragraph) {
        CTPPr pPr = paragraph.getCTP().isSetPPr()
            ? paragraph.getCTP().getPPr()
            : paragraph.getCTP().addNewPPr();
        if (!pPr.isSetKeepNext()) {
            pPr.addNewKeepNext();
        }
        if (!pPr.isSetKeepLines()) {
            pPr.addNewKeepLines();
        }
    }

    /**
     * Safely apply a named style to a paragraph.
     * Falls back silently if the style doesn't exist in the document.
     */
    private void applyStyle(XWPFParagraph paragraph, String styleId) {
        try {
            paragraph.setStyle(styleId);
        } catch (Exception e) {
            // Style may not exist in a blank document fallback
            log.trace("Style '{}' not found in template, using default", styleId);
        }
    }

    // ========================================================================
    // TABLE CELL HELPERS
    // ========================================================================

    private void setCellText(XWPFTableCell cell, String text, int fontSize, boolean bold, String color) {
        for (int i = cell.getParagraphs().size() - 1; i > 0; i--) { cell.removeParagraph(i); }
        XWPFParagraph p = cell.getParagraphs().get(0);
        p.setSpacingAfter(0); p.setSpacingBefore(0);
        for (int i = p.getRuns().size() - 1; i >= 0; i--) { p.removeRun(i); }
        XWPFRun r = p.createRun();
        r.setText(text != null ? text : "N/A");
        r.setFontSize(fontSize); r.setBold(bold); r.setFontFamily(FONT_FAMILY);
        if (color != null) r.setColor(color);
    }

    private void styleHeaderCell(XWPFTableCell cell, String text) {
        setCellText(cell, text, 9, true, COLOR_HEADER_TEXT);
        setCellShading(cell, COLOR_HEADER_BG);
        cell.getParagraphs().get(0).setAlignment(ParagraphAlignment.CENTER);
        CTTcPr tcPr = getCellProperties(cell);
        CTTcMar margin = tcPr.addNewTcMar();
        margin.addNewTop().setW(BigInteger.valueOf(40));
        margin.addNewBottom().setW(BigInteger.valueOf(40));
        margin.addNewLeft().setW(BigInteger.valueOf(60));
        margin.addNewRight().setW(BigInteger.valueOf(60));
    }

    private void setCellShading(XWPFTableCell cell, String color) {
        CTTcPr tcPr = getCellProperties(cell);
        CTShd shd = tcPr.isSetShd() ? tcPr.getShd() : tcPr.addNewShd();
        shd.setVal(STShd.CLEAR); shd.setColor("auto"); shd.setFill(color);
    }

    private CTTcPr getCellProperties(XWPFTableCell cell) {
        CTTc ctTc = cell.getCTTc();
        return ctTc.isSetTcPr() ? ctTc.getTcPr() : ctTc.addNewTcPr();
    }

    /**
     * Apply Hamza Word template table style (e.g. TableGrid) for consistent rendering.
     */
    private void applyTableStyle(XWPFTable table, String styleId) {
        try {
            table.getCTTbl().addNewTblPr().addNewTblStyle().setVal(styleId);
        } catch (Exception e) {
            log.debug("Could not apply table style {}: {}", styleId, e.getMessage());
        }
    }

    private void applyTableBorders(XWPFTable table, String color, int size) {
        CTTblPr tblPr = table.getCTTbl().getTblPr() != null
            ? table.getCTTbl().getTblPr()
            : table.getCTTbl().addNewTblPr();
        CTTblBorders borders = tblPr.isSetTblBorders()
                ? tblPr.getTblBorders()
                : tblPr.addNewTblBorders();

        setBorder(borders.isSetTop() ? borders.getTop() : borders.addNewTop(), color, size);
        setBorder(borders.isSetBottom() ? borders.getBottom() : borders.addNewBottom(), color, size);
        setBorder(borders.isSetLeft() ? borders.getLeft() : borders.addNewLeft(), color, size);
        setBorder(borders.isSetRight() ? borders.getRight() : borders.addNewRight(), color, size);
        setBorder(borders.isSetInsideH() ? borders.getInsideH() : borders.addNewInsideH(), color, size);
        setBorder(borders.isSetInsideV() ? borders.getInsideV() : borders.addNewInsideV(), color, size);
    }

    private void setBorder(CTBorder border, String color, int size) {
        border.setVal(STBorder.SINGLE);
        border.setColor(color);
        border.setSz(BigInteger.valueOf(size));
        border.setSpace(BigInteger.ZERO);
    }

    private void preventRowSplitting(XWPFTable table) {
        for (XWPFTableRow row : table.getRows()) {
            CTTrPr trPr = row.getCtRow().isSetTrPr()
                ? row.getCtRow().getTrPr()
                : row.getCtRow().addNewTrPr();
            trPr.addNewCantSplit();
        }
    }

    private void setFixedTableLayout(XWPFTable table, int columnCount) {
        int safeColumns = Math.max(1, columnCount);
        int tableWidth = 9000;
        int colWidth = tableWidth / safeColumns;
        table.setWidthType(TableWidthType.DXA);
        table.setWidth(String.valueOf(tableWidth));

        applyTableBorders(table, "000000", 8);

        CTTblGrid grid = table.getCTTbl().getTblGrid() != null
            ? table.getCTTbl().getTblGrid()
            : table.getCTTbl().addNewTblGrid();
        grid.getGridColList().clear();
        for (int i = 0; i < safeColumns; i++) {
            grid.addNewGridCol().setW(BigInteger.valueOf(colWidth));
        }
    }

    // ========================================================================
    // DATA FORMAT HELPERS
    // ========================================================================

    private String formatNumber(Long value) {
        return value == null ? "N/A" : String.format("%,d", value);
    }

    private String formatMs(Double value) {
        if (value == null) return "N/A";
        return value >= 1000 ? String.format("%.2f s", value / 1000.0) : String.format("%.2f ms", value);
    }

    private String formatDuration(Long seconds) {
        if (seconds == null) return "N/A";
        if (seconds < 60) return seconds + " seconds";
        if (seconds < 3600) return String.format("%d min %d sec", seconds / 60, seconds % 60);
        return String.format("%d hr %d min %d sec", seconds / 3600, (seconds % 3600) / 60, seconds % 60);
    }

    private String fmtStat(Map<String, Object> stats, String key) {
        Object val = stats.get(key);
        if (val == null) return "N/A";
        if (val instanceof Number) return String.format("%,d", ((Number) val).longValue());
        return val.toString();
    }

    private String fmtStatDec(Map<String, Object> stats, String key) {
        Object val = stats.get(key);
        if (val == null) return "N/A";
        if (val instanceof Number) return String.format("%.2f", ((Number) val).doubleValue());
        return val.toString();
    }

    private String fmtStatPct(Map<String, Object> stats, String key) {
        Object val = stats.get(key);
        if (val == null) return "N/A";
        if (val instanceof Number) return String.format("%.2f%%", ((Number) val).doubleValue());
        return val.toString();
    }

    private int getPictureType(String fileType) {
        return switch (fileType.toLowerCase()) {
            case "png" -> XWPFDocument.PICTURE_TYPE_PNG;
            case "jpg", "jpeg" -> XWPFDocument.PICTURE_TYPE_JPEG;
            case "gif" -> XWPFDocument.PICTURE_TYPE_GIF;
            case "bmp" -> XWPFDocument.PICTURE_TYPE_BMP;
            default -> XWPFDocument.PICTURE_TYPE_PNG;
        };
    }
}
