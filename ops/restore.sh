#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${PV_BASE_URL:-http://localhost:8081/api/v1}"
DRY_RUN=false

log() { echo "[$(date -u +%Y-%m-%dT%H:%M:%SZ)] $*" >&2; }

# ── usage ─────────────────────────────────────────────────────────────────────

usage() {
    cat <<EOF
Usage: $(basename "$0") [--dry-run] <backup-file>

Restores cases from a backup file produced by backup.sh.
Each case is replaced via PUT /cases/{caseId} (full overwrite, not a merge).
Idempotent: PUTting the same data twice produces the same stored state.

Options:
  --dry-run   Print what would be POSTed without making any HTTP calls.

Environment:
  PV_BASE_URL   Override the service base URL (default: http://localhost:8081/api/v1)
EOF
}

# ── arg parsing ───────────────────────────────────────────────────────────────

BACKUP_FILE=""
for arg in "$@"; do
    case "${arg}" in
        --dry-run)  DRY_RUN=true ;;
        --help|-h)  usage; exit 0 ;;
        -*)         echo "error: unknown option '${arg}'" >&2; usage >&2; exit 2 ;;
        *)
            [ -z "${BACKUP_FILE}" ] \
                || { echo "error: unexpected argument '${arg}'" >&2; usage >&2; exit 2; }
            BACKUP_FILE="${arg}"
            ;;
    esac
done

[ -n "${BACKUP_FILE}" ] \
    || { echo "error: backup file argument is required" >&2; usage >&2; exit 2; }

# ── dependency check ──────────────────────────────────────────────────────────

for cmd in curl jq; do
    command -v "${cmd}" > /dev/null 2>&1 \
        || { log "error: '${cmd}' is not installed"; exit 1; }
done

# ── validate file ─────────────────────────────────────────────────────────────

[ -f "${BACKUP_FILE}" ] \
    || { log "error: file not found: ${BACKUP_FILE}"; exit 1; }

jq empty < "${BACKUP_FILE}" 2>/dev/null \
    || { log "error: ${BACKUP_FILE} is not valid JSON"; exit 1; }

case_count="$(jq 'length' < "${BACKUP_FILE}")"
if [ "${case_count}" -eq 0 ]; then
    log "Backup file contains 0 cases — nothing to restore"
    exit 0
fi

# ── restore ───────────────────────────────────────────────────────────────────

log "Restore started — ${case_count} case(s) from ${BACKUP_FILE}"
if [ "${DRY_RUN}" = "true" ]; then
    log "DRY RUN — no HTTP requests will be made"
fi

success=0
failed=0

while IFS= read -r case_json; do
    case_id="$(echo "${case_json}" | jq -r '.case_id')"
    endpoint="${BASE_URL}/cases/${case_id}"

    if [ "${DRY_RUN}" = "true" ]; then
        log "[${case_id}] Would PUT to ${endpoint}:"
        echo "${case_json}" | jq '.' >&2
        success=$((success + 1))
        continue
    fi

    log "[${case_id}] PUTting to ${endpoint}..."
    http_status="$(echo "${case_json}" | curl -s \
        -w '%{http_code}' -o /dev/null \
        -X PUT "${endpoint}" \
        -H 'Content-Type: application/json' \
        --data @-)" \
        || { log "[${case_id}] error: connection to ${endpoint} failed"; failed=$((failed + 1)); continue; }

    if [ "${http_status}" = "200" ]; then
        log "[${case_id}] Restored (HTTP ${http_status})"
        success=$((success + 1))
    else
        log "[${case_id}] error: unexpected HTTP ${http_status}"
        failed=$((failed + 1))
    fi

done < <(jq -c '.[]' < "${BACKUP_FILE}")

log "Restore complete — ${success} succeeded, ${failed} failed"
[ "${failed}" -eq 0 ] || exit 1
