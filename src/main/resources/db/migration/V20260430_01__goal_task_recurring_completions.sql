-- Recurring goal task completion tracking (2026-04-30)

DO $$
BEGIN
    IF to_regclass('public.goal_tasks') IS NULL THEN
        RETURN;
    END IF;

    ALTER TABLE goal_tasks
        ADD COLUMN IF NOT EXISTS cycle_type VARCHAR(20) NOT NULL DEFAULT 'ONE_TIME',
        ADD COLUMN IF NOT EXISTS cycle_frequency VARCHAR(20),
        ADD COLUMN IF NOT EXISTS cycle_interval INTEGER,
        ADD COLUMN IF NOT EXISTS cycle_unit VARCHAR(20),
        ADD COLUMN IF NOT EXISTS cycle_label VARCHAR(120),
        ADD COLUMN IF NOT EXISTS cycle_end_at DATE,
        ADD COLUMN IF NOT EXISTS cycle_no_end BOOLEAN NOT NULL DEFAULT TRUE,
        ADD COLUMN IF NOT EXISTS enrollment_start_at DATE,
        ADD COLUMN IF NOT EXISTS enrollment_end_at DATE,
        ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE;

    UPDATE goal_tasks
    SET enrollment_start_at = COALESCE(enrollment_start_at, due_at, created_at::date)
    WHERE enrollment_start_at IS NULL;

    CREATE INDEX IF NOT EXISTS idx_goal_tasks_group_active
        ON goal_tasks (task_group_id, active);
END $$;

CREATE TABLE IF NOT EXISTS goal_task_completion_entries (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    goal_task_id BIGINT NOT NULL REFERENCES goal_tasks(id) ON DELETE CASCADE,
    occurrence_key VARCHAR(40) NOT NULL,
    occurrence_label VARCHAR(120) NOT NULL,
    occurrence_start_at DATE NOT NULL,
    occurrence_end_at DATE,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    completed_at TIMESTAMP,
    updated_by_teacher_id BIGINT REFERENCES teachers(id),
    progress_note VARCHAR(2000) NOT NULL DEFAULT '',
    CONSTRAINT uq_goal_task_completion_goal_occurrence UNIQUE (goal_task_id, occurrence_key)
);

CREATE INDEX IF NOT EXISTS idx_goal_task_completion_goal
    ON goal_task_completion_entries (goal_task_id);

CREATE INDEX IF NOT EXISTS idx_goal_task_completion_occurrence
    ON goal_task_completion_entries (occurrence_start_at);

CREATE INDEX IF NOT EXISTS idx_goal_task_completion_teacher
    ON goal_task_completion_entries (updated_by_teacher_id);
