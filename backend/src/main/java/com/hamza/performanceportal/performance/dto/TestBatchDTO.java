package com.hamza.performanceportal.performance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hamza.performanceportal.performance.entity.TestBatch;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for returning test batch information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestBatchDTO {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("batchId")
    private String batchId;

    @JsonProperty("batchName")
    private String batchName;

    @JsonProperty("description")
    private String description;

    @JsonProperty("capabilityId")
    private Long capabilityId;

    @JsonProperty("capabilityName")
    private String capabilityName;

    @JsonProperty("status")
    private String status;

    @JsonProperty("batchResult")
    private String batchResult;

    @JsonProperty("totalTestCases")
    private Integer totalTestCases;

    @JsonProperty("completedTestCases")
    private Integer completedTestCases;

    @JsonProperty("failedTestCases")
    private Integer failedTestCases;

    @JsonProperty("progressPercentage")
    private Double progressPercentage;

    @JsonProperty("startTime")
    private LocalDateTime startTime;

    @JsonProperty("endTime")
    private LocalDateTime endTime;

    @JsonProperty("totalDurationSeconds")
    private Long totalDurationSeconds;

    @JsonProperty("performanceSummary")
    private String performanceSummary;

    @JsonProperty("testRuns")
    private List<TestBatchRunDTO> testRuns;

    @JsonProperty("consolidatedReports")
    private List<BatchReportDTO> consolidatedReports;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    @JsonProperty("updatedAt")
    private LocalDateTime updatedAt;

    /**
     * Convert TestBatch entity to DTO
     */
    public static TestBatchDTO fromEntity(TestBatch batch) {
        return TestBatchDTO.builder()
                .id(batch.getId())
                .batchId(batch.getBatchId())
                .batchName(batch.getBatchName())
                .description(batch.getDescription())
                .capabilityId(batch.getCapability() != null ? batch.getCapability().getId() : null)
                .capabilityName(batch.getCapability() != null ? batch.getCapability().getName() : null)
                .status(batch.getStatus() != null ? batch.getStatus().toString() : null)
                .batchResult(batch.getBatchResult())
                .totalTestCases(batch.getTotalTestCases())
                .completedTestCases(batch.getCompletedTestCases())
                .failedTestCases(batch.getFailedTestCases())
                .progressPercentage(batch.getTotalTestCases() > 0 
                    ? (batch.getCompletedTestCases().doubleValue() / batch.getTotalTestCases()) * 100 
                    : 0.0)
                .startTime(batch.getStartTime())
                .endTime(batch.getEndTime())
                .totalDurationSeconds(batch.getTotalDurationSeconds())
                .performanceSummary(batch.getPerformanceSummary())
                .createdAt(batch.getCreatedAt())
                .updatedAt(batch.getUpdatedAt())
                .build();
    }
}
