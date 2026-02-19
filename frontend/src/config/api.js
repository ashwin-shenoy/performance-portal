// API Configuration
// API base path is centralized here; endpoint constants are relative to this base.
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/v1';

export const API_ENDPOINTS = {
  // Authentication
  LOGIN: '/auth/login',
  REGISTER: '/auth/register',
  REFRESH: '/auth/refresh',
  
  // File Upload
  UPLOAD: '/upload',
  UPLOAD_TEST_RUN: '/upload/test-run',
  UPLOAD_STATUS: (id) => `/upload/status/${id}`,
  
  // Test Runs
  TESTS: '/tests',
  TEST_BY_ID: (id) => `/tests/${id}`,
  
  // Test Cases
  TEST_CASES: '/testcases',
  TEST_CASE_BY_ID: (id) => `/testcases/${id}`,
  TEST_CASE_STATS: '/testcases/stats/by-status',
  TEST_CASE_STATISTICS: (testRunId) => `/testcases/test-run/${testRunId}/statistics`,
  
  // Artifacts
  ARTIFACTS: '/artifacts',
  ARTIFACT_BY_ID: (id) => `/artifacts/${id}`,
  ARTIFACTS_BY_TEST_RUN: (testRunId) => `/artifacts/test-run/${testRunId}`,
  ARTIFACT_DOWNLOAD: (id) => `/artifacts/${id}/download`,
  ARTIFACT_PREVIEW: (id) => `/artifacts/${id}/preview`,
  ARCHITECTURE_DIAGRAM: (testRunId) => `/artifacts/test-run/${testRunId}/architecture-diagram`,
  TEST_CASES_SUMMARY: (testRunId) => `/artifacts/test-run/${testRunId}/test-cases-summary`,
  
  // Capabilities
  CAPABILITIES: '/capabilities',
  CAPABILITIES_DROPDOWN: '/capabilities/dropdown',
  CAPABILITIES_ACTIVE: '/capabilities/active',
  CAPABILITY_TEST_CASE_COUNTS: '/capabilities/test-case-counts',
  CAPABILITY_BASELINE: (id) => `/capabilities/${id}/baseline`,
  CAPABILITY_ARCHITECTURE_DIAGRAM: (id) => `/capabilities/${id}/architecture-diagram`,
  CAPABILITY_ARCHITECTURE_PREVIEW: (id) => `/capabilities/${id}/architecture-diagram/preview`,
  CAPABILITY_ARCHITECTURE_DOWNLOAD: (id) => `/capabilities/${id}/architecture-diagram/download`,
  CAPABILITY_ARCHITECTURE_DELETE: (id) => `/capabilities/${id}/architecture-diagram`,
  
  // Reports
  GENERATE_REPORT: '/reports/generate',
  GENERATE_PDF_REPORT: (testRunId) => `/reports/generate/jmeter/${testRunId}`,
  GENERATE_WORD_REPORT: (testRunId) => `/reports/generate/jmeter/${testRunId}`,
  GENERATE_WORD_AND_PDF_REPORT: (testRunId) => `/reports/generate/jmeter/${testRunId}/both`,
  GENERATE_CAPABILITY_REPORT: '/reports/generate/capability',
  GENERATE_WORD_TEMPLATE: (capabilityName) => `/reports/generate/word/${capabilityName}`,
  REPORT_BY_ID: (id) => `/reports/${id}`,
  REPORTS: '/reports',
  REPORT_VALIDATE_JMETER: (testRunId) => `/reports/validate/jmeter/${testRunId}`,
  REPORT_DOWNLOAD: (id) => `/reports/download/${id}`,
  REPORT_STATUS: (testRunId) => `/reports/status/${testRunId}`,
  REPORTS_BY_TEST_RUN: (testRunId) => `/reports/test-run/${testRunId}`,
  REPORTS_LIST: '/reports/list',
  DELETE_REPORT: (id) => `/reports/${id}`,

  // Document Data
  DOCUMENT_DATA: (testRunId) => `/test-runs/${testRunId}/document-data`,
  
  // Analytics
  ANALYTICS_SUMMARY: '/analytics/summary',
  ANALYTICS_TRENDS: '/analytics/trends',
};

export default API_BASE_URL;

// Made with Bob
