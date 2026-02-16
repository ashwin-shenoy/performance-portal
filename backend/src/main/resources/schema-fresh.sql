-- ============================================================================
-- Fresh Database Schema for durandhar Performance Portal
-- PostgreSQL Database - Matches JPA Entity Definitions
-- ============================================================================

-- Drop existing tables if they exist (for clean install)
DROP TABLE IF EXISTS capability_test_cases CASCADE;
DROP TABLE IF EXISTS test_artifacts CASCADE;
DROP TABLE IF EXISTS reports CASCADE;
DROP TABLE IF EXISTS test_metrics CASCADE;
DROP TABLE IF EXISTS test_transactions CASCADE;
DROP TABLE IF EXISTS test_runs CASCADE;
DROP TABLE IF EXISTS capabilities CASCADE;

-- ============================================================================
-- CORE TABLES
-- ============================================================================

-- Create capabilities table
CREATE TABLE capabilities (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    test_objective TEXT,
    test_scope TEXT,
    environment_details TEXT,
    acceptance_criteria TEXT,
    architecture_diagram_path VARCHAR(500),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create capability_test_cases table
CREATE TABLE capability_test_cases (
    id BIGSERIAL PRIMARY KEY,
    capability_id BIGINT NOT NULL REFERENCES capabilities(id) ON DELETE CASCADE,
    test_case_name VARCHAR(200) NOT NULL,
    description TEXT,
    expected_behavior TEXT,
    priority VARCHAR(20) NOT NULL CHECK (priority IN ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create test_runs table
CREATE TABLE test_runs (
    id BIGSERIAL PRIMARY KEY,
    capability_id BIGINT NOT NULL REFERENCES capabilities(id) ON DELETE CASCADE,
    test_name VARCHAR(200) NOT NULL,
    test_date TIMESTAMP NOT NULL,
    uploaded_by VARCHAR(100) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500),
    file_type VARCHAR(20) NOT NULL CHECK (file_type IN ('JTL')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('UPLOADED', 'PROCESSING', 'COMPLETED', 'FAILED')),
    test_type VARCHAR(50) CHECK (test_type IN ('JMETER')),
    description VARCHAR(1000),
    error_message VARCHAR(2000),
    total_requests BIGINT,
    successful_requests BIGINT,
    failed_requests BIGINT,
    avg_response_time DOUBLE PRECISION,
    min_response_time DOUBLE PRECISION,
    max_response_time DOUBLE PRECISION,
    percentile_90 DOUBLE PRECISION,
    percentile_95 DOUBLE PRECISION,
    percentile_99 DOUBLE PRECISION,
    throughput DOUBLE PRECISION,
    error_rate DOUBLE PRECISION,
    test_duration_seconds BIGINT,
    virtual_users INTEGER,
    build_number VARCHAR(100),
    has_architecture_diagram BOOLEAN DEFAULT FALSE,
    has_test_cases_summary BOOLEAN DEFAULT FALSE,
    notes TEXT,
    capability_specific_data TEXT DEFAULT '{}',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


-- Create reports table
CREATE TABLE reports (
    id BIGSERIAL PRIMARY KEY,
    test_run_id BIGINT NOT NULL REFERENCES test_runs(id) ON DELETE CASCADE,
    report_type VARCHAR(20) NOT NULL CHECK (report_type IN ('TECHNICAL_PDF', 'TECHNICAL_HTML', 'TECHNICAL_WORD', 'EXECUTIVE_WORD', 'EXECUTIVE_PDF', 'CAPABILITY_PDF', 'RAW_DATA_CSV')),
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT,
    generated_by VARCHAR(100) NOT NULL,
    generated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    description VARCHAR(500)
);

-- Create test_artifacts table
CREATE TABLE test_artifacts (
    id BIGSERIAL PRIMARY KEY,
    test_run_id BIGINT NOT NULL REFERENCES test_runs(id) ON DELETE CASCADE,
    artifact_type VARCHAR(50) NOT NULL CHECK (artifact_type IN ('ARCHITECTURE_DIAGRAM', 'TEST_CASES_SUMMARY', 'PERFORMANCE_DATA')),
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_type VARCHAR(50) NOT NULL,
    file_size BIGINT NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB,
    CONSTRAINT unique_artifact_per_type UNIQUE (test_run_id, artifact_type)
);


-- ============================================================================
-- INDEXES
-- ============================================================================

-- Capabilities indexes
CREATE INDEX idx_capability_name ON capabilities(name);

-- Capability test cases indexes
CREATE INDEX idx_ctc_capability ON capability_test_cases(capability_id);

-- Test runs indexes
CREATE INDEX idx_test_run_capability ON test_runs(capability_id);
CREATE INDEX idx_test_run_status ON test_runs(status);
CREATE INDEX idx_test_run_date ON test_runs(test_date);


-- Reports indexes
CREATE INDEX idx_report_test_run ON reports(test_run_id);
CREATE INDEX idx_report_type ON reports(report_type);

-- Test artifacts indexes
CREATE INDEX idx_artifact_test_run ON test_artifacts(test_run_id);
CREATE INDEX idx_artifact_type ON test_artifacts(artifact_type);


-- ============================================================================
-- FUNCTIONS AND TRIGGERS
-- ============================================================================

-- Create function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create triggers for updated_at
CREATE TRIGGER update_capabilities_updated_at BEFORE UPDATE ON capabilities
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_test_runs_updated_at BEFORE UPDATE ON test_runs
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_capability_test_cases_updated_at BEFORE UPDATE ON capability_test_cases
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- DEFAULT DATA
-- ============================================================================

-- Insert default capabilities
INSERT INTO capabilities (name, description) VALUES
    ('test capability', 'Test capability for schema initialization')
ON CONFLICT (name) DO NOTHING;

-- Made with Bob

