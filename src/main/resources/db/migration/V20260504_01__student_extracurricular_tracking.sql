-- Extracurricular activity tracking for admissions-related student profiles.

DO $$
BEGIN
    IF to_regclass('public.students') IS NULL THEN
        RETURN;
    END IF;

    IF to_regclass('public.student_extracurricular_tracking') IS NULL THEN
        CREATE TABLE student_extracurricular_tracking (
            id BIGSERIAL PRIMARY KEY,
            created_at TIMESTAMP NOT NULL DEFAULT NOW(),
            updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
            student_id BIGINT NOT NULL,
            note VARCHAR(2000),
            updated_by_teacher_id BIGINT
        );
    END IF;

    ALTER TABLE student_extracurricular_tracking
        ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT NOW();
    ALTER TABLE student_extracurricular_tracking
        ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT NOW();
    ALTER TABLE student_extracurricular_tracking
        ADD COLUMN IF NOT EXISTS student_id BIGINT;
    ALTER TABLE student_extracurricular_tracking
        ADD COLUMN IF NOT EXISTS note VARCHAR(2000);
    ALTER TABLE student_extracurricular_tracking
        ADD COLUMN IF NOT EXISTS updated_by_teacher_id BIGINT;

    UPDATE student_extracurricular_tracking
    SET created_at = NOW()
    WHERE created_at IS NULL;

    UPDATE student_extracurricular_tracking
    SET updated_at = NOW()
    WHERE updated_at IS NULL;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_student_extracurricular_tracking_student'
    ) THEN
        ALTER TABLE student_extracurricular_tracking
            ADD CONSTRAINT fk_student_extracurricular_tracking_student
                FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_student_extracurricular_tracking_updated_by_teacher'
    ) THEN
        ALTER TABLE student_extracurricular_tracking
            ADD CONSTRAINT fk_student_extracurricular_tracking_updated_by_teacher
                FOREIGN KEY (updated_by_teacher_id) REFERENCES teachers(id) ON DELETE SET NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_student_extracurricular_tracking_student_id'
    ) THEN
        ALTER TABLE student_extracurricular_tracking
            ADD CONSTRAINT uk_student_extracurricular_tracking_student_id
                UNIQUE (student_id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_student_extracurricular_tracking_student_id
    ON student_extracurricular_tracking (student_id);
CREATE INDEX IF NOT EXISTS idx_student_extracurricular_tracking_updated_by_teacher_id
    ON student_extracurricular_tracking (updated_by_teacher_id);

DO $$
BEGIN
    IF to_regclass('public.student_extracurricular_tracking') IS NULL THEN
        RETURN;
    END IF;

    IF to_regclass('public.student_extracurricular_activity') IS NULL THEN
        CREATE TABLE student_extracurricular_activity (
            id BIGSERIAL PRIMARY KEY,
            created_at TIMESTAMP NOT NULL DEFAULT NOW(),
            updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
            tracking_id BIGINT NOT NULL,
            activity_type VARCHAR(40) NOT NULL,
            activity_name VARCHAR(200) NOT NULL,
            organization VARCHAR(200),
            activity_role VARCHAR(120),
            activity_level VARCHAR(40),
            award_or_result VARCHAR(255),
            competition_category VARCHAR(120),
            activity_date DATE,
            start_date DATE,
            end_date DATE,
            description VARCHAR(2000),
            admission_relevance VARCHAR(2000),
            proof_contact VARCHAR(255),
            proof_url VARCHAR(500)
        );
    END IF;

    ALTER TABLE student_extracurricular_activity
        ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT NOW();
    ALTER TABLE student_extracurricular_activity
        ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT NOW();
    ALTER TABLE student_extracurricular_activity
        ADD COLUMN IF NOT EXISTS tracking_id BIGINT;
    ALTER TABLE student_extracurricular_activity
        ADD COLUMN IF NOT EXISTS activity_type VARCHAR(40);
    ALTER TABLE student_extracurricular_activity
        ADD COLUMN IF NOT EXISTS activity_name VARCHAR(200);
    ALTER TABLE student_extracurricular_activity
        ADD COLUMN IF NOT EXISTS organization VARCHAR(200);
    ALTER TABLE student_extracurricular_activity
        ADD COLUMN IF NOT EXISTS activity_role VARCHAR(120);
    ALTER TABLE student_extracurricular_activity
        ADD COLUMN IF NOT EXISTS activity_level VARCHAR(40);
    ALTER TABLE student_extracurricular_activity
        ADD COLUMN IF NOT EXISTS award_or_result VARCHAR(255);
    ALTER TABLE student_extracurricular_activity
        ADD COLUMN IF NOT EXISTS competition_category VARCHAR(120);
    ALTER TABLE student_extracurricular_activity
        ADD COLUMN IF NOT EXISTS activity_date DATE;
    ALTER TABLE student_extracurricular_activity
        ADD COLUMN IF NOT EXISTS start_date DATE;
    ALTER TABLE student_extracurricular_activity
        ADD COLUMN IF NOT EXISTS end_date DATE;
    ALTER TABLE student_extracurricular_activity
        ADD COLUMN IF NOT EXISTS description VARCHAR(2000);
    ALTER TABLE student_extracurricular_activity
        ADD COLUMN IF NOT EXISTS admission_relevance VARCHAR(2000);
    ALTER TABLE student_extracurricular_activity
        ADD COLUMN IF NOT EXISTS proof_contact VARCHAR(255);
    ALTER TABLE student_extracurricular_activity
        ADD COLUMN IF NOT EXISTS proof_url VARCHAR(500);

    UPDATE student_extracurricular_activity
    SET created_at = NOW()
    WHERE created_at IS NULL;

    UPDATE student_extracurricular_activity
    SET updated_at = NOW()
    WHERE updated_at IS NULL;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_student_extracurricular_activity_tracking'
    ) THEN
        ALTER TABLE student_extracurricular_activity
            ADD CONSTRAINT fk_student_extracurricular_activity_tracking
                FOREIGN KEY (tracking_id) REFERENCES student_extracurricular_tracking(id) ON DELETE CASCADE;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_student_extracurricular_activity_type'
    ) THEN
        ALTER TABLE student_extracurricular_activity
            ADD CONSTRAINT chk_student_extracurricular_activity_type
                CHECK (activity_type IN (
                    'COMPETITION',
                    'PUBLIC_EVENT',
                    'SUMMER_CAMP',
                    'CLUB',
                    'RESEARCH',
                    'INTERNSHIP',
                    'CERTIFICATE',
                    'OTHER'
                ));
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_student_extracurricular_activity_dates'
    ) THEN
        ALTER TABLE student_extracurricular_activity
            ADD CONSTRAINT chk_student_extracurricular_activity_dates
                CHECK (
                    (activity_type = 'COMPETITION' AND activity_date IS NOT NULL)
                    OR
                    (activity_type <> 'COMPETITION' AND start_date IS NOT NULL AND end_date IS NOT NULL AND end_date >= start_date)
                );
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_student_extracurricular_activity_tracking_id
    ON student_extracurricular_activity (tracking_id);
CREATE INDEX IF NOT EXISTS idx_student_extracurricular_activity_type
    ON student_extracurricular_activity (activity_type);
CREATE INDEX IF NOT EXISTS idx_student_extracurricular_activity_date
    ON student_extracurricular_activity (activity_date, start_date, end_date);
