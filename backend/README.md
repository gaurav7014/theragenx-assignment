# PV Case Review Service

Spring Boot service that powers pharmacovigilance case review. Handles follow-up merging, field-level diff annotations, and reviewer queries.

---

## Build and run

**Prerequisites:** Java 17+ on PATH, internet access to Maven Central.

```bash
cd backend
./gradlew bootRun
```

Service starts on `http://localhost:8080/api/v1` (Docker: `http://localhost:8081/api/v1`).  
All endpoints are prefixed with `/api/v1` via `server.servlet.context-path`.

To run tests:

```bash
./gradlew test
```

---

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/health` | Liveness check |
| `GET` | `/api/v1/cases` | List all cases |
| `GET` | `/api/v1/cases/{caseId}` | Get latest version of a case |
| `POST` | `/api/v1/cases/{caseId}/follow-ups` | Submit a follow-up and get merged diff |
| `POST` | `/api/v1/queries` | Raise a reviewer query on a field |
| `GET` | `/api/v1/queries?caseId={id}` | List queries for a case |

---

## curl examples

**Get a case**

```bash
curl -s http://localhost:8081/api/v1/cases/PV-2026-0451 | python3 -m json.tool
```

**Submit a follow-up**

The follow-up payload mirrors the case structure but may be partial. The response annotates every field with a `status` indicating what changed.

```bash
curl -s -X POST http://localhost:8081/api/v1/cases/PV-2026-0451/follow-ups \
  -H "Content-Type: application/json" \
  -d '{
    "extracted_at": "2026-05-01T10:30:00Z",
    "source_document": "followup_report_PV-2026-0451.pdf",
    "missing_fields": ["adverse_event.onset_date"],
    "sections": {
      "patient": {
        "initials":  { "value": "M.K.", "confidence": 0.99, "source": "p.2 §1" },
        "age":       { "value": "63",   "confidence": 0.95, "source": "p.2 §1" }
      },
      "adverse_event": {
        "event_term":  { "value": "Myalgia",       "confidence": 0.94, "source": "p.4 §1" },
        "outcome":     { "value": "Not Recovered",  "confidence": 0.90, "source": "p.5 §2" },
        "seriousness": { "value": "Serious",        "confidence": 0.92, "source": "p.5 §2" }
      }
    }
  }' | python3 -m json.tool
```

Expected field statuses in the response:
- `patient.initials` → `unchanged` (same value, confidence updated)
- `patient.age` → `overridden`, `previous_value: "62"`
- `patient.sex`, `patient.weight_kg` → `missing_in_followup`
- `adverse_event.outcome` → `overridden`, `previous_value: "Recovered"`
- `adverse_event.seriousness` → `overridden`, `previous_value: "Non-serious"`

**Raise a reviewer query**

```bash
curl -s -X POST http://localhost:8081/api/v1/queries \
  -H "Content-Type: application/json" \
  -d '{
    "case_id":    "PV-2026-0451",
    "field_path": "adverse_event.outcome",
    "question":   "Source document indicates partial recovery — please confirm whether outcome should remain Not Recovered or be updated to Recovering."
  }' | python3 -m json.tool
```

**List queries for a case**

```bash
curl -s "http://localhost:8081/api/v1/queries?caseId=PV-2026-0451" | python3 -m json.tool
```

---

## Error responses

All errors — validation failures, missing resources, unexpected exceptions — return the same shape:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Case not found: PV-2026-0451"
}
```

| Status | When |
|--------|------|
| 400 | Missing/blank required fields, malformed JSON, unknown `caseId` in query endpoints |
| 404 | `caseId` path variable not found |
| 500 | Unexpected server error |

---

## Design decisions

### `missing_in_followup` — why preserve the old value explicitly

When a follow-up arrives, the AI re-extracts fields from a new source document. If a field that was previously extracted is absent from the follow-up, there are three possible interpretations: the AI couldn't find it, the information genuinely disappeared from the document, or it was never relevant to begin with.

In pharmacovigilance, silently dropping a field — or worse, treating absence as "unchanged" — is dangerous. A reviewer needs to know when the AI failed to reproduce an extraction it made before. `missing_in_followup` surfaces this explicitly: the previous value is still shown, the status signals that the AI did not confirm it in the latest extraction, and the reviewer can decide whether to keep, update, or query it.

The alternative (dropping the field from the merged output) would mean case v2 has fewer fields than v1 — a regression invisible to the reviewer. Marking it `unchanged` would be a lie: the AI didn't say it was unchanged, it said nothing at all.

### `GET /api/v1/cases` — deliberate addition beyond spec

The spec only defines `GET /cases/{caseId}`. A `GET /cases` endpoint that returns all cases was added deliberately as an operational convenience: it makes it possible to inspect the full in-memory store at runtime, which is essential for debugging and is the foundation of the `backup.sh` script in the ops layer. It is not exposed to the reviewer UI.

---

## Bootstrap data

On startup, `case_v1.json` is loaded from the classpath and stored as case `PV-2026-0451`. No database is used. All state is in-memory and resets on restart.

---

## Operations runbook

All commands run from the **repo root**. `make <target>` delegates to the scripts in `ops/`.

### Start the service from scratch

```bash
make build    # build the Docker image
make start    # bring the stack up; waits for healthcheck to pass
```

Expected output from `make start`:

```
Waiting for pv-service to become healthy...
Service is up → http://localhost:8081/api/v1
```

If you see that line, the service is ready to accept requests.

---

### Verify the service is healthy

```bash
curl -s http://localhost:8081/api/v1/health
```

Expected:

```json
{"status":"UP"}
```

Anything other than HTTP 200 with that body means the service is not ready.

---

### Backup and restore

**Back up all cases to a timestamped file:**

```bash
make backup
```

The file is written to `backups/backup-<timestamp>.json`. All log output goes to stderr; only the JSON goes to the file, so it's safe to redirect:

```bash
make backup 2>>ops.log
```

**Preview what a restore would do (no writes):**

```bash
make restore FILE=backups/backup-2026-06-20T120000Z.json ARGS=--dry-run
```

**Restore for real:**

```bash
make restore FILE=backups/backup-2026-06-20T120000Z.json
```

The restore uses `PUT /cases/{caseId}` — a full overwrite. Running it twice with the same file is safe.

---

### Service won't start

**Docker is not running**

```
error: Docker is not running — start Docker and retry.
```

Fix: start the Docker daemon, then retry.

---

**Port 8081 is already in use**

The container binds host port 8081. If something else is already there:

```bash
# find what's using the port
lsof -i :8081          # macOS
ss -tlnp | grep 8081   # Linux

# stop the conflicting process, then:
make start
```

---

**Image build fails**

Run the build with output visible:

```bash
make build
# or directly:
docker compose -f backend/docker-compose.yml build --no-cache
```

Common causes:
- No internet access to Maven Central (needed to download dependencies)
- Java source files won't compile — read the Gradle error in the build output

---

**Container starts but healthcheck never passes**

```bash
make logs
```

Look for a stack trace near startup. The most common cause is a classpath resource missing (`case_v1.json` not found). If the log shows the Spring banner but then stops, the healthcheck interval (`30s` start period) may not have elapsed yet — wait 30 seconds and check again.

---

### Requests are returning errors

**First thing: confirm the service is up**

```bash
curl -s http://localhost:8081/api/v1/health
```

If that fails, see "Service won't start" above.

**Check the container logs for the failing request**

```bash
make logs
```

Spring logs every request and exception. Find the timestamp of the failing request and read the stack trace below it.

**Error shapes to know**

| Status | Likely cause |
|--------|--------------|
| 400 | Missing or blank required field in the request body; malformed JSON; `caseId` in query endpoints doesn't exist |
| 404 | `caseId` path variable not found in the store |
| 500 | Unexpected server error — check logs for the stack trace |

All error responses follow the same shape:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Case not found: PV-2026-0451"
}
```

**State was lost after a restart**

The store is in-memory and resets on every restart. `case_v1.json` is reloaded automatically, so the bootstrap case `PV-2026-0451` will be there. Any follow-ups submitted before the restart are gone unless you ran `make backup` first. Restore with `make restore FILE=<latest-backup>`.
