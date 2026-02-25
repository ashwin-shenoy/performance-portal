# Capability Modification APIs

This guide documents APIs used to modify capability details, including metadata, baseline metrics, test cases, diagrams, and capability deletion.

## Base URL

`http://localhost:8080/api/v1`

## 1) Update core capability details

Updates the main capability record.

**Endpoint**

`PUT /capabilities/{id}`

**Body**

- `name` (string)
- `description` (string)
- `isActive` (boolean)
- `testObjective` (string, optional)
- `testScope` (string, optional)
- `environmentDetails` (string, optional)
- `acceptanceCriteria` (object, optional)

**Example**

```bash
curl -X PUT "http://localhost:8080/api/v1/capabilities/101" \
	-H "Content-Type: application/json" \
	-d '{
		"name": "Order Management",
		"description": "Updated capability description",
		"isActive": true,
		"testObjective": "Validate latency under 200 virtual users",
		"testScope": "Login, search, and checkout APIs",
		"environmentDetails": "2 app nodes, 1 DB node",
		"acceptanceCriteria": {
			"criteria": "P95 < 1200ms"
		}
	}'
```

## 2) Update metadata only

Updates test objective/scope/environment details and acceptance criteria without changing name/description.

**Endpoint**

`PUT /capabilities/{id}/metadata`

**Body**

- `testObjective` (string, optional)
- `testScope` (string, optional)
- `environmentDetails` (string, optional)
- `acceptanceCriteria` (object, optional)

**Example**

```bash
curl -X PUT "http://localhost:8080/api/v1/capabilities/101/metadata" \
	-H "Content-Type: application/json" \
	-d '{
		"testObjective": "Validate response times",
		"testScope": "Critical APIs only",
		"environmentDetails": "Performance environment - staging",
		"acceptanceCriteria": {
			"criteria": "P95 < 1000ms"
		}
	}'
```

**Success response (example)**

```json
{
	"success": true,
	"message": "Capability metadata updated successfully",
	"capabilityId": 101
}
```

## 3) Update baseline metrics

Stores baseline metrics under `acceptanceCriteria.baseline`.

**Endpoint**

`PUT /capabilities/{id}/baseline`

**Body**

- `p95MaxMs` (number)
- `avgMaxMs` (number)
- `p90MaxMs` (number)
- `throughputMin` (number)

**Example**

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

## 4) Toggle active/inactive state

Flips the current status of `isActive`.

**Endpoint**

`PATCH /capabilities/{id}/toggle`

**Example**

```bash
curl -X PATCH "http://localhost:8080/api/v1/capabilities/101/toggle"
```

## 5) Modify test cases under a capability

### Create test case

`POST /capabilities/{id}/test-cases`

```bash
curl -X POST "http://localhost:8080/api/v1/capabilities/101/test-cases" \
	-H "Content-Type: application/json" \
	-d '{
		"testCaseName": "Checkout baseline",
		"description": "Validate checkout flow at steady load",
		"expectedBehavior": "P95 <= 1200ms",
		"priority": "HIGH"
	}'
```

### Update test case

`PUT /capabilities/{id}/test-cases/{testCaseId}`

```bash
curl -X PUT "http://localhost:8080/api/v1/capabilities/101/test-cases/501" \
	-H "Content-Type: application/json" \
	-d '{
		"testCaseName": "Checkout baseline",
		"description": "Updated test case description",
		"expectedBehavior": "P95 <= 1100ms",
		"priority": "MEDIUM"
	}'
```

### Delete test case

`DELETE /capabilities/{id}/test-cases/{testCaseId}`

```bash
curl -X DELETE "http://localhost:8080/api/v1/capabilities/101/test-cases/501"
```

## 6) Update capability architecture diagram

### Upload / replace diagram

`POST /capabilities/{id}/architecture-diagram`

```bash
curl -X POST "http://localhost:8080/api/v1/capabilities/101/architecture-diagram" \
	-H "Content-Type: multipart/form-data" \
	-F "file=@/path/to/architecture.png"
```

### Delete diagram

`DELETE /capabilities/{id}/architecture-diagram`

```bash
curl -X DELETE "http://localhost:8080/api/v1/capabilities/101/architecture-diagram"
```

## 7) Delete capability

Deletes the capability by ID.

**Endpoint**

`DELETE /capabilities/{id}`

**Example**

```bash
curl -X DELETE "http://localhost:8080/api/v1/capabilities/101"
```

**Response**

- `204 No Content` when deletion succeeds.
- `404 Not Found` when capability does not exist.

## Common error behavior

- `404 Not Found`: Capability or test case ID not found.
- `400 Bad Request`: Invalid create request (for example duplicate capability name on create).
- `500 Internal Server Error`: File upload/delete failures for architecture diagram operations.

## Source

- `backend/src/main/java/com/hamza/performanceportal/performance/controller/CapabilityController.java`
- `backend/src/main/java/com/hamza/performanceportal/performance/service/CapabilityService.java`
