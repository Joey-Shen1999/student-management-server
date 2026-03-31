-- IELTS / English requirement tracking module (v1)

CREATE TABLE IF NOT EXISTS student_ielts_module (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    student_id BIGINT NOT NULL,
    has_taken_ielts_academic BOOLEAN NOT NULL DEFAULT FALSE,
    preparation_intent VARCHAR(20) NOT NULL DEFAULT 'UNSET'
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_student_ielts_module_student_id'
    ) THEN
        ALTER TABLE student_ielts_module
            ADD CONSTRAINT uk_student_ielts_module_student_id UNIQUE (student_id);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_student_ielts_module_student'
    ) THEN
        ALTER TABLE student_ielts_module
            ADD CONSTRAINT fk_student_ielts_module_student
                FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE CASCADE;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_student_ielts_module_student_id
    ON student_ielts_module (student_id);

CREATE TABLE IF NOT EXISTS student_ielts_record (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    ielts_module_id BIGINT NOT NULL,
    record_id VARCHAR(64) NOT NULL,
    test_date DATE NOT NULL,
    listening DOUBLE PRECISION NOT NULL,
    reading DOUBLE PRECISION NOT NULL,
    writing DOUBLE PRECISION NOT NULL,
    speaking DOUBLE PRECISION NOT NULL
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_student_ielts_record_module_record_id'
    ) THEN
        ALTER TABLE student_ielts_record
            ADD CONSTRAINT uk_student_ielts_record_module_record_id UNIQUE (ielts_module_id, record_id);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_student_ielts_record_module'
    ) THEN
        ALTER TABLE student_ielts_record
            ADD CONSTRAINT fk_student_ielts_record_module
                FOREIGN KEY (ielts_module_id) REFERENCES student_ielts_module (id) ON DELETE CASCADE;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_student_ielts_record_module_id
    ON student_ielts_record (ielts_module_id);

CREATE INDEX IF NOT EXISTS idx_student_ielts_record_module_test_date
    ON student_ielts_record (ielts_module_id, test_date DESC);
