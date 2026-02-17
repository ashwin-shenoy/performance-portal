// API Configuration
// All endpoints include /api/v1 prefix to match backend @RequestMapping
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/v1';

export const API_ENDPOINTS = {
  // Authentication
  LOGIN: '/api/v1/auth/login',
  REGISTER: '/api/v1/auth/register',
  REFRESH: '/api/v1/auth/refresh',
  
  // File Upload
  UPLOAD: '/api/v1/upload',
  UPLOAD_TEST_RUN: '/api/v1/upload/test-run',
  UPLOAD_STATUS: (id) => `/api/v1/upload/status/${id}`,
  
  // Test Runs
  TESTS: '/api/v1/tests',
  TEST_BY_ID: (id) => `/api/v1/tests/${id}`,
  
  // Test Cases
  TEST_CASES: '/api/v1/testcases',
  TEST_CASE_BY_ID: (id) => `/api/v1/testcases/${id}`,
  TEST_CASE_STATS: '/api/v1/testcases/stats/by-status',
  TEST_CASE_STATISTICS: (testRunId) => `/api/v1/testcases/test-run/${testRunId}/statistics`,
  
  // Artifacts
  ARTIFACTS: '/api/v1/artifacts',
  ARTIFACT_BY_ID: (id) => `/api/v1/artifacts/${id}`,
  ARTIFACTS_BY_TEST_RUN: (testRunId) => `/api/v1/artifacts/test-run/${testRunId}`,
  ARTIFACT_DOWNLOAD: (id) => `/api/v1/artifacts/${id}/download`,
  ARTIFACT_PREVIEW: (id) => `/api/v1/artifacts/${id}/preview`,
  ARCHITECTURE_DIAGRAM: (testRunId) => `/api/v1/artifacts/test-run/${testRunId}/architecture-diagram`,
  TEST_CASES_SUMMARY: (testRunId) => `/api/v1/artifacts/test-run/${testRunId}/test-cases-summary`,
  
  // Capabilities
  CAPABILITIES: '/api/v1/capabilities',
  CAPABILITIES_DROPDOWN: '/api/v1/capabilities/dropdown',
  CAPABILITIES_ACTIVE: '/api/v1/capabilities/active',
  CAPABILITY_TEST_CASE_COUNTS: '/api/v1/capabilities/test-case-counts',
  CAPABILITY_BASELINE: (id) => `/api/v1/capabilities/${id}/baseline`,
  CAPABILITY_ARCHITECTURE_DIAGRAM: (id) => `/api/v1/capabilities/${id}/architecture-diagram`,
  CAPABILITY_ARCHITECTURE_PREVIEW: (id) => `/api/v1/capabilities/${id}/architecture-diagram/preview`,
  CAPABILITY_ARCHITECTURE_DOWNLOAD: (id) => `/api/v1/capabilities/${id}/architecture-diagram/download`,
  CAPABILITY_ARCHITECTURE_DELETE: (id) => `/api/v1/capabilities/${id}/architecture-diagram`,
  
  // Reports
  GENERATE_REPORT: '/api/v1/reports/generate',
  GENERATE_PDF_REPORT: (testRunId) => `/api/v1/reports/generate/jmeter/${testRunId}`,
  GENERATE_WORD_REPORT: (testRunId) => `/api/v1/reports/generate/jmeter/${testRunId}`,
  GENERATE_CAPABILITY_REPORT: '/api/v1/reports/generate/capability',
  GENERATE_WORD_TEMPLATE: (capabilityName) => `/api/v1/reports/generate/word/${capabilityName}`,
  REPORT_BY_ID: (id) => `/api/v1/reports/${id}`,
  REPORT_VALIDATE_JMETER: (testRunId) => `/api/v1/reports/validate/jmeter/${testRunId}`,
  REPORT_DOWNLOAD: (id) => `/api/v1/reports/download/${id}`,
  REPORT_STATUS: (testRunId) => `/api/v1/reports/status/${testRunId}`,
  REPORTS_BY_TEST_RUN: (testRunId) => `/api/v1/reports/test-run/${testRunId}`,
  REPORTS_LIST: '/api/v1/reports/list',
  DELETE_REPORT: (id) => `/api/v1/reports/${id}`,

  // Document Data
  DOCUMENT_DATA: (testRunId) => `/api/v1/test-runs/${testRunId}/document-data`,
  
  // Analytics
  ANALYTICS_SUMMARY: '/api/v1/analytics/summary',
  ANALYTICS_TRENDS: '/api/v1/analytics/trends',
};

export default API_BASE_URL;

// Made with Bob
