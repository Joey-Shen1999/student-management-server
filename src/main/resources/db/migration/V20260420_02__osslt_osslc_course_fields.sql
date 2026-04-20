-- OSSLT: add OSSLC course details for NEEDS_TRACKING workflow

ALTER TABLE student_osslt_module
    ADD COLUMN IF NOT EXISTS osslc_course_status VARCHAR(32);

ALTER TABLE student_osslt_module
    ADD COLUMN IF NOT EXISTS osslc_course_location VARCHAR(255);

UPDATE student_osslt_module
SET osslc_course_status = NULL
WHERE osslc_course_status IS NOT NULL
  AND osslc_course_status NOT IN ('NOT_PLANNING', 'IN_PROGRESS', 'NOT_ENROLLED');

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ck_student_osslt_module_osslc_course_status'
    ) THEN
        ALTER TABLE student_osslt_module
            ADD CONSTRAINT ck_student_osslt_module_osslc_course_status
                CHECK (
                    osslc_course_status IS NULL
                        OR osslc_course_status IN ('NOT_PLANNING', 'IN_PROGRESS', 'NOT_ENROLLED')
                    );
    END IF;
END $$;
