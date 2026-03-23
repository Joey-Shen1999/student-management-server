-- Add teacher-only internal note field to student profile.
ALTER TABLE student_profile
    ADD COLUMN IF NOT EXISTS teacher_note VARCHAR(5000);
