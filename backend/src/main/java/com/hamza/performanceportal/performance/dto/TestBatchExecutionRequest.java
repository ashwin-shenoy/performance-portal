package com.hamza.performanceportal.performance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for batch test execution requests
 * Contains configuration for running multiple test cases as a single batch
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestBatchExecutionRequest {

    /**
     * Name of the batch execution
     */
    @JsonProperty("batchName")
    private String batchName;

    /**
     * Description of what this batch test covers
     */
    @JsonProperty("description")
    private String description;

    /**
     * ID of the capability being tested
     */
    @JsonProperty("capabilityId")
    private Long capabilityId;

    /**
     * List of test case IDs to run in this batch
     */
    @JsonProperty("testCaseIds")
    private List<Long> testCaseIds;

    /**
     * Test case names if using names instead of IDs
     */
    @JsonProperty("testCaseNames")
    private List<String> testCaseNames;

    /**
     * Optional build number to associate with all tests in batch
     */
    @JsonProperty("buildNumber")
    private String buildNumber;

    /**
     * Whether to generate consolidated report after all tests complete
     */
    @JsonProperty("generateConsolidatedReport")
    @Builder.Default
    private Boolean generateConsolidatedReport = true;

    /**
     * Report types to generate for the batch
     */
    @JsonProperty("reportTypes")
    private List<String> reportTypes;

}
