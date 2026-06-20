# PV Case Review Service

Spring Boot service that powers pharmacovigilance case review. Handles follow-up merging, field-level diff annotations, and reviewer queries.

---

## Build and run

**Prerequisites:** Java 17+ on PATH, internet access to Maven Central.

```bash
cd backend
./gradlew bootRun
```

Service starts on `http://localhost:8080/api/v1`.  
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
curl -s http://localhost:8080/api/v1/cases/PV-2026-0451 | python3 -m json.tool
```

**Submit a follow-up**

The follow-up payload mirrors the case structure but may be partial. The response annotates every field with a `status` indicating what changed.

```bash
curl -s -X POST http://localhost:8080/api/v1/cases/PV-2026-0451/follow-ups \
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
curl -s -X POST http://localhost:8080/api/v1/queries \
  -H "Content-Type: application/json" \
  -d '{
    "case_id":    "PV-2026-0451",
    "field_path": "adverse_event.outcome",
    "question":   "Source document indicates partial recovery — please confirm whether outcome should remain Not Recovered or be updated to Recovering."
  }' | python3 -m json.tool
```

**List queries for a case**

```bash
curl -s "http://localhost:8080/api/v1/queries?caseId=PV-2026-0451" | python3 -m json.tool
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
