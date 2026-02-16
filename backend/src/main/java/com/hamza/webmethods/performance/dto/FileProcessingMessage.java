package com.hamza.durandhar.performance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Message DTO for file processing queue
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileProcessingMessage implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long testRunId;
    private String capability;
    private List<String> filePaths;
    private String fileType;
    private String uploadedBy;
    private LocalDateTime uploadedAt;
    private String correlationId;
}

// Made with Bob
