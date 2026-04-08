-- Canonical volunteer tracking tables for teacher/student volunteer tracking APIs.

DO $$
BEGIN
    IF to_regclass('public.students') IS NULL THEN
        RETURN;
    END IF;

    IF to_regclass('public.student_volunteer_tracking') IS NULL THEN
        CREATE TABLE student_volunteer_tracking (
            id BIGSERIAL PRIMARY KEY,
            created_at TIMESTAMP NOT NULL DEFAULT NOW(),
            updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
            student_id BIGINT NOT NULL,
            total_hours NUMERIC(12, 2) NOT NULL,
            note VARCHAR(2000),
            updated_by_teacher_id BIGINT
        );
    END IF;

    ALTER TABLE student_volunteer_tracking
        ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT NOW();
    ALTER TABLE student_volunteer_tracking
        ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT NOW();
    ALTER TABLE student_volunteer_tracking
        ADD COLUMN IF NOT EXISTS student_id BIGINT;
    ALTER TABLE student_volunteer_tracking
        ADD COLUMN IF NOT EXISTS total_hours NUMERIC(12, 2);
    ALTER TABLE student_volunteer_tracking
        ADD COLUMN IF NOT EXISTS note VARCHAR(2000);
    ALTER TABLE student_volunteer_tracking
        ADD COLUMN IF NOT EXISTS updated_by_teacher_id BIGINT;

    UPDATE student_volunteer_tracking
    SET created_at = NOW()
    WHERE created_at IS NULL;

    UPDATE student_volunteer_tracking
    SET updated_at = NOW()
    WHERE updated_at IS NULL;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_student_volunteer_tracking_student'
    ) THEN
        ALTER TABLE student_volunteer_tracking
            ADD CONSTRAINT fk_student_volunteer_tracking_student
                FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_student_volunteer_tracking_updated_by_teacher'
    ) THEN
        ALTER TABLE student_volunteer_tracking
            ADD CONSTRAINT fk_student_volunteer_tracking_updated_by_teacher
                FOREIGN KEY (updated_by_teacher_id) REFERENCES teachers(id) ON DELETE SET NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_student_volunteer_tracking_student_id'
    ) THEN
        ALTER TABLE student_volunteer_tracking
            ADD CONSTRAINT uk_student_volunteer_tracking_student_id
                UNIQUE (student_id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_student_volunteer_tracking_student_id
    ON student_volunteer_tracking (student_id);
CREATE INDEX IF NOT EXISTS idx_student_volunteer_tracking_updated_by_teacher_id
    ON student_volunteer_tracking (updated_by_teacher_id);

DO $$
BEGIN
    IF to_regclass('public.student_volunteer_tracking') IS NULL THEN
        RETURN;
    END IF;

    IF to_regclass('public.student_volunteer_tracking_task') IS NULL THEN
        CREATE TABLE student_volunteer_tracking_task (
            id BIGSERIAL PRIMARY KEY,
            created_at TIMESTAMP NOT NULL DEFAULT NOW(),
            updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
            tracking_id BIGINT NOT NULL,
            task_name VARCHAR(200) NOT NULL,
            description VARCHAR(2000) NOT NULL,
            duration_hours NUMERIC(12, 2) NOT NULL,
            start_date DATE NOT NULL,
            end_date DATE NOT NULL,
            verifier_contact VARCHAR(255) NOT NULL
        );
    END IF;

    ALTER TABLE student_volunteer_tracking_task
        ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT NOW();
    ALTER TABLE student_volunteer_tracking_task
        ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT NOW();
    ALTER TABLE student_volunteer_tracking_task
        ADD COLUMN IF NOT EXISTS tracking_id BIGINT;
    ALTER TABLE student_volunteer_tracking_task
        ADD COLUMN IF NOT EXISTS task_name VARCHAR(200);
    ALTER TABLE student_volunteer_tracking_task
        ADD COLUMN IF NOT EXISTS description VARCHAR(2000);
    ALTER TABLE student_volunteer_tracking_task
        ADD COLUMN IF NOT EXISTS duration_hours NUMERIC(12, 2);
    ALTER TABLE student_volunteer_tracking_task
        ADD COLUMN IF NOT EXISTS start_date DATE;
    ALTER TABLE student_volunteer_tracking_task
        ADD COLUMN IF NOT EXISTS end_date DATE;
    ALTER TABLE student_volunteer_tracking_task
        ADD COLUMN IF NOT EXISTS verifier_contact VARCHAR(255);

    UPDATE student_volunteer_tracking_task
    SET created_at = NOW()
    WHERE created_at IS NULL;

    UPDATE student_volunteer_tracking_task
    SET updated_at = NOW()
    WHERE updated_at IS NULL;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_student_volunteer_tracking_task_tracking'
    ) THEN
        ALTER TABLE student_volunteer_tracking_task
            ADD CONSTRAINT fk_student_volunteer_tracking_task_tracking
                FOREIGN KEY (tracking_id) REFERENCES student_volunteer_tracking(id) ON DELETE CASCADE;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_student_volunteer_tracking_task_duration_positive'
    ) THEN
        ALTER TABLE student_volunteer_tracking_task
            ADD CONSTRAINT chk_student_volunteer_tracking_task_duration_positive
                CHECK (duration_hours > 0);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_student_volunteer_tracking_task_date_range'
    ) THEN
        ALTER TABLE student_volunteer_tracking_task
            ADD CONSTRAINT chk_student_volunteer_tracking_task_date_range
                CHECK (end_date >= start_date);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_student_volunteer_tracking_task_tracking_id
    ON student_volunteer_tracking_task (tracking_id);
