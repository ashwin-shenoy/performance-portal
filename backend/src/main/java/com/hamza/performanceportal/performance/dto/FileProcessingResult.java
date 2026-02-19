package com.hamza.performanceportal.performance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Result DTO for file processing operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileProcessingResult implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String correlationId;
    private Long testRunId;
    private String status;
    private boolean success;
    private String message;
    private List<String> processedFiles;
    private List<String> failedFiles;
    private Map<String, String> errors;
    private LocalDateTime processedAt;
    private Long processingTimeMs;
    private Integer metricsCount;
    private Integer transactionsCount;
}

// Made with Bob
