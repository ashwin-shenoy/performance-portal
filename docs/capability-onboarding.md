# Capability Onboarding (API)

This guide lists the minimum data you should provide to produce a complete report and gives a ready-to-run shell script.

## Required details for a complete report

1) Capability record
- `name`
- `description` (recommended)
- `isActive`

2) Capability metadata (cover page requirements)
- `testObjective`
- `testScope`
- `environmentDetails`

3) Test cases (used in the report and to filter label stats)
- `testCaseName`
- `description`
- `expectedBehavior`
- `priority`

4) Baseline metrics (optional, but required if you want baseline evaluation)
- `p95MaxMs`, `avgMaxMs`, `p90MaxMs`, `throughputMin`

5) Architecture diagram (optional)
- One diagram per capability

6) JTL upload (creates the test run)
- `capability` (must match capability name)
- `testName`
- `buildNumber`
- `description` (recommended)
- `files` (JTL only)

7) Report narrative sections (document data)
- Introduction, benchmark goals, test setup, hardware info, scenario, analysis, conclusions, notes

8) Validate cover page and generate report
- Validate: `/reports/validate/jmeter/{testRunId}`
- Generate: `/reports/generate/jmeter/{testRunId}`

## Notes

- Test case names should match JTL label names if you want per-test-case label stats and baseline evaluation.
- `includeBaseline=false` skips the baseline section during report generation.
- Architecture diagrams are stored on disk and the path is saved in `capabilities.architecture_diagram_path`.

## Shell script

Use the script at [scripts/onboard_capability.sh](../scripts/onboard_capability.sh).

### Quick start

Use the provided templates:

```bash
ENV_FILE=./scripts/onboard.env \
TEST_CASES_FILE=./scripts/test-cases.json \
./scripts/onboard_capability.sh
```

### Optional .env file

Create a `.env` file and pass it with `ENV_FILE`.

Example:

```bash
ENV_FILE=./onboard.env ./scripts/onboard_capability.sh
```

### Multiple test cases

Provide test cases as JSON in `TEST_CASES_JSON` or point `TEST_CASES_FILE` to a JSON file.

Example JSON:

```json
[
	{
		"testCaseName": "Checkout baseline",
		"description": "Validate checkout flow at 200 VU steady traffic.",
		"expectedBehavior": "P95 <= 1200ms, Avg <= 600ms, Throughput >= 45 req/s",
		"priority": "HIGH"
	},
	{
		"testCaseName": "Search baseline",
		"description": "Validate search latency under steady traffic.",
		"expectedBehavior": "P95 <= 900ms, Avg <= 450ms, Throughput >= 60 req/s",
		"priority": "MEDIUM"
	}
]
```

Run:

```bash
TEST_CASES_FILE=./test-cases.json ./scripts/onboard_capability.sh
```
