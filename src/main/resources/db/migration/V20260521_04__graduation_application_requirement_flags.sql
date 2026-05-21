ALTER TABLE graduation_application_portal_credentials
    ADD COLUMN IF NOT EXISTS interview_required BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE graduation_application_portal_credentials
    ADD COLUMN IF NOT EXISTS language_score_required BOOLEAN NOT NULL DEFAULT FALSE;
