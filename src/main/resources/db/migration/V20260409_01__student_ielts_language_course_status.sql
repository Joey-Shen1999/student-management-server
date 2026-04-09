-- Teacher-recorded language course enrollment/progress status

ALTER TABLE student_ielts_module
    ADD COLUMN IF NOT EXISTS language_course_status VARCHAR(64);
