-- ============================================================================
-- Migration: Add test window and Instana summary columns to test_runs table
-- Purpose: Persist start/end test times and summarized infra/JVM metrics per run
-- Safe to run multiple times (uses IF NOT EXISTS)
-- ============================================================================

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'test_runs'
          AND column_name = 'test_start_time'
    ) THEN
        ALTER TABLE test_runs ADD COLUMN test_start_time TIMESTAMP;
        RAISE NOTICE 'Column test_start_time added to test_runs table';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'test_runs'
          AND column_name = 'test_end_time'
    ) THEN
        ALTER TABLE test_runs ADD COLUMN test_end_time TIMESTAMP;
        RAISE NOTICE 'Column test_end_time added to test_runs table';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'test_runs'
          AND column_name = 'pod_cpu_avg'
    ) THEN
        ALTER TABLE test_runs ADD COLUMN pod_cpu_avg DOUBLE PRECISION;
        RAISE NOTICE 'Column pod_cpu_avg added to test_runs table';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'test_runs'
          AND column_name = 'pod_cpu_max'
    ) THEN
        ALTER TABLE test_runs ADD COLUMN pod_cpu_max DOUBLE PRECISION;
        RAISE NOTICE 'Column pod_cpu_max added to test_runs table';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'test_runs'
          AND column_name = 'pod_memory_avg'
    ) THEN
        ALTER TABLE test_runs ADD COLUMN pod_memory_avg DOUBLE PRECISION;
        RAISE NOTICE 'Column pod_memory_avg added to test_runs table';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'test_runs'
          AND column_name = 'pod_memory_max'
    ) THEN
        ALTER TABLE test_runs ADD COLUMN pod_memory_max DOUBLE PRECISION;
        RAISE NOTICE 'Column pod_memory_max added to test_runs table';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'test_runs'
          AND column_name = 'jvm_heap_used_percent_avg'
    ) THEN
        ALTER TABLE test_runs ADD COLUMN jvm_heap_used_percent_avg DOUBLE PRECISION;
        RAISE NOTICE 'Column jvm_heap_used_percent_avg added to test_runs table';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'test_runs'
          AND column_name = 'jvm_gc_pause_ms_p95'
    ) THEN
        ALTER TABLE test_runs ADD COLUMN jvm_gc_pause_ms_p95 DOUBLE PRECISION;
        RAISE NOTICE 'Column jvm_gc_pause_ms_p95 added to test_runs table';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'test_runs'
          AND column_name = 'jvm_process_cpu_avg'
    ) THEN
        ALTER TABLE test_runs ADD COLUMN jvm_process_cpu_avg DOUBLE PRECISION;
        RAISE NOTICE 'Column jvm_process_cpu_avg added to test_runs table';
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_test_run_start_time ON test_runs(test_start_time);
CREATE INDEX IF NOT EXISTS idx_test_run_end_time ON test_runs(test_end_time);

COMMENT ON COLUMN test_runs.test_start_time IS 'Effective test start timestamp used for telemetry correlation';
COMMENT ON COLUMN test_runs.test_end_time IS 'Effective test end timestamp used for telemetry correlation';
COMMENT ON COLUMN test_runs.pod_cpu_avg IS 'Average pod CPU utilization percentage over test window';
COMMENT ON COLUMN test_runs.pod_cpu_max IS 'Maximum pod CPU utilization percentage over test window';
COMMENT ON COLUMN test_runs.pod_memory_avg IS 'Average pod memory utilization percentage over test window';
COMMENT ON COLUMN test_runs.pod_memory_max IS 'Maximum pod memory utilization percentage over test window';
COMMENT ON COLUMN test_runs.jvm_heap_used_percent_avg IS 'Average JVM heap used percentage over test window';
COMMENT ON COLUMN test_runs.jvm_gc_pause_ms_p95 IS 'P95 JVM GC pause duration in milliseconds over test window';
COMMENT ON COLUMN test_runs.jvm_process_cpu_avg IS 'Average JVM process CPU utilization percentage over test window';
