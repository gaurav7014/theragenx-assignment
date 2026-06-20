# ops

Shell scripts and a Makefile for managing the PV service. All scripts use `set -euo pipefail`, quote every variable, and are safe to run unattended (no interactive prompts).

---

## Prerequisites

| Tool | Used by |
|------|---------|
| Docker + Compose v2 | `run.sh` build / start / stop / logs / clean |
| JDK 17+ (`JAVA_HOME` or on PATH) | `run.sh test` |
| Gradle (system install, or set `$GRADLE_HOME`) | `run.sh test` — only needed once to recreate the wrapper zip |
| `curl`, `jq` | `backup.sh`, `restore.sh` |
| `zip` | `run.sh test` — recreating the Gradle distribution zip if missing |

---

## Scripts

### `run.sh`

Wrapper for common lifecycle operations. Run from the repo root.

```bash
ops/run.sh build    # docker compose build
ops/run.sh start    # bring the stack up; waits for healthcheck; prints base URL
ops/run.sh stop     # docker compose down
ops/run.sh test     # ./gradlew test on the host (auto-recreates Gradle zip if missing)
ops/run.sh logs     # tail pv-service container logs (Ctrl-C to stop)
ops/run.sh clean    # remove containers, images, and Gradle build artifacts
```

Every subcommand accepts `--help`. Set `PV_BASE_URL` to override the default `http://localhost:8081/api/v1`.

### `backup.sh`

Hits `GET /cases` on the running service and writes a pretty-printed JSON snapshot to `backups/backup-<timestamp>.json`. Creates the `backups/` directory if it doesn't exist. All log output goes to stderr — safe to redirect stdout to a file independently.

```bash
ops/backup.sh
# or with a custom service URL:
PV_BASE_URL=http://host:port/api/v1 ops/backup.sh
```

### `restore.sh`

Takes a backup file and replays each case to the running service via `PUT /cases/{caseId}` (full overwrite, not a merge). Idempotent — running it twice with the same file produces the same result.

```bash
ops/restore.sh backups/backup-2026-06-20T120000Z.json

# Preview what would be sent without making any HTTP calls:
ops/restore.sh --dry-run backups/backup-2026-06-20T120000Z.json
```

Exits non-zero if any case fails to restore. Per-case errors are logged but do not abort the remaining cases.

---

## Makefile

Convenience wrappers around the scripts above. Run from the repo root.

```bash
make build
make start
make stop
make test
make logs
make clean
make backup
make restore FILE=backups/backup-2026-06-20T120000Z.json
make restore FILE=backups/backup-2026-06-20T120000Z.json ARGS=--dry-run
```
