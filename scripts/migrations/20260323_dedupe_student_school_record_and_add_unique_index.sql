-- Deduplicate school records per student/type/name/date key.
-- Keep the oldest row, re-point transcript rows, then delete duplicates.
WITH ranked AS (
    SELECT id,
           MIN(id) OVER (
               PARTITION BY student_id,
                            school_type,
                            lower(regexp_replace(btrim(school_name), '\s+', ' ', 'g')),
                            COALESCE(start_time, DATE '0001-01-01'),
                            COALESCE(end_time, DATE '0001-01-01')
               ) AS keep_id,
           ROW_NUMBER() OVER (
               PARTITION BY student_id,
                            school_type,
                            lower(regexp_replace(btrim(school_name), '\s+', ' ', 'g')),
                            COALESCE(start_time, DATE '0001-01-01'),
                            COALESCE(end_time, DATE '0001-01-01')
               ORDER BY id
               ) AS rn
    FROM student_school_record
),
duplicates AS (
    SELECT id AS duplicate_id, keep_id
    FROM ranked
    WHERE rn > 1
)
UPDATE student_school_transcript t
SET school_record_id = d.keep_id
FROM duplicates d
WHERE t.school_record_id = d.duplicate_id;

WITH ranked AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY student_id,
                            school_type,
                            lower(regexp_replace(btrim(school_name), '\s+', ' ', 'g')),
                            COALESCE(start_time, DATE '0001-01-01'),
                            COALESCE(end_time, DATE '0001-01-01')
               ORDER BY id
               ) AS rn
    FROM student_school_record
)
DELETE
FROM student_school_record s
USING ranked r
WHERE s.id = r.id
  AND r.rn > 1;

-- Database-level guard: one record per normalized school key.
CREATE UNIQUE INDEX IF NOT EXISTS uk_student_school_record_unique_school_per_student
    ON student_school_record (
                            student_id,
                            school_type,
                            lower(regexp_replace(btrim(school_name), '\s+', ' ', 'g')),
                            COALESCE(start_time, DATE '0001-01-01'),
                            COALESCE(end_time, DATE '0001-01-01')
        );
