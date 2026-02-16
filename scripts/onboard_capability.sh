#!/usr/bin/env bash
set -euo pipefail

BASE_URL=${BASE_URL:-http://localhost:8080/api/v1}
ENV_FILE=${ENV_FILE:-""}
CAPABILITY_NAME=${CAPABILITY_NAME:-"Sample Capability"}
CAPABILITY_DESC=${CAPABILITY_DESC:-"Sample capability for onboarding"}
CAPABILITY_ACTIVE=${CAPABILITY_ACTIVE:-true}

TEST_OBJECTIVE=${TEST_OBJECTIVE:-"Validate steady-state performance at 200 VU"}
TEST_SCOPE=${TEST_SCOPE:-"- Login API\n- Search API\n- Checkout API"}
ENVIRONMENT_DETAILS=${ENVIRONMENT_DETAILS:-"2x app nodes, 1x DB node, 16GB RAM each"}

BASELINE_P95=${BASELINE_P95:-1200}
BASELINE_AVG=${BASELINE_AVG:-600}
BASELINE_P90=${BASELINE_P90:-900}
BASELINE_THROUGHPUT=${BASELINE_THROUGHPUT:-45}

TEST_CASE_1_NAME=${TEST_CASE_1_NAME:-"Checkout baseline"}
TEST_CASE_1_DESC=${TEST_CASE_1_DESC:-"Validate checkout flow at 200 VU steady traffic."}
TEST_CASE_1_EXPECTED=${TEST_CASE_1_EXPECTED:-"P95 <= 1200ms, Avg <= 600ms, Throughput >= 45 req/s"}
TEST_CASE_1_PRIORITY=${TEST_CASE_1_PRIORITY:-HIGH}
TEST_CASES_JSON=${TEST_CASES_JSON:-""}
TEST_CASES_FILE=${TEST_CASES_FILE:-""}

ARCH_DIAGRAM=${ARCH_DIAGRAM:-""}
JTL_FILE=${JTL_FILE:-""}
TEST_RUN_NAME=${TEST_RUN_NAME:-"${CAPABILITY_NAME} - Baseline Run"}
BUILD_NUMBER=${BUILD_NUMBER:-"1.0.0"}
RUN_DESCRIPTION=${RUN_DESCRIPTION:-"Baseline test run for onboarding"}
UPLOADED_BY=${UPLOADED_BY:-"developer"}

INCLUDE_BASELINE=${INCLUDE_BASELINE:-true}
OUTPUT_DOC=${OUTPUT_DOC:-"${CAPABILITY_NAME// /-}-Report.docx"}

urlencode() {
  python - <<'PY'
import os, sys, urllib.parse
print(urllib.parse.quote(sys.argv[1]))
PY
}

require_file() {
  local label="$1"
  local path="$2"
  if [[ -z "$path" || ! -f "$path" ]]; then
    echo "[ERROR] ${label} not found: ${path}" >&2
    exit 1
  fi
}

if [[ -n "$ENV_FILE" ]]; then
  if [[ ! -f "$ENV_FILE" ]]; then
    echo "[ERROR] ENV_FILE not found: ${ENV_FILE}" >&2
    exit 1
  fi
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
fi

if [[ -n "$JTL_FILE" ]]; then
  require_file "JTL file" "$JTL_FILE"
else
  echo "[ERROR] JTL_FILE is required" >&2
  exit 1
fi

if [[ -n "$ARCH_DIAGRAM" ]]; then
  require_file "Architecture diagram" "$ARCH_DIAGRAM"
fi

log() {
  echo "[INFO] $1"
}

log "Creating capability..."
capability_resp=$(curl -s -X POST "${BASE_URL}/capabilities" \
  -H "Content-Type: application/json" \
  -d "{\"name\": \"${CAPABILITY_NAME}\", \"description\": \"${CAPABILITY_DESC}\", \"isActive\": ${CAPABILITY_ACTIVE}}")

capability_id=$(python - <<'PY'
import json, sys
try:
    data=json.loads(sys.stdin.read())
    print(data.get("id") or "")
except Exception:
    print("")
PY
<<<"$capability_resp")

if [[ -z "$capability_id" ]]; then
  log "Capability may already exist. Fetching by name..."
  encoded_name=$(python - <<'PY'
import sys, urllib.parse
print(urllib.parse.quote(sys.argv[1]))
PY
"$CAPABILITY_NAME")
  capability_resp=$(curl -s "${BASE_URL}/capabilities/name/${encoded_name}")
  capability_id=$(python - <<'PY'
import json, sys
try:
    data=json.loads(sys.stdin.read())
    print(data.get("id") or "")
except Exception:
    print("")
PY
<<<"$capability_resp")
fi

if [[ -z "$capability_id" ]]; then
  echo "[ERROR] Unable to resolve capability ID" >&2
  exit 1
fi

log "Capability ID: ${capability_id}"

log "Updating capability metadata..."
curl -s -X PUT "${BASE_URL}/capabilities/${capability_id}/metadata" \
  -H "Content-Type: application/json" \
  -d "{\"testObjective\": \"${TEST_OBJECTIVE}\", \"testScope\": \"${TEST_SCOPE}\", \"environmentDetails\": \"${ENVIRONMENT_DETAILS}\"}" \
  >/dev/null

log "Updating baseline metrics..."
curl -s -X PUT "${BASE_URL}/capabilities/${capability_id}/baseline" \
  -H "Content-Type: application/json" \
  -d "{\"p95MaxMs\": ${BASELINE_P95}, \"avgMaxMs\": ${BASELINE_AVG}, \"p90MaxMs\": ${BASELINE_P90}, \"throughputMin\": ${BASELINE_THROUGHPUT}}" \
  >/dev/null

log "Creating test case..."
if [[ -n "$TEST_CASES_FILE" ]]; then
  require_file "Test cases file" "$TEST_CASES_FILE"
  TEST_CASES_JSON=$(cat "$TEST_CASES_FILE")
fi

if [[ -z "$TEST_CASES_JSON" ]]; then
  TEST_CASES_JSON=$(cat <<EOF
[
  {
    "testCaseName": "${TEST_CASE_1_NAME}",
    "description": "${TEST_CASE_1_DESC}",
    "expectedBehavior": "${TEST_CASE_1_EXPECTED}",
    "priority": "${TEST_CASE_1_PRIORITY}"
  }
]
EOF
)
fi

temp_cases_file=$(mktemp)
python - <<'PY' "$TEST_CASES_JSON" > "$temp_cases_file"
import json, sys

def clean(value):
    if value is None:
        return ""
    return str(value).replace("\t", " ").replace("\n", " ").strip()

raw = sys.argv[1]
cases = json.loads(raw)
if not isinstance(cases, list):
    raise SystemExit("TEST_CASES_JSON must be a JSON array")

for case in cases:
    name = clean(case.get("testCaseName"))
    desc = clean(case.get("description"))
    expected = clean(case.get("expectedBehavior"))
    priority = clean(case.get("priority") or "MEDIUM")
    if not name:
        raise SystemExit("Each test case needs testCaseName")
    print("\t".join([name, desc, expected, priority]))
PY

while IFS=$'\t' read -r name desc expected priority; do
  log "Creating test case: ${name}"
  curl -s -X POST "${BASE_URL}/capabilities/${capability_id}/test-cases" \
    -H "Content-Type: application/json" \
    -d "{\"testCaseName\": \"${name}\", \"description\": \"${desc}\", \"expectedBehavior\": \"${expected}\", \"priority\": \"${priority}\"}" \
    >/dev/null
done < "$temp_cases_file"

rm -f "$temp_cases_file"

if [[ -n "$ARCH_DIAGRAM" ]]; then
  log "Uploading architecture diagram..."
  curl -s -X POST "${BASE_URL}/capabilities/${capability_id}/architecture-diagram" \
    -H "Content-Type: multipart/form-data" \
    -F "file=@${ARCH_DIAGRAM}" \
    >/dev/null
fi

log "Uploading JTL and creating test run..."
upload_resp=$(curl -s -X POST "${BASE_URL}/upload" \
  -H "Content-Type: multipart/form-data" \
  -F "capability=${CAPABILITY_NAME}" \
  -F "testName=${TEST_RUN_NAME}" \
  -F "buildNumber=${BUILD_NUMBER}" \
  -F "description=${RUN_DESCRIPTION}" \
  -F "uploadedBy=${UPLOADED_BY}" \
  -F "files=@${JTL_FILE}")

test_run_id=$(python - <<'PY'
import json, sys
try:
    data=json.loads(sys.stdin.read())
    print(data.get("testRunId") or data.get("id") or "")
except Exception:
    print("")
PY
<<<"$upload_resp")

if [[ -z "$test_run_id" ]]; then
  echo "[ERROR] Unable to resolve test run ID" >&2
  echo "$upload_resp" >&2
  exit 1
fi

log "Test run ID: ${test_run_id}"

log "Saving report narrative sections..."
curl -s -X POST "${BASE_URL}/test-runs/${test_run_id}/document-data" \
  -H "Content-Type: application/json" \
  -d "{\"capabilityName\": \"${CAPABILITY_NAME}\", \"description\": \"${RUN_DESCRIPTION}\", \"introduction\": [\"This is a sample introduction paragraph.\"], \"benchmarkGoals\": [\"Meet baseline response targets under 200 VU.\"], \"testSetup\": [\"JMeter 5.6.3\", \"Heap 4GB\", \"Ramp-up 2 minutes\"], \"hardwareInfo\": [\"App nodes: 16 vCPU, 32GB RAM\"], \"scenario1\": [\"Login + Search + Checkout\"], \"performanceAnalysis\": [\"No major bottlenecks observed.\"], \"capacityPlanning\": [\"Scale to 400 VU with +1 app node.\"], \"conclusions\": [\"System meets baseline thresholds.\"], \"additionalNotes\": [\"This is onboarding content.\"]}" \
  >/dev/null

log "Validating cover page fields..."
validation_resp=$(curl -s "${BASE_URL}/reports/validate/jmeter/${test_run_id}")
echo "$validation_resp"

log "Generating report..."
report_resp=$(curl -s -X POST "${BASE_URL}/reports/generate/jmeter/${test_run_id}" \
  -H "Content-Type: application/json" \
  -d "" \
  -G \
  --data-urlencode "includeBaseline=${INCLUDE_BASELINE}")

report_id=$(python - <<'PY'
import json, sys
try:
    data=json.loads(sys.stdin.read())
    data = data.get("data", data)
    print(data.get("reportId") or data.get("id") or "")
except Exception:
    print("")
PY
<<<"$report_resp")

if [[ -z "$report_id" ]]; then
  echo "[WARN] Report ID not found in response" >&2
  echo "$report_resp" >&2
  exit 0
fi

log "Downloading report ${report_id} -> ${OUTPUT_DOC}"
curl -s -L "${BASE_URL}/reports/download/${report_id}" -o "${OUTPUT_DOC}"

log "Done."
