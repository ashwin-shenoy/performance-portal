-- ============================================================================
-- Migration: Add instana_config column to capabilities table
-- Purpose: Store capability-level Instana metric query mapping
-- Safe to run multiple times (uses IF NOT EXISTS)
-- ============================================================================

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'capabilities'
          AND column_name = 'instana_config'
    ) THEN
        ALTER TABLE capabilities ADD COLUMN instana_config TEXT DEFAULT '{}';
        RAISE NOTICE 'Column instana_config added to capabilities table';
    ELSE
        RAISE NOTICE 'Column instana_config already exists in capabilities table';
    END IF;
END $$;

COMMENT ON COLUMN capabilities.instana_config IS 'Capability-level Instana namespace/entity/metric mapping configuration';
