ALTER TABLE IF EXISTS info_volunteer_task_items
    ALTER COLUMN duration_hours DROP NOT NULL;

ALTER TABLE IF EXISTS info_volunteer_task_items
    ALTER COLUMN verifier_contact DROP NOT NULL;
