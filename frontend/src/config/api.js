// API Configuration
// API base path is centralized here; endpoint constants are relative to this base.
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/v1';

export const API_ENDPOINTS = {
  // File Upload
  UPLOAD: '/upload',
  
  // Test Runs
  TESTS: '/tests',
  TEST_BY_ID: (id) => `/tests/${id}`,
  
  // Test Cases
  TEST_CASES: '/testcases',
  
  // Artifacts
  ARTIFACTS: '/artifacts',
  
  // Capabilities
  CAPABILITIES: '/capabilities',
  CAPABILITIES_DROPDOWN: '/capabilities/dropdown',
  CAPABILITY_TEST_CASE_COUNTS: '/capabilities/test-case-counts',
  CAPABILITY_BASELINE: (id) => `/capabilities/${id}/baseline`,
  CAPABILITY_METADATA: (id) => `/capabilities/${id}/metadata`,
  CAPABILITY_ARCHITECTURE_DIAGRAM: (id) => `/capabilities/${id}/architecture-diagram`,
  CAPABILITY_ARCHITECTURE_PREVIEW: (id) => `/capabilities/${id}/architecture-diagram/preview`,
  CAPABILITY_ARCHITECTURE_DOWNLOAD: (id) => `/capabilities/${id}/architecture-diagram/download`,
  CAPABILITY_ARCHITECTURE_DELETE: (id) => `/capabilities/${id}/architecture-diagram`,
  
  // Reports
  GENERATE_WORD_REPORT: (testRunId) => `/reports/generate/jmeter/${testRunId}`,
  REPORTS: '/reports',
  REPORT_VALIDATE_JMETER: (testRunId) => `/reports/validate/jmeter/${testRunId}`,
  REPORT_DOWNLOAD: (id) => `/reports/download/${id}`,
  REPORTS_LIST: '/reports/list',
  DELETE_REPORT: (id) => `/reports/${id}`,

  // Analytics
  ANALYTICS_SUMMARY: '/analytics/summary',
  ANALYTICS_TRENDS: '/analytics/trends',

  // Batch Tests
  BATCH_TEST_STATUS: (batchId) => `/batch-tests/${batchId}`,
  BATCH_TEST_GENERATE_REPORT: (batchId) => `/batch-tests/${batchId}/generate-report`,
  BATCH_TESTS_BY_CAPABILITY: (capabilityId) => `/batch-tests/capability/${capabilityId}`,
};

export default API_BASE_URL;

