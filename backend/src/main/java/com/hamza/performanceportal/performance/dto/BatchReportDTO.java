package com.hamza.performanceportal.performance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hamza.performanceportal.performance.entity.Report;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for consolidated batch reports
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchReportDTO {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("reportType")
    private String reportType;

    @JsonProperty("fileName")
    private String fileName;

    @JsonProperty("filePath")
    private String filePath;

    @JsonProperty("fileSize")
    private Long fileSize;

    @JsonProperty("generatedBy")
    private String generatedBy;

    @JsonProperty("generatedAt")
    private LocalDateTime generatedAt;

    @JsonProperty("description")
    private String description;

    @JsonProperty("isBatchReport")
    private Boolean isBatchReport;

    /**
     * Convert Report entity to batch report DTO
     */
    public static BatchReportDTO fromEntity(Report report) {
        return BatchReportDTO.builder()
                .id(report.getId())
                .reportType(report.getReportType() != null ? report.getReportType().toString() : null)
                .fileName(report.getFileName())
                .filePath(report.getFilePath())
                .fileSize(report.getFileSize())
                .generatedBy(report.getGeneratedBy())
                .generatedAt(report.getGeneratedAt())
                .description(report.getDescription())
                .isBatchReport(report.getTestBatch() != null)
                .build();
    }
}
