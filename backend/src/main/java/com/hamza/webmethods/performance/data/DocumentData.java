package com.hamza.durandhar.performance.data;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;
import java.util.List;
import java.util.ArrayList;

/**
 * Base class for report data
 * Contains common report sections that can be populated via frontend UI
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentData {
    
    // Capability identification
    private String capabilityName;
    private String description;
    
    // Report Introduction Section
    @lombok.Builder.Default
    private List<String> introduction = new ArrayList<>();
    
    // Benchmark Goals Section
    @lombok.Builder.Default
    private List<String> benchmarkGoals = new ArrayList<>();
    
    // Capacity Planning Section
    @lombok.Builder.Default
    private List<String> capacityPlanning = new ArrayList<>();
    
    // Hardware/Infrastructure Information
    @lombok.Builder.Default
    private List<String> hardwareInfo = new ArrayList<>();
    
    // Test Setup Details
    @lombok.Builder.Default
    private List<String> testSetup = new ArrayList<>();
    
    // Test Scenarios (up to 10 scenarios)
    @lombok.Builder.Default
    private List<String> scenario1 = new ArrayList<>();
    
    @lombok.Builder.Default
    private List<String> scenario2 = new ArrayList<>();
    
    @lombok.Builder.Default
    private List<String> scenario3 = new ArrayList<>();
    
    @lombok.Builder.Default
    private List<String> scenario4 = new ArrayList<>();
    
    @lombok.Builder.Default
    private List<String> scenario5 = new ArrayList<>();
    
    @lombok.Builder.Default
    private List<String> scenario6 = new ArrayList<>();
    
    @lombok.Builder.Default
    private List<String> scenario7 = new ArrayList<>();
    
    @lombok.Builder.Default
    private List<String> scenario8 = new ArrayList<>();
    
    @lombok.Builder.Default
    private List<String> scenario9 = new ArrayList<>();
    
    @lombok.Builder.Default
    private List<String> scenario10 = new ArrayList<>();
    
    // Architecture Details
    @lombok.Builder.Default
    private List<String> architectureDetails = new ArrayList<>();
    private String architectureDiagram;
    private String architectureDescription;
    
    // Test Infrastructure Details
    private String testInfrastructure;
    private String infrastructureTool;
    
    // CI/CD Details
    @lombok.Builder.Default
    private List<String> cicdDetails = new ArrayList<>();
    private String cicdWorkflow;
    private String cicdTool;
    
    // Test Results Summary
    @lombok.Builder.Default
    private List<String> testResults = new ArrayList<>();
    
    // Performance Analysis
    @lombok.Builder.Default
    private List<String> performanceAnalysis = new ArrayList<>();
    
    // Conclusions and Recommendations
    @lombok.Builder.Default
    private List<String> conclusions = new ArrayList<>();
    
    // Additional Notes
    @lombok.Builder.Default
    private List<String> additionalNotes = new ArrayList<>();
}

// Made with Bob