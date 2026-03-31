-- Performance index bundle (2026-03-27)
-- Goal: accelerate high-frequency auth/session checks and task/profile list queries.

-- users
CREATE INDEX IF NOT EXISTS idx_users_role
    ON users (role);

CREATE INDEX IF NOT EXISTS idx_users_status
    ON users (status);

-- user_sessions
CREATE INDEX IF NOT EXISTS idx_user_sessions_user_revoked
    ON user_sessions (user_id, revoked_at);

-- teacher_student
CREATE INDEX IF NOT EXISTS idx_teacher_student_teacher_student
    ON teacher_student (teacher_id, student_id);

CREATE INDEX IF NOT EXISTS idx_teacher_student_teacher_status_student
    ON teacher_student (teacher_id, status, student_id);

-- goal_tasks
CREATE INDEX IF NOT EXISTS idx_goal_tasks_student_updated_id
    ON goal_tasks (assigned_student_id, updated_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_goal_tasks_teacher_updated_id
    ON goal_tasks (assigned_by_teacher_id, updated_at DESC, id DESC);

-- info_tasks
CREATE INDEX IF NOT EXISTS idx_info_tasks_publisher_updated_id
    ON info_tasks (published_by_teacher_id, updated_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_info_tasks_category_updated_id
    ON info_tasks (category, updated_at DESC, id DESC);

-- info_task_recipients
CREATE INDEX IF NOT EXISTS idx_info_recipient_student_read
    ON info_task_recipients (student_id, is_read);

CREATE INDEX IF NOT EXISTS idx_info_recipient_student_task
    ON info_task_recipients (student_id, info_task_id);

-- dll_tasks
CREATE INDEX IF NOT EXISTS idx_dll_task_creator_updated_id
    ON dll_tasks (created_by_teacher_id, updated_at DESC, id DESC);

-- student files/transcripts
CREATE INDEX IF NOT EXISTS idx_school_transcript_school_uploaded_id
    ON student_school_transcript (school_record_id, uploaded_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_student_identity_profile_uploaded_id
    ON student_identity_file (student_profile_id, uploaded_at DESC, id DESC);

-- student schools
CREATE INDEX IF NOT EXISTS idx_student_school_record_student_type_name_dates
    ON student_school_record (student_id, school_type, school_name, start_time, end_time);
