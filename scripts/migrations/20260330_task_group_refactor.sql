-- Task group refactor for task center (2026-03-30)
-- 1) goal_tasks: introduce task_group_id for grouped task operations
-- 2) info_tasks: support task_group_id-based upsert key (teacher + task_group_id)

-- goal_tasks: add and backfill task_group_id
ALTER TABLE goal_tasks
    ADD COLUMN IF NOT EXISTS task_group_id VARCHAR(64);

UPDATE goal_tasks
SET task_group_id = 'legacy-' || id::text
WHERE task_group_id IS NULL
   OR btrim(task_group_id) = '';

ALTER TABLE goal_tasks
    ALTER COLUMN task_group_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_goal_tasks_task_group_id
    ON goal_tasks (task_group_id);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uq_goal_tasks_group_student'
    ) THEN
        ALTER TABLE goal_tasks
            ADD CONSTRAINT uq_goal_tasks_group_student
                UNIQUE (task_group_id, assigned_student_id);
    END IF;
END $$;

-- info_tasks: add task_group_id with compatibility for schemas without goal_id
DO $$
BEGIN
    IF to_regclass('public.info_tasks') IS NULL THEN
        RETURN;
    END IF;

    ALTER TABLE info_tasks
        ADD COLUMN IF NOT EXISTS task_group_id VARCHAR(64);

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'info_tasks'
          AND column_name = 'goal_id'
    ) THEN
        EXECUTE $sql$
            UPDATE info_tasks
            SET task_group_id = 'legacy-' || goal_id::text
            WHERE (task_group_id IS NULL OR btrim(task_group_id) = '')
              AND goal_id IS NOT NULL
        $sql$;
    END IF;

    UPDATE info_tasks
    SET task_group_id = 'legacy-info-' || id::text
    WHERE task_group_id IS NULL
       OR btrim(task_group_id) = '';

    IF to_regclass('public.info_task_recipients') IS NOT NULL THEN
        WITH ranked AS (
            SELECT id,
                   row_number() OVER (
                       PARTITION BY published_by_teacher_id, task_group_id
                       ORDER BY updated_at DESC NULLS LAST, id DESC
                   ) AS rn
            FROM info_tasks
            WHERE task_group_id IS NOT NULL
        ),
        to_delete AS (
            SELECT id
            FROM ranked
            WHERE rn > 1
        )
        DELETE FROM info_task_recipients r
        USING to_delete d
        WHERE r.info_task_id = d.id;
    END IF;

    WITH ranked AS (
        SELECT id,
               row_number() OVER (
                   PARTITION BY published_by_teacher_id, task_group_id
                   ORDER BY updated_at DESC NULLS LAST, id DESC
               ) AS rn
        FROM info_tasks
        WHERE task_group_id IS NOT NULL
    ),
    to_delete AS (
        SELECT id
        FROM ranked
        WHERE rn > 1
    )
    DELETE FROM info_tasks i
    USING to_delete d
    WHERE i.id = d.id;

    CREATE INDEX IF NOT EXISTS idx_info_tasks_publisher_task_group
        ON info_tasks (published_by_teacher_id, task_group_id);

    CREATE UNIQUE INDEX IF NOT EXISTS uq_info_tasks_teacher_group
        ON info_tasks (published_by_teacher_id, task_group_id)
        WHERE task_group_id IS NOT NULL;
END $$;
