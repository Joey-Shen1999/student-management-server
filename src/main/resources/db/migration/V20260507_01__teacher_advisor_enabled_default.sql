ALTER TABLE teachers
    ADD COLUMN IF NOT EXISTS advisor_enabled boolean;

UPDATE teachers
SET advisor_enabled = false
WHERE advisor_enabled IS NULL;

ALTER TABLE teachers
    ALTER COLUMN advisor_enabled SET DEFAULT false;
