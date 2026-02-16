-- Normalize all test types to JMETER

UPDATE test_runs
SET test_type = 'JMETER'
WHERE test_type IS NOT NULL AND UPPER(test_type) <> 'JMETER';

-- Verify the fix
SELECT test_type, COUNT(*) as count 
FROM test_runs 
WHERE test_type IS NOT NULL 
GROUP BY test_type;

-- Made with Bob
