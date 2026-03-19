-- Add school board field for student school records.
ALTER TABLE IF EXISTS student_school_record
    ADD COLUMN IF NOT EXISTS school_board VARCHAR(64);
