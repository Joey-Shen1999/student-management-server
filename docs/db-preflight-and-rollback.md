# DB Preflight And Rollback

This checklist is for the release that contains:
- `V20260330_01__task_group_refactor.sql`
- `V20260330_02__ielts_tracking_module.sql`

## 1. Backup Before Deploy

If DB runs in Docker:

```bash
docker exec -e PGPASSWORD=postgres uni_apply_db \
  pg_dump -U postgres -d uni_apply -F c -f /tmp/uni_apply_pre_release.dump
docker cp uni_apply_db:/tmp/uni_apply_pre_release.dump ./uni_apply_pre_release.dump
```

If DB is external PostgreSQL:

```bash
PGPASSWORD="$DB_PASSWORD" pg_dump \
  -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" \
  -F c -f ./uni_apply_pre_release.dump
```

## 2. Run Read-Only Preflight

```bash
docker cp scripts/ops/preflight_release_20260330.sql uni_apply_db:/tmp/preflight_release_20260330.sql
docker exec -e PGPASSWORD=postgres uni_apply_db \
  psql -v ON_ERROR_STOP=1 -U postgres -d uni_apply -f /tmp/preflight_release_20260330.sql
```

Expected result:
- `NOTICE: [PASS] DB preflight completed successfully.`

If preflight fails:
- stop deployment immediately
- resolve the reported DB issue first

## 3. Checksum Mismatch Handling

Current release expects checksum for `20260330.01`:
- `1323014771`

Inspect current DB value:

```sql
SELECT version, checksum, installed_on
FROM flyway_schema_history
WHERE version = '20260330.01';
```

If checksum is different (for example `-2126154558`):
- preferred: restore the original migration content or create a new migration for new changes
- emergency only: run Flyway `repair` with approved change control

## 4. Rollback Checklist

1. Stop service.
2. Roll back application code to previous stable commit/tag.
3. Restore DB from `uni_apply_pre_release.dump` if schema/data changed and app rollback alone is insufficient.
4. Start service and verify health.
5. Confirm Flyway history has no failed rows:

```sql
SELECT installed_rank, version, description, checksum, success, installed_on
FROM flyway_schema_history
ORDER BY installed_rank;
```

## 5. Guard Rails

- Do not edit already-applied Flyway SQL files.
- Keep production profile strict:
  - `spring.jpa.hibernate.ddl-auto=validate`
  - `spring.flyway.baseline-on-migrate=false`
  - `spring.flyway.clean-disabled=true`
