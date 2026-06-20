# PV Case Reviewer — Frontend

React reviewer UI for the pharmacovigilance case processing platform. Built with Vite. Connects to the Spring Boot backend at `http://localhost:8081/api/v1`.

This module is implemented during the Phase 2 live session.

---

## What it does

The UI lets a human reviewer inspect AI-extracted case data and validate it before sign-off.

**Key screens and features:**

- **Case view** — loads case `PV-2026-0451` on mount from `GET /cases/PV-2026-0451`; fields are grouped into Patient, Suspect Drug, Adverse Event, and Reporter sections
- **Confidence colour-coding** — each field shows an AI confidence score; low `< 0.80` (red), medium `0.80–0.90` (amber), high `> 0.90` (green); brand palette: navy `#0C1A36`, blue `#0077B6`, teal `#00C2E0`
- **Conflict view** — fields with `status: "overridden"` show the new value alongside the previous value for side-by-side comparison
- **Raise Query modal** — reviewer can flag a field with a question; submits to `POST /queries` with `{ caseId, fieldPath, question }`
- **Case Classification selector** — mark a case as significant / non-significant / unclassified (UI-only, not persisted)
- **Sort and filter** — sort fields by confidence (low first); filter to conflicting fields only
- **Missing fields banner** — surfaces fields the AI could not extract

---

## Dev

```bash
cd frontend
npm install
npm run dev      # dev server at http://localhost:5173
npm run build    # production build
npm run lint
```

Set `VITE_API_URL` to point at a non-default backend URL:

```bash
VITE_API_URL=http://localhost:8081/api/v1 npm run dev
```
