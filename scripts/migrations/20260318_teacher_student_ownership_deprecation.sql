-- Ownership deprecation migration (2026-03-18)
-- Goal: stop requiring teacher ownership for student invite / student management authorization.
-- Safe to run on PostgreSQL. Legacy data is retained.

-- 1) Invite records no longer require teacher binding.
ALTER TABLE IF EXISTS student_invites
    ALTER COLUMN teacher_id DROP NOT NULL;

-- 2) Clear legacy owner pointer on students to fully disable ownership semantics.
UPDATE students
SET teacher_id = NULL
WHERE teacher_id IS NOT NULL;

-- 3) Legacy teacher_student relation rows are intentionally retained for history/audit.
--    Current auth/permission logic no longer relies on this table.
