package com.hamza.performanceportal.performance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Result DTO for report generation operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportGenerationResult implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String correlationId;
    private Long reportId;
    private String status;
    private boolean success;
    private String message;
    private String reportPath;
    private String reportUrl;
    private Long fileSizeBytes;
    private LocalDateTime generatedAt;
    private Long generationTimeMs;
    private String errorDetails;
}

// Made with Bob
