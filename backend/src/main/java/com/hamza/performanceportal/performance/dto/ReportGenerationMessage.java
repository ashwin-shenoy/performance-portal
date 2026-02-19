package com.hamza.performanceportal.performance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Message DTO for report generation queue
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportGenerationMessage implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long reportId;
    private String reportTitle;
    private String capability;
    private List<Long> testRunIds;
    private String requestedBy;
    private LocalDateTime requestedAt;
    private String correlationId;
    private String templatePath;
    private String outputFormat;
}

// Made with Bob
