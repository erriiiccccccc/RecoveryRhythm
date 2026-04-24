# Recovery Rhythm

Post-discharge recovery monitoring — proof of concept for a Software Engineering coursework.

Patients log daily signals and photo evidence. Clinicians review them, run risk assessments, and get alerted when someone hits ACUTE_RISK. A personalised baseline is compared against recent behaviour to produce a 0–100 risk score and a named recovery state.

> **Disclaimer:** This is a software engineering POC, not a medical device. Auth is client-side sessionStorage for demo purposes only.

---

## Quick Start

```bash
docker compose up --build
```

Then open **http://localhost:8080/login.html**

- Clinician: `clinician@rr.nhs.uk` / `demo123`
- Patient: create one via the clinician UI ("Add New Patient"), then log in with that email on the patient portal

That's it. Docker handles everything — Postgres, Redis, Kafka, RabbitMQ, S3 (LocalStack), and the app.

---

## Prerequisites

- **Docker** and **Docker Compose** — required
- **Java 17** + **Maven** — only if running the app outside Docker
- **Node.js** — only for the npm script shortcuts

---

## Other ways to run

### Infrastructure in Docker, app local

```bash
npm run dev:infra-up   # or: docker compose up -d postgres redis kafka rabbitmq localstack
mvn spring-boot:run    # or: npm run dev:app
```

### npm scripts

| Script | What it does |
|--------|--------------|
| `npm run dev` | Start infra + run app |
| `npm run dev:infra-up` | Start backing services only |
| `npm run dev:infra-down` | Stop infra containers (keeps volumes) |
| `npm run dev:infra-reset` | `docker compose down -v` — **wipes the database** |
| `npm run build` | `mvn clean package` |
| `npm run test` | `mvn test` |

---

## Ports

| Service | Port |
|---------|------|
| App | http://localhost:8080 |
| PostgreSQL | 5432 |
| Redis | 6379 |
| Kafka | 9092 |
| RabbitMQ AMQP | 5672 |
| RabbitMQ UI | http://localhost:15672 (guest / guest) |
| LocalStack (S3) | http://localhost:4566 |

---

## Architecture

```
clinician.html          patient.html
(polls 5s / 15s)        (submits signals + photos)
      │                        │
      └──── REST / JSON / multipart ────┘
                     │
          ┌──────────▼───────────┐
          │   Spring Boot 3.2    │
          │  (Java 17)           │
          │                      │
          │  UserController      │
          │  SignalController    │
          │  EvidenceController  │
          │  AssessmentController│
          │  EpisodeController   │
          │                      │
          │  BaselineEngine      │
          │  RiskEngine          │
          │  StateEngine         │
          └──┬──────┬──────┬─────┘
             │      │      │
        ┌────▼─┐ ┌──▼─┐ ┌──▼───┐
        │  PG  │ │ S3 │ │Redis │
        └──┬───┘ └────┘ └──────┘
           │
      ┌────▼──────┬──────────────┐
      │ RabbitMQ  │    Kafka     │
      │ (async    │ (domain      │
      │  jobs)    │  events)     │
      └───────────┴──────────────┘
```

| Component | Role |
|-----------|------|
| **PostgreSQL** | Main store — users, signals, evidence, baselines, assessments, episodes, escalations |
| **S3 / LocalStack** | Photo storage for evidence and profile images |
| **Redis** | Risk score cache (5s TTL), streak counters, intervention dedup locks |
| **RabbitMQ** | Async jobs — intervention, escalation, re-entry queues |
| **Kafka** | Domain event log — risk assessed, state changed, signal logged, escalation triggered, etc. |

---

## How the risk engine works

### Step 1 — Baseline

```bash
POST /api/users/{id}/baseline/recalculate
```

`BaselineEngine` reads the first N days of signal logs (default 5, min 3) and computes personal rates: morning check-in %, medication %, meals %, activity %, evening check-in %, average sleep hour. Saved as a `BaselineSnapshot`.

### Step 2 — Risk score

```bash
POST /api/users/{id}/risk/recalculate
```

`RiskEngine` computes a 0–100 score from scratch:

| Factor | Trigger | Max impact |
|--------|---------|------------|
| Morning check-in drop | ≥22pp below baseline | +20 |
| Medication adherence drop | ≥12pp below baseline | +23 |
| Activity absence streak | ≥2 consecutive days missed | +26 |
| Meal logging decline | ≥18pp below baseline | +11 |
| Sleep timing drift | >1.5h from baseline avg | +14 |
| Missed appointments | Any scheduled + not attended | +24 (cap) |
| Evening check-in decline | ≥22pp below baseline | +10 |
| Denied evidence penalty | Clinician denied ≥1 item | +15 |
| Multi-signal synergy | ≥2 factors active | +7 |
| Re-engagement bonus | All signals done yesterday | −10 |

A patient's "yes" claim counts by default on submission. It's only excluded if a clinician explicitly denies that evidence item — approving does nothing, denying removes it and adds a penalty.

### Step 3 — State

| Score | State |
|-------|-------|
| 0–24 | STABLE |
| 25–44 | DRIFTING |
| 45–69 | CONCERNING |
| 70–100 | ACUTE_RISK |
| — | RECOVERING (override: prior state was CONCERNING/ACUTE_RISK + last 2 scores strictly decreasing) |

Score ≥80 + ACUTE_RISK → `EscalationService` fires a LEVEL_2 alert via RabbitMQ.

---

## REST API

### Users — `/api/users`
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/users` | List all patients |
| POST | `/api/users` | Create patient |
| POST | `/api/users/with-profile` | Create patient with profile photo |
| GET | `/api/users/patient-accounts` | Patient login list |
| GET | `/api/users/{id}` | Get one patient |
| POST | `/api/users/{id}/support-contacts` | Add support contact |
| GET | `/api/users/{id}/support-contacts` | List support contacts |

### Signals — `/api/users/{id}/signals`
| Method | Path | Description |
|--------|------|-------------|
| POST | `.../daily` | Submit daily signals (JSON) |
| POST | `.../daily-with-evidence` | Submit signals + photos (multipart) |
| GET | `.../signals` | All signal logs |
| GET | `.../signals/recent` | Recent signal logs |

Multipart parts: `payload` (JSON), `morningEvidence`, `medicationEvidence`, `mealEvidence`, `mealEvidence2`, `mealEvidence3`, `activityEvidence`, `eveningEvidence`. Max 12MB per file, 40MB per request.

### Evidence — `/api/evidence`
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/evidence/pending` | Clinician review queue |
| GET | `/api/evidence/signal/{logId}` | Evidence for a specific day |
| POST | `/api/evidence/{id}/approve` | Approve |
| POST | `/api/evidence/{id}/deny` | Deny (removes claim from score) |

### Assessment — `/api/users/{id}`
| Method | Path | Description |
|--------|------|-------------|
| POST | `.../baseline/recalculate` | Compute and save baseline |
| GET | `.../baseline/latest` | Latest baseline |
| POST | `.../risk/recalculate` | Run full risk assessment |
| GET | `.../risk/latest` | Latest assessment result |
| GET | `.../risk/history` | Assessment history |

### Episodes & escalations — `/api/users/{id}`
| Method | Path | Description |
|--------|------|-------------|
| GET | `.../episodes` | All episodes |
| GET | `.../episodes/active` | Active episode |
| GET | `.../interventions` | Intervention records |
| GET | `.../escalations` | Escalation records |
| GET | `.../recovery-summary` | Aggregated summary (used by clinician UI) |

---

## Seeded patients

Five patients pre-loaded on first startup:

| Name | State | Score | Notes |
|------|-------|-------|-------|
| Morgan Ellis | STABLE | 18 | |
| Jordan Lee | DRIFTING | 33 | |
| Alex Thompson | CONCERNING | 58 | Full seed: signals, episode, interventions |
| Samira Okonkwo | ACUTE_RISK | 85 | Welfare popup fires on load |
| Casey Reid | RECOVERING | 36 | |

These are roster-only — no `loginEmail`, can't log in as patients. To create a live demo patient, use **Add New Patient** in the clinician UI.

---

## Clinician portal

- Roster with live state badges, auto-refreshed every 15s
- Patient detail panel, auto-refreshed every 5s
- **Run Assessment** → `POST .../risk/recalculate`
- Evidence queue — approve / deny photo submissions
- Add New Patient modal
- **Acute-Risk Welfare Tray** — when any patient hits ACUTE_RISK, a modal fires asking "Have you dispatched personnel?" with four status options. The patient gets pinned in a pulsing red sidebar tray for the session. Client-side only (sessionStorage).

---

## Config tuning (`application.yml`)

| Key | Default | Meaning |
|-----|---------|---------|
| `baseline.stable-window-days` | 5 | Days used for baseline |
| `baseline.minimum-days-required` | 3 | Minimum before fallback |
| `risk.assessment-window-days` | 7 | Lookback window |
| `risk.recent-window-days` | 5 | Short sub-window for rate factors |
| `risk.episode-open-threshold` | 45 | Score to open an episode |
| `risk.episode-close-threshold` | 30 | Score to close an episode |
| `intervention.dedup-lock-ttl-minutes` | 60 | Redis TTL for dedup locks |
| `reentry.disengagement-streak-threshold` | 4 | Morning-miss streak before re-entry job |

> `ddl-auto: create-drop` — schema is **dropped and recreated on every restart**.

---

## Known limitations

- No server-side auth or RBAC
- No "Recalculate Baseline" button in the UI — have to call the API directly
- `create-drop` wipes data on restart
- Re-engagement bonus uses real `LocalDate.now() - 1`, not a demo date
- Same-day signal merge ORs new booleans with existing — can't undo a `true` the same day
- Welfare check status is not persisted to the backend

---

## Companion docs

- **[DEMO_PLAN.md](DEMO_PLAN.md)** — CW4 video script with timestamps, clicks, and spoken words
- **[REPORT_PLAN.md](REPORT_PLAN.md)** — Technical notes, rubric mapping, coursework context
