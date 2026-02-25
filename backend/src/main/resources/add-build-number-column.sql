-- ============================================================================
-- Migration: Add build_number column to test_runs table
-- Purpose: Store build numbers separately from description field
-- Safe to run multiple times (uses IF NOT EXISTS)
-- ============================================================================

-- Step 1: Add the column (nullable to allow existing records)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'test_runs'
        AND column_name = 'build_number'
    ) THEN
        ALTER TABLE test_runs ADD COLUMN build_number VARCHAR(100);
        RAISE NOTICE 'Column build_number added to test_runs table';
    ELSE
        RAISE NOTICE 'Column build_number already exists in test_runs table';
    END IF;
END $$;

-- Step 2: Create an index for faster queries by build number
CREATE INDEX IF NOT EXISTS idx_test_run_build_number ON test_runs(build_number);

-- Step 3: Extract build numbers from existing descriptions (OPTIONAL - commented out by default)
-- Uncomment the following lines if you want to migrate existing data
-- This will parse descriptions like "Build Number: 12345" and populate the new column
-- WARNING: This will modify existing data. Review before running.

/*
UPDATE test_runs
SET build_number = SUBSTRING(description FROM 'Build Number: ([^\n]+)')
WHERE description LIKE '%Build Number:%'
  AND build_number IS NULL;
*/

-- Step 4: Add comment to the column for documentation
COMMENT ON COLUMN test_runs.build_number IS 'Build or version number associated with this test run';

-- Verification query (run this after migration to verify)
-- SELECT COUNT(*) as total_rows,
--        COUNT(build_number) as rows_with_build_number,
--        COUNT(*) - COUNT(build_number) as rows_without_build_number
-- FROM test_runs;

-- Made with Bob
