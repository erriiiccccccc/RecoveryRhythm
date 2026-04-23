# Recovery Rhythm — Applied Cloud Programming (CW3)

**Event-driven post-discharge recovery monitoring — proof of concept**

Recovery Rhythm is a course submission demonstrating how **routine signals** from a patient, together with **clinician intake** and **verifiable photo evidence**, flow through a **multi-service architecture** to produce an **interpretable risk score** and **asynchronous support actions**. A **clinician portal** and **patient portal** are intended to run **side by side** for live demonstration.

> **Disclaimer:** This is a software engineering proof of concept, not a medical device. It does not provide diagnosis, treatment, or clinical advice. **Authentication is client-side** (`sessionStorage`) for demo only.

---

## Table of contents

1. [Executive summary](#1-executive-summary)  
2. [Coursework alignment](#2-coursework-alignment)  
3. [Problem and motivation](#3-problem-and-motivation)  
4. [What the system does](#4-what-the-system-does)  
5. [Architecture and packages](#5-architecture-and-packages)  
6. [Evidence, S3, and risk (verified vs raw)](#6-evidence-s3-and-risk-verified-vs-raw)  
7. [Risk model (engine summary)](#7-risk-model-engine-summary)  
8. [Recovery states](#8-recovery-states)  
9. [Technology stack](#9-technology-stack)  
10. [Seeded data and demo patients](#10-seeded-data-and-demo-patients)  
11. [Build, run, and configuration](#11-build-run-and-configuration)  
12. [Operational tuning (`application.yml`)](#12-operational-tuning-applicationyml)  
13. [Ports and URLs](#13-ports-and-urls)  
14. [REST API (complete)](#14-rest-api-complete)  
15. [Messaging and events (actual names)](#15-messaging-and-events-actual-names)  
16. [Front ends](#16-front-ends)  
17. [Rubric mapping](#17-rubric-mapping)  
18. [AI usage](#18-ai-usage)  
19. [Limitations and future work](#19-limitations-and-future-work)  
20. [Companion documents](#20-companion-documents)

---

## 1. Executive summary

| Aspect | Description |
|--------|-------------|
| **Idea** | Compare **recent** daily behaviour (and verified claims) to a **baseline snapshot**; aggregate weighted **factors** into a 0–100 **risk score**; map score + history to a **RecoveryState**; open/close **episodes**; queue **interventions** / **escalations**; emit **Kafka** events. |
| **Stack** | Spring Boot **3.2.0** (Java **17**), PostgreSQL, Redis, RabbitMQ, Apache Kafka, AWS SDK v2 → **S3** (LocalStack in Docker for dev). |
| **Demo** | **Five** seeded roster users (one per `RecoveryState`); **Eric** (or any real patient) is **added in the UI** — see [DEMO_PLAN.md](DEMO_PLAN.md). |

---

## 2. Coursework alignment

CW3 rewards **innovation**, **implementation quality** (real integrations), **completeness** of a credible story, and **presentation**. Each major infrastructure component has a **defined role** (relational truth, object storage, hot state, work queues, event log).

---

## 3. Problem and motivation

Sustained **routine drift** (sleep, check-ins, meals, activity, adherence) is often how difficulty appears after discharge. The system is built to (a) **store** what happened, (b) **verify** where photo proof matters, and (c) show **why** a score changed via named **contributing factors**.

---

## 4. What the system does

- **Clinicians:** list users, create patients (`POST` JSON or `POST` multipart **with profile photo** to S3), add support contacts, read **recovery summary** (aggregated), review **evidence** (approve / deny with optional reason), run **`POST /risk/recalculate`** via **Run Assessment** in `clinician.html`.  
- **Patients:** submit daily signals via **`POST /signals/daily-with-evidence`** (multipart) as implemented in `patient.html`.  
- **No clinician UI** currently calls **`POST /baseline/recalculate`**. A **baseline snapshot** (used for comparative risk) is **not** created when a user is first registered; it is produced when `BaselineService.recalculate` runs (see §6, §10, [DEMO_PLAN.md](DEMO_PLAN.md)).  
- **Interventions** go through **RabbitMQ**; consumers persist outcomes; **Redis** is used for deduplication locks; **Kafka** records domain events.

---

## 5. Architecture and packages

```text
patient.html / clinician.html
  →  REST (JSON + multipart)
  →  Spring Boot: controller → service → repository / S3 / Redis / Rabbit / Kafka
  →  PostgreSQL (entities)
  →  S3  (evidence + profile object bytes)
  →  Redis, RabbitMQ, Kafka
```

| Package / area | Role |
|----------------|------|
| `controller/` | All REST: `User`, `Signal`, `Evidence`, `Assessment`, `Episode`, `RecoverySummary`. |
| `service/` | Business logic, S3, Redis, interventions, episodes, risk/baseline. |
| `engine/` | `BaselineEngine`, `RiskEngine`, `StateEngine`. |
| `messaging/` | Rabbit publish + consumer. |
| `events/` | `KafkaEventPublisher`, `KafkaEventConsumer`, `DomainEvent`. |
| `config/` | Redis, Kafka, RabbitMQ, S3, Jackson. |
| `init/` | `DataInitializer`, `S3BucketInitializer`. |

**Repository:** 75 Java files under `uk.ac.ed.inf.recoveryrhythm` in tree (see IDE); controllers are the external contract.

---

## 6. Evidence, S3, and risk (verified vs raw)

| Concern | Behaviour in code |
|---------|-------------------|
| **Storage** | Evidence files: `EvidenceService` + `S3StorageService` → S3. Profile images: `UserService` + `S3StorageService` on `with-profile` create. `S3BucketInitializer` runs on startup (bucket). |
| **Metadata** | `SignalEvidence` rows in **PostgreSQL**; statuses `PENDING` / `APPROVED` / `DENIED`. |
| **Risk** | `RiskEngine.isEffectiveClaim(...)`: a claimed signal counts toward rates/streaks if the day is **fully** `VERIFIED` **or** that signal type has **at least one** `APPROVED` evidence record (`countByDailySignalLogAndSignalTypeAndStatus` — **approved &gt; 0** for that type). **Pending** evidence does **not** make a “yes” claim effective. |
| **Baseline** | `BaselineEngine` computes rates from **raw** `DailySignalLog` booleans (e.g. `isMorningCheckInCompleted()`), **not** the same effective-claim logic as `RiskEngine`. For a new patient, `BaselineEngine.calculate` may fall back to **synthetic** defaults when there are no logs (`emptyBaseline`). |
| **Denied evidence** | `RiskEngine` adds a **denied evidence** factor when count of `DENIED` in the assessment window is &gt; 0. |

**Multipart limits** (see `application.yml`): `max-file-size: 12MB`, `max-request-size: 40MB`. The patient page also enforces a **10MB** per-file check client-side.

---

## 7. Risk model (engine summary)

`RiskEngine` (see class-level comment) uses:

- `recovery-rhythm.risk.assessment-window-days` (default **7**): which `DailySignalLog` rows are loaded — **`logDate` from `LocalDate.now().minusDays(7)` to `LocalDate.now()`** inclusive.  
- `recovery-rhythm.risk.recent-window-days` (default **5**): subset of those logs for some “short window” rates.

**Implication for demos:** the **date picker** simulates a calendar day, but the engine’s **inclusion in the 7-day window** is always measured against the **real system clock** (`LocalDate.now()`). Logs dated **earlier than seven days before today** are **excluded** from the next assessment. See [DEMO_PLAN.md](DEMO_PLAN.md).

**Representative factors** (non-exhaustive; see `RiskEngine` for the full set):  
morning / medication / meal / evening **rate drop vs baseline** (percentage-point thresholds are **moderately tight** — e.g. morning ≥22pp, medication ≥12pp, meals ≥18pp, evening ≥22pp), **activity absence streak** from the **most recent** log-days backward (≥2 days without effective activity; stronger at ≥3 / ≥5), **sleep timing drift** if average sleep shifts **&gt;1.5h** from baseline, **missed appointments** (12 per miss, cap 24), **multi-signal synergy** (+7 when **≥2** primary factors are present), **re-engagement bonus** (negative), **denied evidence** penalty.

Each **Run Assessment** recomputes **`totalScore` from scratch** from these factors — it does **not** add on top of the previous number. The **25–44** “DRIFTING” band is only a **label** for whatever total the engine produced last time.

`totalScore` is clamped to **0–100**.

---

## 8. Recovery states

`StateEngine` score bands:

| Score | State |
|------|--------|
| 0–24 | `STABLE` |
| 25–44 | `DRIFTING` |
| 45–69 | `CONCERNING` |
| 70–100 | `ACUTE_RISK` |

**`RECOVERING`:** if the **base** state from the score is `STABLE` or `DRIFTING`, **and** the **previous** state was `CONCERNING` or `ACUTE_RISK`, and the last **two** stored assessments (history order: newest first) show a **strictly decreasing** risk score, then the label becomes `RECOVERING` instead of `STABLE`/`DRIFTING`.

**Escalation** (`RiskEngine` tail): an escalation is only triggered for **`ACUTE_RISK` with score ≥ 80** (and `EscalationService`).

**Episodes** use `episode-open-threshold` (default 45) and `episode-close-threshold` (30) from config (`EpisodeService` / `application.yml`).

---

## 9. Technology stack

| Technology | Role |
|------------|------|
| **Spring Boot 3.2, Java 17** | Application server, validation, JPA, integrations. |
| **PostgreSQL** | Authoritative data: users, signals, baselines, assessments, evidence, episodes, interventions, escalations, etc. |
| **Redis** | `RedisStateService`: streaks, cached risk, intervention **deduplication** locks, re-entry mode flags (TTL in config). |
| **RabbitMQ** | `recovery.exchange` (direct) → queues in §15. |
| **Kafka** | String JSON events on **topic name = event type** (see §15). KRaft in `docker-compose` (no Zookeeper). |
| **S3 (LocalStack)** | Object storage; credentials `test`/`test` in compose (dev only). |
| **Docker Compose** | Full stack: **postgres, redis, kafka, rabbitmq, localstack, app** (see [docker-compose.yml](docker-compose.yml)). |

---

## 10. Seeded data and demo patients

On first run, `DataInitializer` loads **five** users (clinician roster only — no `loginEmail`):

- **Morgan Ellis** — STABLE (18)  
- **Jordan Lee** — DRIFTING (33)  
- **Alex Thompson** — CONCERNING (58); full **seed** (signals, history, **active** episode, interventions, escalation)  
- **Samira Okonkwo** — ACUTE_RISK (85)  
- **Casey Reid** — RECOVERING (36)  

**Live patient (e.g. Eric):** create via **Add New Patient**; not in the seed.

**Clinician login** (not in DB): `clinician@rr.nhs.uk` / `demo123` in `login.html`.  

`RiskEngine` only looks at logs in the **last 7 real calendar days**; seed dates are in **~April 2026**. The UI can still show **pre-seeded** summary/trend. For a **new** user’s comparative scores, run **`POST /api/users/{id}/baseline/recalculate`** once (no button in the UI) — see [DEMO_PLAN.md](DEMO_PLAN.md).

**Summary metrics** (`RecoverySummaryController`): `daysSinceRecoveryStart` = `ChronoUnit.DAYS` between `recoveryStartDate` and **`LocalDate.now()`**; `loggedDaysCount` = **total** `DailySignalLog` rows for the user (not “last 7 days”).

---

## 11. Build, run, and configuration

### Full stack (recommended)

```bash
docker compose up --build
```

App: **http://localhost:8080/login.html** (or `/clinician.html` / `/patient.html` with session from login).

### NPM scripts (see [package.json](package.json))

| Script | Command |
|--------|---------|
| `dev:infra-up` | `docker compose up -d postgres redis kafka rabbitmq localstack` |
| `dev:app` | `mvn spring-boot:run` |
| `dev` | infra then app |
| `dev:infra-down` | `docker compose stop …` |
| `dev:infra-reset` | `docker compose down -v` (wipes DB volume) |
| `build` | `mvn clean package` |
| `test` | `mvn test` |

### Local (no app container)

Run Postgres, Redis, RabbitMQ, Kafka, and LocalStack (or S3) per `application.yml` defaults, then `mvn spring-boot:run`. Set env to match: `POSTGRES_*`, `REDIS_*`, `RABBITMQ_*`, `KAFKA_BOOTSTRAP_SERVERS`, `S3_*`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`.

---

## 12. Operational tuning (`application.yml`)

| Key (prefix `recovery-rhythm.`) | Default | Meaning |
|---------------------------------|---------|---------|
| `baseline.stable-window-days` | 5 | `BaselineEngine` window from `recoveryStartDate` (with fallback to recent logs if sparse). |
| `baseline.minimum-days-required` | 3 | Floors before using “recent logs” for baseline. |
| `risk.assessment-window-days` | 7 | Load logs for this many days ending **today** (inclusive of both ends in query). |
| `risk.recent-window-days` | 5 | Shorter sub-window for some rates. |
| `risk.episode-open-threshold` | 45 | Episode open when risk crosses up. |
| `risk.episode-close-threshold` | 30 | Close when under. |
| `risk.recovering-stable-days` | 3 | Present in config; **not** referenced in `EpisodeService` in the current code (episode open/close uses state logic in that class). |
| `intervention.dedup-lock-ttl-minutes` | 60 | Redis lock for intervention de-dupe. |
| `reentry.disengagement-streak-threshold` | 4 | Morning-miss streak before re-entry job path. |
| `s3.*` | endpoint/region/bucket/keys | S3 access (LocalStack: `http://localhost:4566` from host, `http://localstack:4566` in Docker network). |

JPA: `spring.jpa.hibernate.ddl-auto: create-drop` (dev; **restarts drop schema**).

---

## 13. Ports and URLs

| Service | Port / URL |
|---------|------------|
| App | `http://localhost:8080` |
| PostgreSQL | `5432` (user `rhythm`, db `recoveryrhythm` in compose) |
| Redis | `6379` |
| Kafka | `9092` |
| RabbitMQ AMQP | `5672` |
| RabbitMQ management | `http://localhost:15672` — `guest` / `guest` |
| LocalStack | `http://localhost:4566` |

---

## 14. REST API (complete)

**Base** paths — all are under the shown prefixes.

### Users — `UserController` → `/api/users`

- `GET /api/users`  
- `POST /api/users` (JSON `CreateUserRequest`)  
- `POST /api/users/with-profile` (multipart: `payload` + optional `profilePhoto`)  
- `GET /api/users/patient-accounts`  
- `GET /api/users/{id}`  
- `POST /api/users/{id}/support-contacts`  
- `GET /api/users/{id}/support-contacts`  

### Signals — `SignalController` → `/api/users/{userId}/signals`

- `POST /api/users/{userId}/signals/daily`  
- `POST /api/users/{userId}/signals/daily-with-evidence` (multipart; parts `payload`, `morningEvidence`, `medicationEvidence`, `mealEvidence`, `mealEvidence2`, `mealEvidence3`, `activityEvidence`, `eveningEvidence`)  
- `GET /api/users/{userId}/signals`  
- `GET /api/users/{userId}/signals/recent`  

### Evidence — `EvidenceController` → `/api/evidence`

- `GET /api/evidence/pending`  
- `GET /api/evidence/signal/{signalLogId}`  
- `POST /api/evidence/{evidenceId}/approve` (optional body `EvidenceVerificationRequest`)  
- `POST /api/evidence/{evidenceId}/deny`  

### Assessment — `AssessmentController` → `/api/users/{userId}`

- `POST /api/users/{userId}/baseline/recalculate`  
- `GET /api/users/{userId}/baseline/latest`  
- `POST /api/users/{userId}/risk/recalculate`  
- `GET /api/users/{userId}/risk/latest`  
- `GET /api/users/{userId}/risk/history`  

### Episodes, interventions, escalations — `EpisodeController` → `/api/users/{userId}`

- `GET /api/users/{userId}/episodes`  
- `GET /api/users/{userId}/episodes/active`  
- `GET /api/users/{userId}/interventions`  
- `GET /api/users/{userId}/escalations`  

### Recovery summary — `RecoverySummaryController` → `/api/users/{userId}/recovery-summary`

- `GET /api/users/{userId}/recovery-summary` (single DTO, used by `clinician.html`)

---

## 15. Messaging and events (actual names)

### RabbitMQ (`RabbitMqConfig`)

- **Exchange:** `recovery.exchange` (direct)  
- **Queues:** `recovery.interventions`, `recovery.escalations`, `recovery.reentry`  
- **Routing keys:** `intervention`, `escalation`, `reentry`  

### Kafka (`KafkaEventPublisher` topic = first argument, same as event “type”)

Examples: `routine.signal.logged`, `baseline.updated`, `risk.assessed`, `recovery.state.changed`, `recovery.episode.opened`, `recovery.episode.closed`, `intervention.triggered`, `escalation.triggered`.  
Payloads are JSON `DomainEvent` with `userId` and map fields.

`SignalService.logDailySignal` publishes **`routine.signal.logged`**. `RiskEngine.assess` publishes **`risk.assessed`** and optionally **`recovery.state.changed`**.

---

## 16. Front ends

| File | Purpose |
|------|---------|
| `login.html` | Email/password, role, sessionStorage `rr_session`, redirects. |
| `clinician.html` | Roster, patient detail, **Run Assessment** → `POST .../risk/recalculate` only, evidence modals, **Add patient**. Polling: **~5s** for selected-patient details, **~15s** roster `loadPatients`, **~2s** evidence queue refresh (see `setInterval` in file). |
| `patient.html` | Daily check-in with `daily-with-evidence` only. |
| `index.html` | If opened directly, redirects to login or to clinician/patient from session. |

There is **no** separate public SPA build; everything is static under Spring Boot’s resource path.

---

## 17. Rubric mapping

| Criterion | Evidence in repo |
|----------|-------------------|
| **Innovation** | Multi-signal, evidence, baseline + risk, event stream, queues. |
| **Implementation** | JPA, Redis, S3, Rabbit, Kafka, Dockerized stack. |
| **Completeness** | Seeded + live-create path; async consumers; S3 init. |
| **Style** | Consistent DTOs, `controller` / `service` / `engine` split. |

---

## 18. AI usage

AI assistance was used to accelerate drafting and refactoring. **Design decisions, selection of technologies, integration, and final checks** are the author’s. AI is a tool for speed and text quality, not a substitute for understanding the submission.

---

## 19. Limitations and future work

- **No server-side auth** / RBAC.  
- **Not** a regulated medical device.  
- **Clinician UI** has **no** “Recalculate baseline” button.  
- **Same-day** signal merge: `SignalService` **OR**s new booleans with existing; cannot easily “undo” a `true` in the same day without a different API.  
- **Re-engagement** bonus in `RiskEngine` keys off **`LocalDate.now().minusDays(1)`** (real “yesterday”), not the last simulated day in a backdated demo.  
- `ddl-auto: create-drop` **wipes** DB on each restart.  
- **Future:** production auth, notification providers, stream analytics, HITL/AI governance for evidence, UI for baseline, align baseline computation with `isEffectiveClaim` if desired.

---

## 20. Companion documents

- **[DEMO_PLAN.md](DEMO_PLAN.md)** — Split-screen, **seeded roster** (optional) + **Eric** flow, **date rules**, **baseline** curl, day script.  
- **`cw3_explanation.pdf`** — Short reflective essay; use README for technical fact-checking.

---

**University of Edinburgh — Applied Cloud Programming (CW3)**  
**Project:** Recovery Rhythm
