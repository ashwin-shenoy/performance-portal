# Batch Test Execution Feature

## Overview

The Batch Test Execution feature allows you to run multiple test cases in isolation under a single **Batch ID** and generate a consolidated report combining all results.

### Architecture

```
Batch Test Execution Flow:

1. Create Batch
   └─ Define batch name, test cases, capability
   └─ Batch created with PENDING status
   └─ Unique Batch ID generated (UUID)

2. Execute Batch
   └─ Batch status → IN_PROGRESS
   └─ Each test case runs independently
   └─ Results tracked under same Batch ID

3. Track Results
   └─ Each test completion updates batch
   └─ Aggregates performance metrics
   └─ Calculates progress percentage

4. Finalize & Report
   └─ Batch status → COMPLETED/FAILED
   └─ Generate consolidated report
   └─ Combines all test case results
```

## Data Model

### TestBatch Entity

```
TestBatch
├── id (Long) - Primary Key
├── batchId (String) - Unique batch identifier (UUID)
├── batchName (String) - Human-readable batch name
├── description (String) - Batch description
├── capability (Capability) - Associated capability
├── status (BatchStatus) - PENDING, IN_PROGRESS, COMPLETED, FAILED, CANCELLED
├── totalTestCases (Integer) - Number of test cases in batch
├── completedTestCases (Integer) - Completed count
├── failedTestCases (Integer) - Failed count
├── batchResult (String) - PASS/FAIL
├── performanceSummary (String) - Aggregated metrics
├── startTime (LocalDateTime)
├── endTime (LocalDateTime)
├── totalDurationSeconds (Long)
├── testRuns (List<TestRun>) - Associated test runs
└── consolidatedReports (List<Report>) - Generated reports
```

### TestRun Modifications

Added optional reference to TestBatch:
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "test_batch_id")
private TestBatch testBatch;
```

### Report Modifications

Added optional reference to TestBatch for consolidated reports:
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "test_batch_id")
private TestBatch testBatch;
```

## API Endpoints

### 1. Create a Batch Test

**Endpoint:** `POST /api/v1/batch-tests/create`

**Request:**
```json
{
  "batchName": "LoadTest_Iteration_1",
  "description": "Performance testing for JSON payload with various sizes",
  "capabilityId": 1,
  "testCaseIds": [1, 2, 3, 4, 5],
  "buildNumber": "v2.1.0",
  "generateConsolidatedReport": true,
  "reportTypes": ["TECHNICAL_PDF", "EXECUTIVE_PDF"]
}
```

**Alternative (using test case names):**
```json
{
  "batchName": "XMLTest_Batch",
  "description": "XML parsing performance tests",
  "capabilityId": 2,
  "testCaseNames": [
    "ParseXML_1000k",
    "ParseXML_SSL_1k",
    "WS_ParseXML_2SSL_1000k"
  ]
}
```

**Response:**
```json
{
  "success": true,
  "message": "Batch created successfully",
  "data": {
    "id": 1,
    "batchId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
    "batchName": "LoadTest_Iteration_1",
    "capabilityId": 1,
    "status": "PENDING",
    "totalTestCases": 5,
    "completedTestCases": 0,
    "failedTestCases": 0,
    "progressPercentage": 0.0,
    "createdAt": "2026-02-19T16:30:00"
  }
}
```

### 2. Execute Batch

**Endpoint:** `POST /api/v1/batch-tests/{batchId}/execute`

**Response:**
```json
{
  "success": true,
  "message": "Batch execution started",
  "data": {
    "id": 1,
    "batchId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
    "status": "IN_PROGRESS",
    "startTime": "2026-02-19T16:31:00"
  }
}
```

### 3. Get Batch Status

**Endpoint:** `GET /api/v1/batch-tests/{batchId}`

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "batchId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
    "batchName": "LoadTest_Iteration_1",
    "status": "COMPLETED",
    "batchResult": "PASS",
    "totalTestCases": 5,
    "completedTestCases": 5,
    "failedTestCases": 0,
    "progressPercentage": 100.0,
    "totalDurationSeconds": 325,
    "performanceSummary": "Total Requests: 500000, Avg Response Time: 45.32ms, Avg Throughput: 1538.46 req/s, Overall Error Rate: 0.02%",
    "testRuns": [
      {
        "id": 101,
        "testName": "JSON_WS_prettyFalse_1000k",
        "status": "COMPLETED",
        "totalRequests": 100000,
        "avgResponseTime": 45.2,
        "throughput": 1500.5,
        "errorRate": 0.01
      },
      {
        "id": 102,
        "testName": "JSON_WS_prettyTrue_SSL_1k",
        "status": "COMPLETED",
        "totalRequests": 100000,
        "avgResponseTime": 45.5,
        "throughput": 1520.0,
        "errorRate": 0.03
      }
      // ... more test runs
    ],
    "consolidatedReports": [
      {
        "id": 201,
        "reportType": "TECHNICAL_PDF",
        "fileName": "LoadTest_Iteration_1_consolidated_pdf.pdf",
        "generatedAt": "2026-02-19T16:35:00",
        "fileSize": 5242880
      }
    ]
  }
}
```

### 4. Update Batch Result

**Endpoint:** `POST /api/v1/batch-tests/{batchId}/update-result`

Called automatically after each test case completes.

**Request:**
```json
{
  "testRunId": 101,
  "success": true
}
```

### 5. Generate Consolidated Report

**Endpoint:** `POST /api/v1/batch-tests/{batchId}/generate-report`

**Request (Optional):**
```json
{
  "reportTypes": ["TECHNICAL_PDF", "EXECUTIVE_PDF", "TECHNICAL_WORD"]
}
```

**Response:**
```json
{
  "success": true,
  "message": "Reports generated successfully",
  "data": {
    "batchId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
    "consolidatedReports": [
      {
        "id": 201,
        "reportType": "TECHNICAL_PDF",
        "fileName": "LoadTest_Iteration_1_consolidated_pdf.pdf"
      },
      {
        "id": 202,
        "reportType": "EXECUTIVE_PDF",
        "fileName": "LoadTest_Iteration_1_consolidated_executive.pdf"
      }
    ]
  }
}
```

### 6. Get All Batches for a Capability

**Endpoint:** `GET /api/v1/batch-tests/capability/{capabilityId}`

**Response:**
```json
{
  "success": true,
  "count": 3,
  "data": [
    {
      "id": 1,
      "batchId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
      "batchName": "LoadTest_Iteration_1",
      "status": "COMPLETED",
      "totalTestCases": 5,
      "completedTestCases": 5,
      "failedTestCases": 0
    },
    // ... more batches
  ]
}
```

## Usage Example

```bash
# 1. Create a batch with multiple test cases
curl -X POST http://localhost:8080/api/v1/batch-tests/create \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "batchName": "Performance_Test_Suite_v1",
    "description": "Comprehensive performance testing",
    "capabilityId": 1,
    "testCaseNames": [
      "Cache_Enable_1k",
      "JSON_WS_prettyFalse_1000k",
      "JSON_WS_prettyTrue_SSL_1k",
      "ParseXML_1000k",
      "rest_100k_noTT"
    ],
    "buildNumber": "v2.1.0",
    "generateConsolidatedReport": true,
    "reportTypes": ["TECHNICAL_PDF", "EXECUTIVE_PDF"]
  }'

# 2. Get response with batchId
# Response: batchId = "f47ac10b-58cc-4372-a567-0e02b2c3d479"

# 3. Execute the batch
curl -X POST http://localhost:8080/api/v1/batch-tests/f47ac10b-58cc-4372-a567-0e02b2c3d479/execute \
  -H "Authorization: Bearer $TOKEN"

# 4. Poll batch status
curl -X GET http://localhost:8080/api/v1/batch-tests/f47ac10b-58cc-4372-a567-0e02b2c3d479 \
  -H "Authorization: Bearer $TOKEN"

# 5. Generate consolidated report (when batch completes)
curl -X POST http://localhost:8080/api/v1/batch-tests/f47ac10b-58cc-4372-a567-0e02b2c3d479/generate-report \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "reportTypes": ["TECHNICAL_PDF", "EXECUTIVE_PDF", "TECHNICAL_WORD"]
  }'
```

## Batch Status Flow

```
PENDING
   ↓
IN_PROGRESS (tests running)
   ├─ [Test 1] → updateBatchResult() → completedTestCases++
   ├─ [Test 2] → updateBatchResult() → completedTestCases++
   ├─ [Test 3] → updateBatchResult() → completedTestCases++
   ├─ [Test 4] → updateBatchResult() → failedTestCases++
   └─ [Test 5] → updateBatchResult() → completedTestCases++
   ↓
COMPLETED (if all passed) or FAILED (if any failed)
   ↓
Reports generated
```

## Performance Metrics Aggregation

When a batch completes, the following metrics are calculated automatically:

```
Performance Summary:
├── Total Requests: Sum of all test runs
├── Average Response Time: Mean of avg response times
├── Average Throughput: Mean of throughput values
├── Overall Error Rate: Mean of error rates
├── Total Duration: Time from start to finish
└── Success Rate: (completedTestCases / totalTestCases) * 100
```

## Database Schema

### New Table: test_batches

```sql
CREATE TABLE test_batches (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    batch_id VARCHAR(36) NOT NULL UNIQUE,
    capability_id BIGINT NOT NULL,
    batch_name VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL,
    total_test_cases INT NOT NULL,
    completed_test_cases INT DEFAULT 0,
    failed_test_cases INT DEFAULT 0,
    batch_result VARCHAR(20),
    performance_summary TEXT,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    total_duration_seconds BIGINT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    
    FOREIGN KEY (capability_id) REFERENCES capabilities(id),
    INDEX idx_batch_capability (capability_id),
    INDEX idx_batch_status (status),
    INDEX idx_batch_batch_id (batch_id)
);
```

### Modified Table: test_runs

```sql
ALTER TABLE test_runs ADD COLUMN test_batch_id BIGINT;
ALTER TABLE test_runs ADD FOREIGN KEY (test_batch_id) REFERENCES test_batches(id);
ALTER TABLE test_runs ADD INDEX idx_test_batch (test_batch_id);
```

### Modified Table: reports

```sql
ALTER TABLE reports ADD COLUMN test_batch_id BIGINT;
ALTER TABLE reports ADD FOREIGN KEY (test_batch_id) REFERENCES test_batches(id);
ALTER TABLE reports ADD INDEX idx_batch_report (test_batch_id);
```

## Benefits

✅ **Grouping**: Run related test cases together  
✅ **Isolation**: Each test case runs independently  
✅ **Tracking**: Single Batch ID for all results  
✅ **Consolidation**: Aggregate metrics and reporting  
✅ **Scalability**: Supports sequential execution for controlled isolation  
✅ **Traceability**: Complete history of all batch executions  

## Implementation Notes

- All timestamp fields use `LocalDateTime` (no timezone info stored)
- Batch ID is auto-generated as UUID v4
- Test results are aggregated in real-time as tests complete
- Reports can be generated in multiple formats (PDF, Word, HTML, CSV)
- Supports only sequential test execution mode
