ALTER TABLE graduation_application_portal_credentials
    ADD COLUMN IF NOT EXISTS student_visible BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE graduation_application_portal_credentials
SET student_visible = FALSE
WHERE student_visible IS NULL;
