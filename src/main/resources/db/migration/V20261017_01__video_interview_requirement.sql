ALTER TABLE graduation_application_portal_credentials
    ADD COLUMN IF NOT EXISTS video_interview_required BOOLEAN NOT NULL DEFAULT FALSE;
