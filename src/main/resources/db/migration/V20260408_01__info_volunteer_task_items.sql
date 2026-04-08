-- Structured volunteer task details for INFO/VOLUNTEER notices

DO $$
BEGIN
    IF to_regclass('public.info_tasks') IS NULL THEN
        RETURN;
    END IF;

    IF to_regclass('public.info_volunteer_task_items') IS NULL THEN
        CREATE TABLE info_volunteer_task_items (
            id BIGSERIAL PRIMARY KEY,
            created_at TIMESTAMP NOT NULL DEFAULT NOW(),
            updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
            info_id BIGINT NOT NULL,
            task_name VARCHAR(200) NOT NULL,
            description VARCHAR(2000) NOT NULL,
            duration_hours NUMERIC(12, 2) NOT NULL,
            start_date DATE NOT NULL,
            end_date DATE NOT NULL,
            verifier_contact VARCHAR(255) NOT NULL
        );
    END IF;

    ALTER TABLE info_volunteer_task_items
        ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT NOW();
    ALTER TABLE info_volunteer_task_items
        ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT NOW();
    ALTER TABLE info_volunteer_task_items
        ADD COLUMN IF NOT EXISTS info_id BIGINT;
    ALTER TABLE info_volunteer_task_items
        ADD COLUMN IF NOT EXISTS task_name VARCHAR(200);
    ALTER TABLE info_volunteer_task_items
        ADD COLUMN IF NOT EXISTS description VARCHAR(2000);
    ALTER TABLE info_volunteer_task_items
        ADD COLUMN IF NOT EXISTS duration_hours NUMERIC(12, 2);
    ALTER TABLE info_volunteer_task_items
        ADD COLUMN IF NOT EXISTS start_date DATE;
    ALTER TABLE info_volunteer_task_items
        ADD COLUMN IF NOT EXISTS end_date DATE;
    ALTER TABLE info_volunteer_task_items
        ADD COLUMN IF NOT EXISTS verifier_contact VARCHAR(255);

    UPDATE info_volunteer_task_items
    SET created_at = NOW()
    WHERE created_at IS NULL;

    UPDATE info_volunteer_task_items
    SET updated_at = NOW()
    WHERE updated_at IS NULL;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_info_volunteer_task_items_info'
    ) THEN
        ALTER TABLE info_volunteer_task_items
            ADD CONSTRAINT fk_info_volunteer_task_items_info
                FOREIGN KEY (info_id) REFERENCES info_tasks(id) ON DELETE CASCADE;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_info_volunteer_task_items_duration_positive'
    ) THEN
        ALTER TABLE info_volunteer_task_items
            ADD CONSTRAINT chk_info_volunteer_task_items_duration_positive
                CHECK (duration_hours > 0);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_info_volunteer_task_items_date_range'
    ) THEN
        ALTER TABLE info_volunteer_task_items
            ADD CONSTRAINT chk_info_volunteer_task_items_date_range
                CHECK (end_date >= start_date);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_info_volunteer_task_items_info_id
    ON info_volunteer_task_items (info_id);
