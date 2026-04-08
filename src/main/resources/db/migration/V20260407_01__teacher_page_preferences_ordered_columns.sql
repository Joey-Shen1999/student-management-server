-- Teacher page preference storage (column visibility + order)

DO $$
BEGIN
    IF to_regclass('public.teacher_page_preferences') IS NULL THEN
        CREATE TABLE teacher_page_preferences (
            id BIGSERIAL PRIMARY KEY,
            created_at TIMESTAMP NOT NULL DEFAULT NOW(),
            updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
            teacher_id BIGINT NOT NULL,
            page_key VARCHAR(160) NOT NULL,
            version VARCHAR(32),
            visible_column_keys_json TEXT NOT NULL DEFAULT '[]',
            ordered_column_keys_json TEXT
        );
    END IF;

    ALTER TABLE teacher_page_preferences
        ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT NOW();
    ALTER TABLE teacher_page_preferences
        ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT NOW();
    ALTER TABLE teacher_page_preferences
        ADD COLUMN IF NOT EXISTS teacher_id BIGINT;
    ALTER TABLE teacher_page_preferences
        ADD COLUMN IF NOT EXISTS page_key VARCHAR(160);
    ALTER TABLE teacher_page_preferences
        ADD COLUMN IF NOT EXISTS version VARCHAR(32);
    ALTER TABLE teacher_page_preferences
        ADD COLUMN IF NOT EXISTS visible_column_keys_json TEXT NOT NULL DEFAULT '[]';
    ALTER TABLE teacher_page_preferences
        ADD COLUMN IF NOT EXISTS ordered_column_keys_json TEXT;

    UPDATE teacher_page_preferences
    SET visible_column_keys_json = '[]'
    WHERE visible_column_keys_json IS NULL;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_teacher_page_preferences_teacher'
    ) THEN
        ALTER TABLE teacher_page_preferences
            ADD CONSTRAINT fk_teacher_page_preferences_teacher
                FOREIGN KEY (teacher_id) REFERENCES teachers(id) ON DELETE CASCADE;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uq_teacher_page_preferences_teacher_page_key'
    ) THEN
        ALTER TABLE teacher_page_preferences
            ADD CONSTRAINT uq_teacher_page_preferences_teacher_page_key
                UNIQUE (teacher_id, page_key);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_teacher_page_preferences_teacher
    ON teacher_page_preferences (teacher_id);
CREATE INDEX IF NOT EXISTS idx_teacher_page_preferences_page_key
    ON teacher_page_preferences (page_key);
