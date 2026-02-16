-- Migration script to remove test_transactions and test_metrics tables
-- These tables are no longer needed as we now store only aggregated metrics in test_runs

-- Drop foreign key constraints first
ALTER TABLE test_transactions DROP CONSTRAINT IF EXISTS fk_test_transactions_test_run;
ALTER TABLE test_metrics DROP CONSTRAINT IF EXISTS fk_test_metrics_test_run;

-- Drop indexes
DROP INDEX IF EXISTS idx_transaction_test_run;
DROP INDEX IF EXISTS idx_transaction_name;
DROP INDEX IF EXISTS idx_transaction_timestamp;
DROP INDEX IF EXISTS idx_metric_test_run;
DROP INDEX IF EXISTS idx_metric_name;

-- Drop tables
DROP TABLE IF EXISTS test_transactions CASCADE;
DROP TABLE IF EXISTS test_metrics CASCADE;

-- Note: test_runs table is kept as it stores the aggregated metrics
-- The following columns in test_runs contain the summary data:
-- - total_requests
-- - successful_requests
-- - failed_requests
-- - avg_response_time
-- - min_response_time
-- - max_response_time
-- - percentile_90
-- - percentile_95
-- - percentile_99
-- - error_rate
-- - throughput
-- - test_duration_seconds

-- Made with Bob
