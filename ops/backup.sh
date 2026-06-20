#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="${SCRIPT_DIR}/.."
BACKUP_DIR="${REPO_DIR}/backups"
BASE_URL="${PV_BASE_URL:-http://localhost:8081/api/v1}"
TIMESTAMP="$(date -u +%Y-%m-%dT%H%M%SZ)"
BACKUP_FILE="${BACKUP_DIR}/backup-${TIMESTAMP}.json"

log() { echo "[$(date -u +%Y-%m-%dT%H:%M:%SZ)] $*" >&2; }

# ── dependency check ──────────────────────────────────────────────────────────

for cmd in curl jq; do
    command -v "${cmd}" > /dev/null 2>&1 \
        || { log "error: '${cmd}' is not installed"; exit 1; }
done

# ── backup ────────────────────────────────────────────────────────────────────

log "Backup started — endpoint: ${BASE_URL}/cases"

mkdir -p "${BACKUP_DIR}"
log "Backup directory: ${BACKUP_DIR}"

log "Fetching cases..."
response="$(curl -sf --show-error "${BASE_URL}/cases")" \
    || { log "error: Failed to reach ${BASE_URL}/cases — is the service running?"; exit 1; }

echo "${response}" | jq '.' > "${BACKUP_FILE}" \
    || { log "error: jq failed to format response"; exit 1; }

case_count="$(echo "${response}" | jq 'length')"
log "Backup complete — ${case_count} case(s) written to ${BACKUP_FILE}"
