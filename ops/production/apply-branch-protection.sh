#!/usr/bin/env bash
set -euo pipefail

# Usage:
#   GITHUB_REPOSITORY=owner/student-management-server \
#   bash ops/production/apply-branch-protection.sh

: "${GITHUB_REPOSITORY:?Set GITHUB_REPOSITORY=owner/repo}"

if ! command -v gh >/dev/null 2>&1; then
  echo "GitHub CLI (gh) is required."
  exit 1
fi

BRANCH="master"
CONFIG_FILE="ops/production/branch-protection-master.json"

if [[ ! -f "${CONFIG_FILE}" ]]; then
  echo "Missing config file: ${CONFIG_FILE}"
  exit 1
fi

gh api \
  --method PUT \
  -H "Accept: application/vnd.github+json" \
  "/repos/${GITHUB_REPOSITORY}/branches/${BRANCH}/protection" \
  --input "${CONFIG_FILE}"

echo "Branch protection applied for ${GITHUB_REPOSITORY}:${BRANCH}"
