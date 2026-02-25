package com.hamza.performanceportal.performance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hamza.performanceportal.performance.entity.TestRun;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for test run information within a batch
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestBatchRunDTO {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("testName")
    private String testName;

    @JsonProperty("status")
    private String status;

    @JsonProperty("totalRequests")
    private Long totalRequests;

    @JsonProperty("successfulRequests")
    private Long successfulRequests;

    @JsonProperty("failedRequests")
    private Long failedRequests;

    @JsonProperty("avgResponseTime")
    private Double avgResponseTime;

    @JsonProperty("minResponseTime")
    private Double minResponseTime;

    @JsonProperty("maxResponseTime")
    private Double maxResponseTime;

    @JsonProperty("percentile90")
    private Double percentile90;

    @JsonProperty("percentile95")
    private Double percentile95;

    @JsonProperty("percentile99")
    private Double percentile99;

    @JsonProperty("throughput")
    private Double throughput;

    @JsonProperty("errorRate")
    private Double errorRate;

    @JsonProperty("testDurationSeconds")
    private Long testDurationSeconds;

    @JsonProperty("virtualUsers")
    private Integer virtualUsers;

    @JsonProperty("buildNumber")
    private String buildNumber;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    /**
     * Convert TestRun entity to batch run DTO
     */
    public static TestBatchRunDTO fromEntity(TestRun testRun) {
        return TestBatchRunDTO.builder()
                .id(testRun.getId())
                .testName(testRun.getTestName())
                .status(testRun.getStatus() != null ? testRun.getStatus().toString() : null)
                .totalRequests(testRun.getTotalRequests())
                .successfulRequests(testRun.getSuccessfulRequests())
                .failedRequests(testRun.getFailedRequests())
                .avgResponseTime(testRun.getAvgResponseTime())
                .minResponseTime(testRun.getMinResponseTime())
                .maxResponseTime(testRun.getMaxResponseTime())
                .percentile90(testRun.getPercentile90())
                .percentile95(testRun.getPercentile95())
                .percentile99(testRun.getPercentile99())
                .throughput(testRun.getThroughput())
                .errorRate(testRun.getErrorRate())
                .testDurationSeconds(testRun.getTestDurationSeconds())
                .virtualUsers(testRun.getVirtualUsers())
                .buildNumber(testRun.getBuildNumber())
                .createdAt(testRun.getCreatedAt())
                .build();
    }
}
