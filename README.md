# Recovery Rhythm (CW3 Report)

**Applied Cloud Programming Coursework 3 - Event-Driven Recovery Monitoring Proof of Concept**

Recovery Rhythm is an academic proof-of-concept platform for post-discharge recovery monitoring.  
The system captures daily routine signals from a patient, computes an explainable risk score against a personal baseline, and triggers tiered support workflows for clinicians.

> This project is a software engineering PoC and not a medical device. It does not provide diagnosis, treatment, or clinical advice.

---

## 1. Coursework Context

CW3 asks for an innovative cloud programming solution in ACP scope, with strong justification of:

- innovation and benefit,
- implementation quality,
- completeness,
- and style.

This submission focuses on an **event-driven health support architecture** where each technology has a distinct systems role:

- transactional persistence,
- real-time state handling,
- asynchronous orchestration,
- and event streaming for auditability/extensibility.

---

## 2. Problem Statement and Motivation

Many wellbeing tools are diary-like and reactive. However, in recovery settings, deterioration often appears as **small sustained routine deviations** (missed morning check-ins, sleep drift, reduced activity) rather than one single dramatic event.

Recovery Rhythm addresses this by:

1. building a **personal baseline** from stable days,
2. detecting **pattern drift** in recent data,
3. producing an **explainable risk score** (not black-box ML),
4. launching **graduated support interventions** using messaging systems.

Primary beneficiary: clinical teams who need a compact, interpretable operational view of patient trajectory.  
Secondary beneficiary: patients who receive low-pressure support prompts instead of binary "all-or-nothing" alerts.

---

## 3. High-Level Architecture

```
Patient UI (patient.html)
  -> POST daily signals
  -> POST risk recalculate
      -> Spring Boot API + Services + Engines
         -> PostgreSQL (durable history)
         -> Redis (fast rolling counters/cache/locks)
         -> RabbitMQ (async intervention/escalation jobs)
         -> Kafka (domain event stream)
  -> Clinician UI (clinician.html) polls recovery summary every 5s
```

### 3.1 Core Components

- `controller/`: API surface (`/api/users`, `/signals`, `/risk`, `/recovery-summary`).
- `engine/`: baseline and risk logic (`BaselineEngine`, `RiskEngine`, `StateEngine`).
- `service/`: orchestration layer for persistence, assessment, interventions, and escalation.
- `messaging/`: RabbitMQ publishers/consumers for asynchronous jobs.
- `events/`: Kafka domain event publisher/consumer for event traceability.
- `entity/` + `repository/`: JPA domain model and data access.

---

## 4. Technology Stack and What Each Technology Does

### 4.1 Spring Boot (Java 17)

Spring Boot is the application backbone. It exposes REST APIs, wires dependencies, handles validation, and integrates with PostgreSQL, Redis, RabbitMQ, and Kafka through dedicated starters.

Why it helps:

- rapid integration of multiple cloud-style components,
- clear service-layer architecture suitable for coursework marking,
- production-like structure while remaining compact.

### 4.2 PostgreSQL (System of Record)

PostgreSQL stores durable relational history:

- users and support contacts,
- daily logs,
- baseline snapshots,
- risk assessments,
- intervention records,
- escalation records,
- recovery episodes.

Why it helps:

- guarantees persistence and relational integrity,
- supports clinician audit trail and historical analysis,
- separates durable facts from ephemeral runtime state.

### 4.3 Redis (Fast State and Caching Layer)

Redis stores operational short-horizon state such as:

- streak counters,
- rolling 7-day counts,
- cached risk state/explanations,
- re-entry flags,
- deduplication locks for intervention dispatch.

Why it helps:

- fast reads/writes for repeated risk operations,
- avoids expensive repeated DB scans for streak logic,
- prevents duplicate intervention spam via lock TTL.

### 4.4 RabbitMQ (Asynchronous Action Queue)

RabbitMQ is used to queue intervention/escalation jobs.  
Assessment and support delivery are deliberately decoupled:

1. risk engine decides support action,
2. publisher enqueues message,
3. consumer handles message and persists intervention outcome.

Why it helps:

- keeps risk recalculation responsive,
- supports retries and eventual processing,
- models real distributed system behavior (producer/consumer separation).

### 4.5 Kafka (Domain Event Stream)

Kafka captures significant domain events (signal logged, baseline updated, risk assessed, episode opened/closed, intervention triggered, escalation triggered).

Why it helps:

- creates an append-only event history for observability,
- enables future analytics/stream-processing extensions,
- demonstrates event-driven architecture beyond request-response APIs.

### 4.6 Static Frontend (HTML/CSS/JavaScript)

- `login.html`: role-based demo entry.
- `patient.html`: patient logging workflow with date-based entries.
- `clinician.html`: live monitoring dashboard with periodic refresh.

Why it helps:

- quick to deploy in one Spring Boot artifact,
- enough UI depth for a convincing split-screen demonstration,
- no heavy SPA framework overhead for a timed coursework PoC.

---

## 5. Functional Flow (What Is Going On End-to-End)

### 5.1 Daily Logging Flow

1. Patient selects a date and submits daily routine signals.
2. API stores the signal in PostgreSQL.
3. Redis rolling counters/streak state are updated.
4. Risk recalculation compares recent window vs baseline.
5. New risk assessment is persisted.
6. State transitions (stable/drifting/concerning/etc.) are evaluated.
7. Intervention/escalation jobs may be published to RabbitMQ.
8. Domain events are published to Kafka.
9. Clinician view refreshes and reflects the latest state.

### 5.2 Baseline and Explainability

The baseline is computed from stable days and includes rates (morning, medication, activity, etc.) and average sleep timing.  
Each risk assessment returns:

- score (0-100),
- state label,
- concise summary,
- detailed explanation,
- named contributing factors with impact/severity.

This improves interpretability in an academic setting because the assessor can trace "why score changed" rather than seeing an opaque number.

---

## 6. Recovery States Used in the System

- `STABLE` (0-24): behavior aligned with baseline.
- `DRIFTING` (25-44): early deviation, monitor closely.
- `CONCERNING` (45-69): significant multi-signal drift.
- `ACUTE_RISK` (70-100): critical breakdown; escalation threshold.
- `RECOVERING`: downward trend after high-risk period.

---

## 7. Seeded Demo Data

On startup, `DataInitializer` seeds:

- **Alex Thompson**: full historical trajectory across stable, drifting, acute, and recovering phases.
- **Eric Ng**: fresh patient profile intended for live logging demonstration.

Demo accounts:

- Clinician: `clinician@rr.nhs.uk` / `demo123`
- Patient: `eric@patient.com` / `demo123`

---

## 8. Build and Run Instructions

### 8.1 Recommended: Docker Compose

```bash
docker compose up --build
```

Services exposed:

- App: `http://localhost:8080/login.html`
- RabbitMQ management: `http://localhost:15672` (`guest` / `guest`)
- PostgreSQL: `localhost:5432`
- Redis: `localhost:6379`
- Kafka: `localhost:9092`

### 8.2 Local (Without Docker)

Requirements:

- Java 17
- Maven
- Running PostgreSQL, Redis, RabbitMQ, Kafka

Set environment variables used in `application.yml`:

- `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`
- `REDIS_HOST`, `REDIS_PORT`
- `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USER`, `RABBITMQ_PASS`
- `KAFKA_BOOTSTRAP_SERVERS`

Run:

```bash
mvn spring-boot:run
```

---

## 9. Key API Endpoints

- `GET /api/users`
- `POST /api/users/{id}/signals/daily`
- `GET /api/users/{id}/signals`
- `POST /api/users/{id}/baseline/recalculate`
- `POST /api/users/{id}/risk/recalculate`
- `GET /api/users/{id}/risk/latest`
- `GET /api/users/{id}/recovery-summary`
- `GET /api/users/{id}/episodes/active`
- `GET /api/users/{id}/interventions`
- `GET /api/users/{id}/escalations`

---

## 10. Mapping to CW3 Rubric

### 10.1 Innovation / Idea / Benefit

- Personal baseline drift detection (not population averaging).
- Explainable risk factors instead of black-box output.
- Clinician-patient dual view showing operational impact in real time.
- Tiered intervention design with asynchronous queueing.

### 10.2 Execution / Implementation

- Structured multi-layer Spring architecture.
- Correct role separation of PostgreSQL/Redis/RabbitMQ/Kafka.
- Event publication + async consumers + dashboard integration.

### 10.3 Completeness

- End-to-end flow implemented from input to dashboard update.
- Historical seed trajectory plus live patient scenario.
- Working deployment via Docker Compose.

### 10.4 Style

- Consistent API design and package layout.
- Production-style concerns (state caching, decoupling, eventing).
- Polished UI for coursework demonstration and clear storytelling.

---

## 11. AI Usage Statement

AI assistance was used to accelerate iterative development, code drafting, and documentation refinement.  
All architecture choices, technology selection, integration decisions, and final validation were reviewed and controlled by the student.  
AI improved speed and writing quality but did not replace implementation ownership or system-level reasoning.

---

## 12. Limitations and Future Work

Current PoC limitations:

- no real authentication backend (demo-only login),
- no real SMS/email connector (interventions are persisted/logged),
- no wearable/device ingestion,
- schema reset behavior (`ddl-auto: create-drop`) is for demo convenience.

Potential extensions:

- production auth and role-based access control,
- stream analytics (e.g., Flink) over Kafka topics,
- notification provider integration,
- temporal smoothing and personalization upgrades in risk modeling.

---

## 13. Notes for `cw3_explanation.pdf`

Use this README as source material, then produce a focused PDF essay (max 1,000 words) covering:

1. Why this problem matters and who benefits.
2. Why each technology was chosen and what role it plays.
3. How the implementation works end-to-end.
4. What is complete vs what remains PoC.
5. How AI influenced the process.

Keep the PDF concise, evidence-based, and aligned with rubric language.

---

**University of Edinburgh - Applied Cloud Programming (CW3)**  
**Project:** Recovery Rhythm
