DO $$
BEGIN
    IF to_regclass('public.info_tasks') IS NULL THEN
        RETURN;
    END IF;

    IF to_regclass('public.info_task_attachments') IS NULL THEN
        CREATE TABLE info_task_attachments (
            id BIGSERIAL PRIMARY KEY,
            created_at TIMESTAMP NOT NULL DEFAULT NOW(),
            updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
            info_task_id BIGINT NOT NULL,
            storage_key VARCHAR(255) NOT NULL,
            original_filename VARCHAR(255) NOT NULL,
            mime_type VARCHAR(120) NOT NULL,
            size_bytes BIGINT NOT NULL
        );
    END IF;

    ALTER TABLE info_task_attachments
        ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT NOW();
    ALTER TABLE info_task_attachments
        ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT NOW();
    ALTER TABLE info_task_attachments
        ADD COLUMN IF NOT EXISTS info_task_id BIGINT;
    ALTER TABLE info_task_attachments
        ADD COLUMN IF NOT EXISTS storage_key VARCHAR(255);
    ALTER TABLE info_task_attachments
        ADD COLUMN IF NOT EXISTS original_filename VARCHAR(255);
    ALTER TABLE info_task_attachments
        ADD COLUMN IF NOT EXISTS mime_type VARCHAR(120);
    ALTER TABLE info_task_attachments
        ADD COLUMN IF NOT EXISTS size_bytes BIGINT;

    UPDATE info_task_attachments
    SET created_at = NOW()
    WHERE created_at IS NULL;

    UPDATE info_task_attachments
    SET updated_at = NOW()
    WHERE updated_at IS NULL;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_info_task_attachment_task'
    ) THEN
        ALTER TABLE info_task_attachments
            ADD CONSTRAINT fk_info_task_attachment_task
                FOREIGN KEY (info_task_id) REFERENCES info_tasks(id) ON DELETE CASCADE;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_info_task_attachment_task_id
    ON info_task_attachments (info_task_id);
CREATE INDEX IF NOT EXISTS idx_info_task_attachment_storage_key
    ON info_task_attachments (storage_key);
