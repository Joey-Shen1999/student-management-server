-- Language-score tracking namespace split + OSSLT tracking module (v1)

-- 1) IELTS language tracking canonical columns
ALTER TABLE student_ielts_module
    ADD COLUMN IF NOT EXISTS language_score_tracking_manual_status VARCHAR(64);

ALTER TABLE student_ielts_module
    ADD COLUMN IF NOT EXISTS language_score_tracking_status VARCHAR(64);

UPDATE student_ielts_module
SET language_score_tracking_manual_status = language_tracking_manual_status
WHERE language_score_tracking_manual_status IS NULL
  AND language_tracking_manual_status IS NOT NULL;

UPDATE student_ielts_module
SET language_score_tracking_status = language_tracking_status
WHERE language_score_tracking_status IS NULL
  AND language_tracking_status IS NOT NULL;

UPDATE student_ielts_module
SET language_score_tracking_status = CASE
    WHEN language_score_tracking_manual_status IS NOT NULL THEN language_score_tracking_manual_status
    WHEN tracking_status = 'GREEN_STRICT_PASS' THEN 'AUTO_PASS_ALL_SCHOOLS'
    WHEN tracking_status = 'GREEN_COMMON_PASS_WITH_WARNING' THEN 'AUTO_PASS_PARTIAL_SCHOOLS'
    ELSE 'NEEDS_TRACKING'
END
WHERE language_score_tracking_status IS NULL
   OR language_score_tracking_status NOT IN (
       'TEACHER_REVIEW_APPROVED',
       'AUTO_PASS_ALL_SCHOOLS',
       'AUTO_PASS_PARTIAL_SCHOOLS',
       'NEEDS_TRACKING'
   );

-- Keep legacy columns readable during transition.
UPDATE student_ielts_module
SET language_tracking_manual_status = language_score_tracking_manual_status
WHERE language_tracking_manual_status IS NULL
  AND language_score_tracking_manual_status IS NOT NULL;

UPDATE student_ielts_module
SET language_tracking_status = language_score_tracking_status
WHERE language_tracking_status IS NULL
  AND language_score_tracking_status IS NOT NULL;

ALTER TABLE student_ielts_module
    ALTER COLUMN language_score_tracking_status SET DEFAULT 'NEEDS_TRACKING';

ALTER TABLE student_ielts_module
    ALTER COLUMN language_score_tracking_status SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ck_student_ielts_module_language_score_tracking_manual_status'
    ) THEN
        ALTER TABLE student_ielts_module
            ADD CONSTRAINT ck_student_ielts_module_language_score_tracking_manual_status
                CHECK (
                    language_score_tracking_manual_status IS NULL
                        OR language_score_tracking_manual_status IN (
                            'TEACHER_REVIEW_APPROVED',
                            'AUTO_PASS_ALL_SCHOOLS',
                            'AUTO_PASS_PARTIAL_SCHOOLS',
                            'NEEDS_TRACKING'
                        )
                    );
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ck_student_ielts_module_language_score_tracking_status'
    ) THEN
        ALTER TABLE student_ielts_module
            ADD CONSTRAINT ck_student_ielts_module_language_score_tracking_status
                CHECK (
                    language_score_tracking_status IN (
                        'TEACHER_REVIEW_APPROVED',
                        'AUTO_PASS_ALL_SCHOOLS',
                        'AUTO_PASS_PARTIAL_SCHOOLS',
                        'NEEDS_TRACKING'
                    )
                );
    END IF;
END $$;

-- 2) OSSLT module
CREATE TABLE IF NOT EXISTS student_osslt_module (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    student_id BIGINT NOT NULL,
    latest_osslt_result VARCHAR(16) NOT NULL DEFAULT 'UNKNOWN',
    latest_osslt_date DATE,
    osslt_tracking_manual_status VARCHAR(32),
    osslt_tracking_status VARCHAR(32) NOT NULL DEFAULT 'WAITING_UPDATE',
    osslt_teacher_note TEXT,
    osslt_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE student_osslt_module
    ADD COLUMN IF NOT EXISTS latest_osslt_result VARCHAR(16);

ALTER TABLE student_osslt_module
    ADD COLUMN IF NOT EXISTS latest_osslt_date DATE;

ALTER TABLE student_osslt_module
    ADD COLUMN IF NOT EXISTS osslt_tracking_manual_status VARCHAR(32);

ALTER TABLE student_osslt_module
    ADD COLUMN IF NOT EXISTS osslt_tracking_status VARCHAR(32);

ALTER TABLE student_osslt_module
    ADD COLUMN IF NOT EXISTS osslt_teacher_note TEXT;

ALTER TABLE student_osslt_module
    ADD COLUMN IF NOT EXISTS osslt_updated_at TIMESTAMP;

UPDATE student_osslt_module
SET latest_osslt_result = 'UNKNOWN'
WHERE latest_osslt_result IS NULL
   OR latest_osslt_result NOT IN ('PASS', 'FAIL', 'UNKNOWN');

UPDATE student_osslt_module
SET osslt_tracking_manual_status = NULL
WHERE osslt_tracking_manual_status IS NOT NULL
  AND osslt_tracking_manual_status NOT IN ('WAITING_UPDATE', 'NEEDS_TRACKING', 'PASSED');

UPDATE student_osslt_module
SET osslt_tracking_status = CASE
    WHEN osslt_tracking_manual_status IS NOT NULL THEN osslt_tracking_manual_status
    WHEN latest_osslt_result = 'PASS' THEN 'PASSED'
    ELSE 'WAITING_UPDATE'
END
WHERE osslt_tracking_status IS NULL
   OR osslt_tracking_status NOT IN ('WAITING_UPDATE', 'NEEDS_TRACKING', 'PASSED');

UPDATE student_osslt_module
SET osslt_updated_at = COALESCE(osslt_updated_at, updated_at, created_at, CURRENT_TIMESTAMP)
WHERE osslt_updated_at IS NULL;

ALTER TABLE student_osslt_module
    ALTER COLUMN latest_osslt_result SET DEFAULT 'UNKNOWN';

ALTER TABLE student_osslt_module
    ALTER COLUMN osslt_tracking_status SET DEFAULT 'WAITING_UPDATE';

ALTER TABLE student_osslt_module
    ALTER COLUMN osslt_updated_at SET DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE student_osslt_module
    ALTER COLUMN latest_osslt_result SET NOT NULL;

ALTER TABLE student_osslt_module
    ALTER COLUMN osslt_tracking_status SET NOT NULL;

ALTER TABLE student_osslt_module
    ALTER COLUMN osslt_updated_at SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_student_osslt_module_student_id'
    ) THEN
        ALTER TABLE student_osslt_module
            ADD CONSTRAINT uk_student_osslt_module_student_id UNIQUE (student_id);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_student_osslt_module_student'
    ) THEN
        ALTER TABLE student_osslt_module
            ADD CONSTRAINT fk_student_osslt_module_student
                FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE CASCADE;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ck_student_osslt_module_latest_osslt_result'
    ) THEN
        ALTER TABLE student_osslt_module
            ADD CONSTRAINT ck_student_osslt_module_latest_osslt_result
                CHECK (latest_osslt_result IN ('PASS', 'FAIL', 'UNKNOWN'));
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ck_student_osslt_module_manual_status'
    ) THEN
        ALTER TABLE student_osslt_module
            ADD CONSTRAINT ck_student_osslt_module_manual_status
                CHECK (
                    osslt_tracking_manual_status IS NULL
                        OR osslt_tracking_manual_status IN ('WAITING_UPDATE', 'NEEDS_TRACKING', 'PASSED')
                    );
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ck_student_osslt_module_tracking_status'
    ) THEN
        ALTER TABLE student_osslt_module
            ADD CONSTRAINT ck_student_osslt_module_tracking_status
                CHECK (osslt_tracking_status IN ('WAITING_UPDATE', 'NEEDS_TRACKING', 'PASSED'));
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_student_osslt_module_student_id
    ON student_osslt_module (student_id);

CREATE INDEX IF NOT EXISTS idx_student_osslt_module_tracking_status
    ON student_osslt_module (osslt_tracking_status);
