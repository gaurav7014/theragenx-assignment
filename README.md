# TheragenX PV Case Processing Platform

## What this is

Pharmacovigilance (PV) is the science of detecting, assessing, and preventing adverse effects of medicines. Regulatory agencies require pharmaceutical companies to review every adverse-event report — a patient experiencing an unexpected drug reaction — and classify it before submission to health authorities.

This platform automates the first pass. An AI model reads a source document (a clinical or patient report) and extracts structured fields: patient demographics, the suspect drug, the adverse event details, and reporter information. A human reviewer then opens the case, inspects the AI's extractions alongside confidence scores and source references, flags any fields that look wrong, and signs off on the classification.

The platform also handles **follow-ups**: when a new version of a source document arrives, the AI re-extracts the same fields. The backend merges the two versions field-by-field, annotating each field as unchanged, overridden (value changed), new (not in the original), or missing from the follow-up (AI couldn't find it this time). The reviewer sees exactly what changed and why before deciding whether to accept or query it.

---

## Architecture

```
┌─────────────────────┐        REST / JSON        ┌──────────────────────┐
│   React Reviewer UI │  ◄───────────────────────► │  Spring Boot API     │
│   (frontend/)       │                            │  (backend/)          │
└─────────────────────┘                            └──────────┬───────────┘
                                                              │ in-memory
                                                              │ ConcurrentHashMap
                                                   ┌──────────▼───────────┐
                                                   │  case_v1.json        │
                                                   │  (bootstrap data)    │
                                                   └──────────────────────┘
```

**Backend** — Spring Boot 3.5, Java 17. No database; all state is in-memory and bootstrapped from `case_v1.json` on startup. Exposes a REST API for case retrieval, follow-up merging, and reviewer queries.

**Frontend** — React + Vite reviewer UI. Displays field groups with confidence colour-coding, conflict diffs, a query modal, and sort/filter controls. Implemented in the Phase 2 live session.

**Ops** — Shell scripts (`run.sh`, `backup.sh`, `restore.sh`) and a Makefile. Handles the full service lifecycle, plus backup and restore of in-memory state via the API.

---

## Phases

| Phase | Scope | Status |
|-------|-------|--------|
| 1A | Spring Boot backend — case loading, follow-up merge, query API, tests, global error handling | Complete |
| 1B | Docker, docker-compose, ops scripts, Makefile, README | Complete |
| 2 | React reviewer UI | Live session |

---

## Quick start

Requires Docker and Docker Compose v2.

```bash
make build    # build the Docker image
make start    # bring the service up; waits for the healthcheck; prints the URL
```

Service is available at `http://localhost:8081/api/v1`.

```bash
make stop     # tear it down
make test     # run backend unit tests on the host (no Docker needed)
make logs     # tail live container logs
make clean    # remove containers, images, and build artifacts
```

---

## Repository layout

```
backend/    Spring Boot service — controllers, services, models, tests
ops/        run.sh · backup.sh · restore.sh · README
frontend/   React + Vite app — reviewer UI
Makefile    Top-level targets delegating to ops/
```

Full API reference and design decisions → `backend/README.md`  
Script usage and operability runbook → `ops/README.md`  
Frontend feature spec and dev setup → `frontend/README.md`
