# Test Capability API Guide

This guide walks through the end-to-end API flow using a dummy capability named "Test Capability". It shows how to create the capability, add metadata, baseline metrics, test cases, upload a JTL file, and generate reports.

## Base URL

```
http://localhost:8080/api/v1
```

## 1) Create the capability

```bash
curl -X POST "http://localhost:8080/api/v1/capabilities" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Capability",
    "description": "Dummy capability used for API documentation demos.",
    "isActive": true
  }'
```

Example response (trimmed):

```json
{
  "id": 101,
  "name": "Test Capability",
  "description": "Dummy capability used for API documentation demos.",
  "isActive": true
}
```

## 2) Add objective, scope, and environment metadata

```bash
curl -X PUT "http://localhost:8080/api/v1/capabilities/101/metadata" \
  -H "Content-Type: application/json" \
  -d '{
    "testObjective": "Validate response times under steady 200 VU traffic.",
    "testScope": "- Login API\n- Search API\n- Checkout API",
    "environmentDetails": "2x app nodes, 1x DB node, 16GB RAM each"
  }'
```

## 3) Add baseline metrics (per test case label)

```bash
curl -X PUT "http://localhost:8080/api/v1/capabilities/101/baseline" \
  -H "Content-Type: application/json" \
  -d '{
    "p95MaxMs": 1200,
    "avgMaxMs": 600,
    "p90MaxMs": 900,
    "throughputMin": 45
  }'
```

## 4) Create test cases

```bash
curl -X POST "http://localhost:8080/api/v1/capabilities/101/test-cases" \
  -H "Content-Type: application/json" \
  -d '{
    "testCaseName": "Checkout baseline",
    "description": "Validate checkout flow at 200 VU steady traffic.",
    "expectedBehavior": "P95 <= 1200ms, Avg <= 600ms, Throughput >= 45 req/s",
    "priority": "HIGH"
  }'
```

Repeat for additional test cases.

## 5) (Optional) Upload an architecture diagram

```bash
curl -X POST "http://localhost:8080/api/v1/capabilities/101/architecture-diagram" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@/path/to/architecture.png"
```

## 6) Upload a JTL file to create a test run

This creates a test run, parses the JTL, and generates diagrams and metrics.

```bash
curl -X POST "http://localhost:8080/api/v1/upload" \
  -H "Content-Type: multipart/form-data" \
  -F "capability=Test Capability" \
  -F "testName=Test Capability - Baseline Run" \
  -F "buildNumber=1.0.0-demo" \
  -F "description=Baseline test run for documentation" \
  -F "files=@/path/to/sample.jtl"
```

Example response (trimmed):

```json
{
  "success": true,
  "testRunId": 555,
  "message": "Upload completed",
  "filesProcessed": 1
}
```

## 7) View the test run

```bash
curl "http://localhost:8080/api/v1/tests/555"
```

## 8) Save narrative sections (report builder data)

```bash
curl -X POST "http://localhost:8080/api/v1/test-runs/555/document-data" \
  -H "Content-Type: application/json" \
  -d '{
    "capabilityName": "Test Capability",
    "description": "Demo report for Test Capability.",
    "introduction": ["This is a sample introduction paragraph."],
    "benchmarkGoals": ["Meet baseline response targets under 200 VU."],
    "testSetup": ["JMeter 5.6.3", "Heap 4GB", "Ramp-up 2 minutes"],
    "hardwareInfo": ["App nodes: 16 vCPU, 32GB RAM"],
    "scenario1": ["Login + Search + Checkout"],
    "performanceAnalysis": ["No major bottlenecks observed."],
    "capacityPlanning": ["Scale to 400 VU with +1 app node."],
    "conclusions": ["System meets baseline thresholds."],
    "additionalNotes": ["This is dummy content for documentation."]
  }'
```

## 9) Validate cover page requirements

```bash
curl "http://localhost:8080/api/v1/reports/validate/jmeter/555"
```

If fields are missing, the response contains `missingFields`.

## 10) Generate the report

Word report only:

```bash
curl -X POST "http://localhost:8080/api/v1/reports/generate/jmeter/555" \
  -H "Content-Type: application/json" \
  -d "" \
  -G \
  --data-urlencode "includeBaseline=true"
```

Word + PDF together:

```bash
curl -X POST "http://localhost:8080/api/v1/reports/generate/jmeter/555/both" \
  -H "Content-Type: application/json" \
  -d "" \
  -G \
  --data-urlencode "includeBaseline=true"
```

Example response (trimmed):

```json
{
  "success": true,
  "data": {
    "reportId": 901,
    "downloadUrl": "/api/v1/reports/download/901"
  }
}
```

## 11) Download the report

```bash
curl -L "http://localhost:8080/api/v1/reports/download/901" -o Test-Capability-Report.docx
```

## 12) (Optional) View baseline evaluation for a test run

```bash
curl "http://localhost:8080/api/v1/tests/555/baseline-evaluation"
```

## Notes

- Only JTL files are supported for performance data uploads.
- `includeBaseline=false` omits the baseline evaluation section in the report.
- The report uses the template at `templates/ReportTemplate2.docx` if present.
