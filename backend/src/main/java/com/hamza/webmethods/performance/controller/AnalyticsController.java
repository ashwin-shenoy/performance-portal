package com.hamza.durandhar.performance.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.hamza.durandhar.performance.repository.TestRunRepository;
import com.hamza.durandhar.performance.repository.ReportRepository;
import com.hamza.durandhar.performance.entity.TestRun;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * REST controller for analytics and dashboard statistics
 */
@Slf4j
@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
@CrossOrigin(origins = {
    "http://localhost:3000",
    "http://localhost:5173"
}, allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class AnalyticsController {

    private final TestRunRepository testRunRepository;
    private final ReportRepository reportRepository;

    /**
     * Get dashboard summary statistics
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary() {
        log.info("Fetching analytics summary");
        
        try {
            long totalTests = testRunRepository.count();
            long completedTests = testRunRepository.countByStatus(TestRun.TestStatus.COMPLETED);
            long totalReports = reportRepository.count();
            
            Map<String, Object> summary = new HashMap<>();
            summary.put("totalTests", totalTests);
            summary.put("completedTests", completedTests);
            summary.put("totalReports", totalReports);
            
            log.info("Analytics summary: {} total tests, {} completed, {} reports", 
                    totalTests, completedTests, totalReports);
            
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            log.error("Error fetching analytics summary", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get test execution trends over time
     */
    @GetMapping("/trends")
    public ResponseEntity<List<Map<String, Object>>> getTrends() {
        log.info("Fetching analytics trends");
        
        try {
            // Get test runs from the last 6 months
            LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);
            List<TestRun> recentTests = testRunRepository.findByTestDateAfter(sixMonthsAgo);
            
            // Group by month
            Map<String, Long> monthlyCount = recentTests.stream()
                .collect(Collectors.groupingBy(
                    test -> test.getTestDate().format(DateTimeFormatter.ofPattern("MMM yyyy")),
                    Collectors.counting()
                ));
            
            // Convert to list of maps for frontend
            List<Map<String, Object>> trends = new ArrayList<>();
            
            // Generate last 6 months
            for (int i = 5; i >= 0; i--) {
                LocalDateTime month = LocalDateTime.now().minusMonths(i);
                String monthKey = month.format(DateTimeFormatter.ofPattern("MMM yyyy"));
                String shortMonth = month.format(DateTimeFormatter.ofPattern("MMM"));
                
                Map<String, Object> dataPoint = new HashMap<>();
                dataPoint.put("date", shortMonth);
                dataPoint.put("tests", monthlyCount.getOrDefault(monthKey, 0L));
                trends.add(dataPoint);
            }
            
            log.info("Analytics trends: {} data points", trends.size());
            
            return ResponseEntity.ok(trends);
        } catch (Exception e) {
            log.error("Error fetching analytics trends", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

// Made with Bob