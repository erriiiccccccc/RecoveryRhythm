# Recovery Rhythm Demo Plan (Split-Screen Recording)

This guide is designed for your coursework video demonstration.

---

## 1. Recording Setup

Use two browser windows side-by-side:

- **Left window:** Clinician portal (`clinician.html`) logged in as hospital personnel.
- **Right window:** Patient portal (`patient.html`) logged in as Eric Ng.

Suggested opening line:

> "This is a proof-of-concept showing how daily patient logs flow through an event-driven cloud architecture and appear in near real-time for clinicians."

---

## 2. Login and Initial State

1. Open `http://localhost:8080/login.html` in both windows.
2. Left login:
   - Email: `clinician@rr.nhs.uk`
   - Password: `demo123`
3. Right login:
   - Email: `eric@patient.com`
   - Password: `demo123`
4. In left window, select **Eric Ng** from patient list.

Narration:

> "Eric starts as a fresh patient profile. The clinician side auto-refreshes every 5 seconds, so any patient-side check-in should propagate quickly."

---

## 3. Why Date Selection Exists in the Demo

Say this explicitly in your video:

> "The date picker is included for proof-of-concept demonstration so I can simulate multiple days quickly. In real deployment, this would usually be one real check-in per calendar day, with reminders and stricter time logic."

---

## 4. Day-by-Day Script (Day 1 to Day 5)

Each day below includes:

- what to enter on patient side,
- expected clinician-side effect,
- and what to explain about technology flow.

Use the date picker on the right window and submit one day at a time.

### Day 1 (Healthy Baseline Start)

Patient inputs:

- Morning check-in: **Yes**
- Medication: **Yes**
- Meal logged: **Yes**
- Activity: **Yes**
- Evening check-in: **Yes**
- Appointment scheduled: **No**
- Sleep: **11:00 PM**
- Notes: "Felt okay, followed routine."

What to explain on clinician side:

- score should be low/stable trend,
- first signal appears in recent history,
- baseline-building behavior begins.

Technology explanation:

- API stores signal in **PostgreSQL**,
- streak/rolling counters update in **Redis**,
- risk assessment event emitted to **Kafka**,
- summary endpoint refreshes in clinician UI.

### Day 2 (Still Stable)

Patient inputs:

- Morning: **Yes**
- Medication: **Yes**
- Meal: **Yes**
- Activity: **Yes**
- Evening: **Yes**
- Appointment scheduled: **Yes**
- Appointment attended: **Yes**
- Sleep: **10:00 PM**
- Notes: "Routine maintained, attended appointment."

What to explain on clinician side:

- stable pattern continues,
- history now has two logged days,
- baseline confidence improves.

Technology explanation:

- additional relational records written in **PostgreSQL**,
- appointment adherence contributes to risk context,
- clinician dashboard updates from refreshed `/recovery-summary`.

### Day 3 (Slight Deterioration Starts)

Patient inputs:

- Morning: **No**
- Medication: **Yes**
- Meal: **Yes**
- Activity: **No**
- Evening: **No**
- Appointment scheduled: **No**
- Sleep: **1:00 AM**
- Notes: "Felt tired and skipped activity."

What to explain on clinician side:

- risk should rise toward drifting,
- factors likely mention morning/activity/sleep drift,
- dashboard now shows mixed green/red signal pattern.

Technology explanation:

- changed behavior increases feature deltas vs baseline,
- **RiskEngine** computes explainable contributing factors,
- if threshold crossed, intervention job may be queued to **RabbitMQ**.

### Day 4 (Noticeably Worse)

Patient inputs:

- Morning: **No**
- Medication: **No**
- Meal: **No**
- Activity: **No**
- Evening: **No**
- Appointment scheduled: **Yes**
- Appointment attended: **No**
- Sleep: **2:00 AM**
- Notes: "Very difficult day, stayed in bed."

What to explain on clinician side:

- score should increase further (concerning direction),
- contributing factors list should become more severe,
- support actions/interventions may appear.

Technology explanation:

- intervention and escalation preparation are asynchronous via **RabbitMQ**,
- support records are persisted in **PostgreSQL**,
- event trail continues in **Kafka**.

### Day 5 (Critical Day / Escalation Demonstration)

Patient inputs:

- Morning: **No**
- Medication: **No**
- Meal: **No**
- Activity: **No**
- Evening: **No**
- Appointment scheduled: **No**
- Sleep: **3:00 AM**
- Notes: "Could not manage today."

What to explain on clinician side:

- likely high-risk state (concerning/acute, depending on current baseline/history),
- stronger interventions and possible escalation visibility,
- this validates early-warning and action orchestration.

Technology explanation:

- risk threshold logic in `RiskEngine` drives state transition,
- escalation/intervention jobs are produced and consumed asynchronously,
- clinician view reflects latest persisted records plus recalculated state.

---

## 5. Live Narration Template (Per Day)

Use this concise structure each time:

1. "I am now logging Day X on the patient side."
2. "I submit the check-in and trigger risk recalculation."
3. "On the clinician side, we can see the new state, score, and factors."
4. "Under the hood: PostgreSQL stores history, Redis maintains fast rolling state, RabbitMQ handles asynchronous support jobs, and Kafka records domain events."

---

## 6. How to Explain "What Goes Where"

- **Daily check-in payload** -> REST API -> persisted in `DailySignalLog` (PostgreSQL).
- **Recent behavior deltas and streaks** -> Redis keys/counters for rapid access.
- **Risk result** -> `RiskAssessment` record (PostgreSQL) + cached state (Redis).
- **Intervention/escalation actions** -> RabbitMQ messages -> consumer -> `InterventionRecord` / `EscalationRecord`.
- **Domain traceability** -> Kafka event topics for system-wide audit/extensibility.
- **UI reflection** -> Clinician page polls and renders latest summary every 5 seconds.

---

## 7. Strong Closing Statement for Video

> "This demonstrates an end-to-end cloud programming solution where patient behavior signals are transformed into explainable clinical insight and asynchronous support actions. Although it is a proof-of-concept, the architecture is intentionally production-oriented and extensible."

