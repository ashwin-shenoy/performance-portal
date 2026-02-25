-- ============================================================================
-- Fresh Database Schema for Performance Portal
-- PostgreSQL Database - Matches JPA Entity Definitions
-- ============================================================================

-- Drop existing tables if they exist (for clean install)
DROP TABLE IF EXISTS capability_test_cases CASCADE;
DROP TABLE IF EXISTS test_artifacts CASCADE;
DROP TABLE IF EXISTS reports CASCADE;
DROP TABLE IF EXISTS test_metrics CASCADE;
DROP TABLE IF EXISTS test_transactions CASCADE;
DROP TABLE IF EXISTS test_runs CASCADE;
DROP TABLE IF EXISTS test_batches CASCADE;
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
    instana_config TEXT DEFAULT '{}',
    architecture_diagram_path VARCHAR(500),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create test_batches table
CREATE TABLE test_batches (
    id BIGSERIAL PRIMARY KEY,
    batch_id VARCHAR(36) NOT NULL UNIQUE,
    capability_id BIGINT NOT NULL REFERENCES capabilities(id) ON DELETE CASCADE,
    batch_name VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED')),
    total_test_cases INTEGER NOT NULL,
    completed_test_cases INTEGER DEFAULT 0,
    failed_test_cases INTEGER DEFAULT 0,
    batch_result VARCHAR(20),
    performance_summary TEXT,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    total_duration_seconds BIGINT,
    created_by VARCHAR(100),
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
    batch_id BIGINT REFERENCES test_batches(id) ON DELETE SET NULL,
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
    test_start_time TIMESTAMP,
    test_end_time TIMESTAMP,
    pod_cpu_avg DOUBLE PRECISION,
    pod_cpu_max DOUBLE PRECISION,
    pod_memory_avg DOUBLE PRECISION,
    pod_memory_max DOUBLE PRECISION,
    jvm_heap_used_percent_avg DOUBLE PRECISION,
    jvm_gc_pause_ms_p95 DOUBLE PRECISION,
    jvm_process_cpu_avg DOUBLE PRECISION,
    virtual_users INTEGER,
    build_number VARCHAR(100),
    has_architecture_diagram BOOLEAN DEFAULT FALSE,
    has_test_cases_summary BOOLEAN DEFAULT FALSE,
    capability_specific_data TEXT DEFAULT '{}',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


-- Create reports table
CREATE TABLE reports (
    id BIGSERIAL PRIMARY KEY,
    test_run_id BIGINT NOT NULL REFERENCES test_runs(id) ON DELETE CASCADE,
    batch_id BIGINT REFERENCES test_batches(id) ON DELETE SET NULL,
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

-- Test batch indexes
CREATE INDEX idx_batch_capability ON test_batches(capability_id);
CREATE INDEX idx_batch_status ON test_batches(status);
CREATE INDEX idx_batch_batch_id ON test_batches(batch_id);

-- Capability test cases indexes
CREATE INDEX idx_ctc_capability ON capability_test_cases(capability_id);

-- Test runs indexes
CREATE INDEX idx_test_run_capability ON test_runs(capability_id);
CREATE INDEX idx_test_run_status ON test_runs(status);
CREATE INDEX idx_test_run_date ON test_runs(test_date);
CREATE INDEX idx_test_run_batch ON test_runs(batch_id);
CREATE INDEX idx_test_run_start_time ON test_runs(test_start_time);
CREATE INDEX idx_test_run_end_time ON test_runs(test_end_time);


-- Reports indexes
CREATE INDEX idx_report_test_run ON reports(test_run_id);
CREATE INDEX idx_report_type ON reports(report_type);
CREATE INDEX idx_report_batch ON reports(batch_id);

-- Test artifacts indexes
CREATE INDEX idx_artifact_test_run ON test_artifacts(test_run_id);
CREATE INDEX idx_artifact_type ON test_artifacts(artifact_type);


-- ============================================================================
-- COLUMN COMMENTS
-- ============================================================================

-- Capabilities column comments
COMMENT ON COLUMN capabilities.instana_config IS 'Capability-level Instana namespace/entity/metric mapping configuration';

-- Test_runs column comments
COMMENT ON COLUMN test_runs.test_start_time IS 'Effective test start timestamp used for telemetry correlation';
COMMENT ON COLUMN test_runs.test_end_time IS 'Effective test end timestamp used for telemetry correlation';
COMMENT ON COLUMN test_runs.pod_cpu_avg IS 'Average pod CPU utilization percentage over test window';
COMMENT ON COLUMN test_runs.pod_cpu_max IS 'Maximum pod CPU utilization percentage over test window';
COMMENT ON COLUMN test_runs.pod_memory_avg IS 'Average pod memory utilization percentage over test window';
COMMENT ON COLUMN test_runs.pod_memory_max IS 'Maximum pod memory utilization percentage over test window';
COMMENT ON COLUMN test_runs.jvm_heap_used_percent_avg IS 'Average JVM heap used percentage over test window';
COMMENT ON COLUMN test_runs.jvm_gc_pause_ms_p95 IS 'P95 JVM GC pause duration in milliseconds over test window';
COMMENT ON COLUMN test_runs.jvm_process_cpu_avg IS 'Average JVM process CPU utilization percentage over test window';


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

CREATE TRIGGER update_test_batches_updated_at BEFORE UPDATE ON test_batches
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

