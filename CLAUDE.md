# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Pharmacovigilance (PV) case processing platform. AI extracts structured fields from adverse-event source documents; a human reviewer validates and signs off. This repo is a take-home assignment with three phases.

**Repo layout**

```
/backend    Spring Boot Java service
/ops        Shell scripts and Makefile
/frontend   React UI (Phase 2, live session)
```

---

## Phase 1A — Spring Boot Backend

### Run & test

```bash
cd backend
./gradlew bootRun                                                  # start service (port 8080)
./gradlew test                                                     # run all tests
./gradlew test --tests "com.theragenx.pv.SomeTest#methodName"     # single test
```

**Local environment notes:**
- `gradle.properties` sets `org.gradle.java.home` to the Homebrew OpenJDK path — no manual `JAVA_HOME` needed.
- `gradle/wrapper/gradle-wrapper.properties` points `distributionUrl` to `file:///tmp/gradle-9.6.0-bin.zip` (a local zip of the Homebrew Gradle install). If that zip is missing on a new machine, recreate it: `cd /tmp && mkdir -p gradle-9.6.0-staging/gradle-9.6.0 && cp -r /opt/homebrew/Cellar/gradle/9.6.0/libexec/* gradle-9.6.0-staging/gradle-9.6.0/ && cd gradle-9.6.0-staging && zip -qr /tmp/gradle-9.6.0-bin.zip gradle-9.6.0/`
- Maven Central is accessible; only the Gradle Plugin Portal (`plugins.gradle.org`) is blocked by the corporate proxy.

### Package structure

Controllers → Services → Models (no DB layer; in-memory storage only).

### Bootstrap

On startup, load `case_v1.json` into in-memory storage as case `PV-2026-0451`. No database.

### Endpoints

All routes are prefixed with `/api/v1` via `server.servlet.context-path` in `application.properties`. Add new endpoints under their resource path only — the prefix is applied globally.

| Method | Path | Notes |
|--------|------|-------|
| `GET` | `/api/v1/cases` | Lists all cases |
| `GET` | `/api/v1/cases/{caseId}` | Returns latest merged case |
| `POST` | `/api/v1/cases/{caseId}/follow-ups` | Merges follow-up; returns merged case with diff |
| `POST` | `/api/v1/queries` | Body: `{caseId, fieldPath, question}` |
| `GET` | `/api/v1/queries?caseId={id}` | Lists queries for a case |
| `GET` | `/api/v1/health` | Liveness check |

### Merge logic (core behavior)

For each field when a follow-up is POSTed:

- **Same value** → `status: "unchanged"`
- **Different value** → `status: "overridden"`, include `previous_value`
- **New field** (not in stored version) → `status: "new"`
- **Field missing from follow-up** (present in stored) → retain with `status: "unchanged"` (follow-up is additive/partial, not a full replace)

Follow-up payload has a top-level `missing_fields` array (fields AI couldn't extract) — surface this in the merged response.

Validation: return 400 for malformed input, 404 for unknown caseId.

---

## Phase 1B — Operability

### Docker

```bash
docker build -t pv-service .
docker compose up -d
docker compose down
```

Multi-stage Dockerfile: builder stage + minimal runtime image, non-root user, pinned base image versions.

### ops scripts

```bash
ops/run.sh build|start|stop|test|logs|clean   # --help for usage
ops/backup.sh          # fetches all cases via curl/jq → backups/<timestamp>.json
ops/restore.sh <file>  # POSTs each case back; supports --dry-run
```

All scripts: proper variable quoting, non-zero exit on failure, no interactive prompts (safe for cron).

### Makefile

`.PHONY` targets; most delegate to `ops/run.sh`.

---

## Phase 2 — React Frontend (live session)

Stack: Next.js, Vite, or CRA (your choice). Located in `/frontend`.

### Dev

```bash
cd frontend
npm install
npm run dev      # dev server
npm run build    # production build
npm run lint
```

### Backend base URL

Java backend runs locally; configure via env var (e.g. `NEXT_PUBLIC_API_URL` or `VITE_API_URL`).

### Key screens / state

1. **Load** merged case from `GET /cases/PV-2026-0451` on mount.
2. **Field groups**: Patient, Suspect Drug, Adverse Event, Reporter.
3. **Per field**: label, value, AI confidence score, source reference.
4. **Confidence color-coding**: low `< 0.80`, medium `0.80–0.90`, high `> 0.90`. Brand colors: navy `#0C1A36`, blue `#0077B6`, teal `#00C2E0`.
5. **Conflict view**: `status: "overridden"` fields show new value (primary) + previous value side-by-side.
6. **Raise Query modal**: textarea + submit → `POST /queries` with `{caseId, fieldPath, question}`.
7. **Case Classification selector** (top of page): significant / non-significant / null. UI-only, no backend persistence.
8. **Sort by confidence** (low first) and **filter to conflicting fields only**.

Bonus if time allows: status pills (New/Overridden/Unchanged), `missing_fields` banner, loading/error/empty states, keyboard navigation.

---

## Constraints & non-goals

- No authentication anywhere.
- No real database (in-memory only for backend).
- No Kubernetes, Terraform, or cloud deployment.
- Do not over-engineer; the backend is intentionally simple.
