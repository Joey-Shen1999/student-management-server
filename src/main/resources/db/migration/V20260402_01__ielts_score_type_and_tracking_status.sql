-- IELTS language score type and derived tracking statuses

ALTER TABLE student_ielts_module
    ADD COLUMN IF NOT EXISTS language_score_type VARCHAR(20);

ALTER TABLE student_ielts_module
    ADD COLUMN IF NOT EXISTS tracking_status VARCHAR(64);

ALTER TABLE student_ielts_module
    ADD COLUMN IF NOT EXISTS language_tracking_status VARCHAR(64);

UPDATE student_ielts_module
SET language_score_type = 'IELTS'
WHERE language_score_type IS NULL
   OR language_score_type NOT IN ('IELTS', 'TOEFL');

UPDATE student_ielts_module
SET tracking_status = 'YELLOW_NEEDS_PREPARATION'
WHERE tracking_status IS NULL
   OR tracking_status NOT IN (
       'GREEN_STRICT_PASS',
       'GREEN_COMMON_PASS_WITH_WARNING',
       'YELLOW_NEEDS_PREPARATION'
   );

UPDATE student_ielts_module
SET language_tracking_status = CASE
    WHEN language_tracking_manual_status IS NOT NULL THEN language_tracking_manual_status
    WHEN tracking_status = 'GREEN_STRICT_PASS' THEN 'AUTO_PASS_ALL_SCHOOLS'
    WHEN tracking_status = 'GREEN_COMMON_PASS_WITH_WARNING' THEN 'AUTO_PASS_PARTIAL_SCHOOLS'
    ELSE 'NEEDS_TRACKING'
END
WHERE language_tracking_status IS NULL
   OR language_tracking_status NOT IN (
       'TEACHER_REVIEW_APPROVED',
       'AUTO_PASS_ALL_SCHOOLS',
       'AUTO_PASS_PARTIAL_SCHOOLS',
       'NEEDS_TRACKING'
   );

ALTER TABLE student_ielts_module
    ALTER COLUMN language_score_type SET DEFAULT 'IELTS';

ALTER TABLE student_ielts_module
    ALTER COLUMN tracking_status SET DEFAULT 'YELLOW_NEEDS_PREPARATION';

ALTER TABLE student_ielts_module
    ALTER COLUMN language_tracking_status SET DEFAULT 'NEEDS_TRACKING';

ALTER TABLE student_ielts_module
    ALTER COLUMN language_score_type SET NOT NULL;

ALTER TABLE student_ielts_module
    ALTER COLUMN tracking_status SET NOT NULL;

ALTER TABLE student_ielts_module
    ALTER COLUMN language_tracking_status SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ck_student_ielts_module_language_score_type'
    ) THEN
        ALTER TABLE student_ielts_module
            ADD CONSTRAINT ck_student_ielts_module_language_score_type
                CHECK (language_score_type IN ('IELTS', 'TOEFL'));
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ck_student_ielts_module_tracking_status'
    ) THEN
        ALTER TABLE student_ielts_module
            ADD CONSTRAINT ck_student_ielts_module_tracking_status
                CHECK (
                    tracking_status IN (
                        'GREEN_STRICT_PASS',
                        'GREEN_COMMON_PASS_WITH_WARNING',
                        'YELLOW_NEEDS_PREPARATION'
                    )
                );
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ck_student_ielts_module_language_tracking_status'
    ) THEN
        ALTER TABLE student_ielts_module
            ADD CONSTRAINT ck_student_ielts_module_language_tracking_status
                CHECK (
                    language_tracking_status IN (
                        'TEACHER_REVIEW_APPROVED',
                        'AUTO_PASS_ALL_SCHOOLS',
                        'AUTO_PASS_PARTIAL_SCHOOLS',
                        'NEEDS_TRACKING'
                    )
                );
    END IF;
END $$;
