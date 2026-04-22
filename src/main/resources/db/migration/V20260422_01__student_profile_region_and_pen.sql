ALTER TABLE student_profile
    ADD COLUMN IF NOT EXISTS student_region VARCHAR(64);

ALTER TABLE student_profile
    ADD COLUMN IF NOT EXISTS pen_number VARCHAR(80);

UPDATE student_profile
SET student_region = 'Ontario'
WHERE (student_region IS NULL OR btrim(student_region) = '')
  AND btrim(COALESCE(oen_number, '')) <> '';

UPDATE student_profile
SET student_region = 'British Columbia'
WHERE (student_region IS NULL OR btrim(student_region) = '')
  AND btrim(COALESCE(pen_number, '')) <> '';

UPDATE student_profile
SET student_region = 'Ontario'
WHERE student_region IS NULL
   OR btrim(student_region) = '';

UPDATE student_profile
SET student_region = CASE
    WHEN upper(btrim(student_region)) IN ('ON', 'CA-ON', 'ONTARIO') THEN 'Ontario'
    WHEN upper(btrim(student_region)) IN ('BC', 'CA-BC', 'BRITISH COLUMBIA') THEN 'British Columbia'
    WHEN upper(btrim(student_region)) IN ('AB', 'ALBERTA') THEN 'Alberta'
    WHEN upper(btrim(student_region)) IN ('SK', 'SASKATCHEWAN') THEN 'Saskatchewan'
    WHEN upper(btrim(student_region)) IN ('MB', 'MANITOBA') THEN 'Manitoba'
    WHEN upper(btrim(student_region)) IN ('QC', 'QUEBEC') THEN 'Quebec'
    WHEN upper(btrim(student_region)) IN ('NB', 'NEW BRUNSWICK') THEN 'New Brunswick'
    WHEN upper(btrim(student_region)) IN ('NS', 'NOVA SCOTIA') THEN 'Nova Scotia'
    WHEN upper(btrim(student_region)) IN ('PEI', 'PRINCE EDWARD ISLAND') THEN 'Prince Edward Island'
    WHEN upper(btrim(student_region)) IN ('NL', 'NEWFOUNDLAND AND LABRADOR') THEN 'Newfoundland and Labrador'
    WHEN upper(btrim(student_region)) IN ('YT', 'YUKON') THEN 'Yukon'
    WHEN upper(btrim(student_region)) IN ('NT', 'NORTHWEST TERRITORIES') THEN 'Northwest Territories'
    WHEN upper(btrim(student_region)) IN ('NU', 'NUNAVUT') THEN 'Nunavut'
    WHEN upper(btrim(student_region)) IN ('CN', 'CHINA') THEN 'China'
    WHEN upper(btrim(student_region)) IN ('US', 'USA', 'UNITED STATES', 'UNITED STATES OF AMERICA') THEN 'United States'
    ELSE student_region
    END;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ck_student_profile_student_region'
    ) THEN
        ALTER TABLE student_profile
            ADD CONSTRAINT ck_student_profile_student_region
                CHECK (
                    student_region IN (
                        'Ontario',
                        'British Columbia',
                        'Alberta',
                        'Saskatchewan',
                        'Manitoba',
                        'Quebec',
                        'New Brunswick',
                        'Nova Scotia',
                        'Prince Edward Island',
                        'Newfoundland and Labrador',
                        'Yukon',
                        'Northwest Territories',
                        'Nunavut',
                        'China',
                        'United States'
                    )
                );
    END IF;
END $$;
