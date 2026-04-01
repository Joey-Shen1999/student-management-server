-- IELTS language tracking manual status alignment and audit

ALTER TABLE student_ielts_module
    ADD COLUMN IF NOT EXISTS language_tracking_manual_status_updated_by BIGINT;

ALTER TABLE student_ielts_module
    ADD COLUMN IF NOT EXISTS language_tracking_manual_status_updated_at TIMESTAMP;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ck_student_ielts_module_manual_status'
    ) THEN
        ALTER TABLE student_ielts_module
            DROP CONSTRAINT ck_student_ielts_module_manual_status;
    END IF;
END $$;

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
                        OR language_tracking_manual_status IN (
                            'TEACHER_REVIEW_APPROVED',
                            'AUTO_PASS_ALL_SCHOOLS',
                            'AUTO_PASS_PARTIAL_SCHOOLS',
                            'NEEDS_TRACKING'
                        )
                    );
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS student_ielts_manual_status_audit_log (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    student_id BIGINT NOT NULL,
    operator_user_id BIGINT NOT NULL,
    operator_role VARCHAR(20) NOT NULL,
    previous_manual_status VARCHAR(64),
    current_manual_status VARCHAR(64),
    change_source VARCHAR(40) NOT NULL,
    changed_at TIMESTAMP NOT NULL
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_ielts_manual_audit_student'
    ) THEN
        ALTER TABLE student_ielts_manual_status_audit_log
            ADD CONSTRAINT fk_ielts_manual_audit_student
                FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE CASCADE;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_ielts_manual_audit_operator_user'
    ) THEN
        ALTER TABLE student_ielts_manual_status_audit_log
            ADD CONSTRAINT fk_ielts_manual_audit_operator_user
                FOREIGN KEY (operator_user_id) REFERENCES users (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_ielts_manual_audit_student_id
    ON student_ielts_manual_status_audit_log (student_id);

CREATE INDEX IF NOT EXISTS idx_ielts_manual_audit_operator_user_id
    ON student_ielts_manual_status_audit_log (operator_user_id);

CREATE INDEX IF NOT EXISTS idx_ielts_manual_audit_changed_at
    ON student_ielts_manual_status_audit_log (changed_at DESC);
