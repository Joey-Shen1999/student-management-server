-- IELTS language tracking manual override (v1)

ALTER TABLE student_ielts_module
    ADD COLUMN IF NOT EXISTS language_tracking_manual_status VARCHAR(64);

UPDATE student_ielts_module
SET language_tracking_manual_status = NULL
WHERE language_tracking_manual_status IS NOT NULL
  AND language_tracking_manual_status <> 'TEACHER_REVIEW_APPROVED';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ck_student_ielts_module_manual_status'
    ) THEN
        ALTER TABLE student_ielts_module
            ADD CONSTRAINT ck_student_ielts_module_manual_status
                CHECK (
                    language_tracking_manual_status IS NULL
                        OR language_tracking_manual_status = 'TEACHER_REVIEW_APPROVED'
                    );
    END IF;
END $$;
