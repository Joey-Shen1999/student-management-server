#!/usr/bin/env bash
set -euo pipefail

# Usage:
#   APP_DB_NAME=student_management_prod APP_DB_USER=student_app APP_DB_PASSWORD='strong-password' \
#   bash ops/production/init-postgresql.sh

: "${APP_DB_NAME:=student_management_prod}"
: "${APP_DB_USER:=student_app}"
: "${APP_DB_PASSWORD:?APP_DB_PASSWORD is required}"

sudo -u postgres psql -v ON_ERROR_STOP=1 <<SQL
DO \$\$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = '${APP_DB_USER}') THEN
    CREATE ROLE ${APP_DB_USER} LOGIN PASSWORD '${APP_DB_PASSWORD}';
  ELSE
    ALTER ROLE ${APP_DB_USER} WITH LOGIN PASSWORD '${APP_DB_PASSWORD}';
  END IF;
END
\$\$;
SQL

if ! sudo -u postgres psql -lqt | cut -d '|' -f 1 | tr -d ' ' | grep -qx "${APP_DB_NAME}"; then
  sudo -u postgres createdb --owner="${APP_DB_USER}" "${APP_DB_NAME}"
fi

sudo -u postgres psql -v ON_ERROR_STOP=1 <<SQL
GRANT ALL PRIVILEGES ON DATABASE ${APP_DB_NAME} TO ${APP_DB_USER};
SQL

echo "PostgreSQL initialized: db=${APP_DB_NAME}, user=${APP_DB_USER}"
