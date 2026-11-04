ALTER TABLE interview_course_plans ADD COLUMN IF NOT EXISTS teacher_id BIGINT REFERENCES teachers(id);
CREATE INDEX IF NOT EXISTS idx_interview_course_plan_teacher ON interview_course_plans(teacher_id);
