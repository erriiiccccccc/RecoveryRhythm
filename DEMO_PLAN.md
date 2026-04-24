# Recovery Rhythm — CW4 Video Script (Complete)

**Target: 9–10 minutes. Arc: stable → baseline → collapse → ACUTE_RISK → welfare check popup fires live.**

---

## CW4 Marking at a glance

| Criterion | Marks | How to hit it |
|-----------|-------|---------------|
| Background / target audience | 2 | §A — first 50s after the hands |
| Problem statement | 3 | §B — the "gap" framing, routine drift |
| Explanation of choices | 5 | §G — why each infra component, why baseline, why evidence, why state engine |
| Presentation | 5 | Face cam on throughout, point at things, vary pace, don't read |

**Hard rules:**
- MP4 format
- 9–11 min (10% off each minute over; 0 for >11 min — hard stop at 11:00)
- First 30s: face + student ID **s2411221** visible + both hands spread and moved left-right in front of face
- Face visible through fingers

---

## Before you record — prepare these NOW

### 1. Log dates
Fill this in for your actual recording date:

| Story day | Formula | Your date |
|-----------|---------|-----------|
| Day 1 | today − 4 | __________ |
| Day 2 | today − 3 | __________ |
| Day 3 | today − 2 | __________ |
| Day 4 | today − 1 | __________ |
| Day 5 | today | __________ |

All five must be within the last 7 real calendar days or `RiskEngine` will ignore them.

### 2. Evidence photos
Prepare **5 small files** (< 1 MB each, JPEG/PNG — phone photos are fine). You can reuse the same image for all of them. Label them mentally: `morning.jpg`, `meds.jpg`, `meal.jpg`, `activity.jpg`, `evening.jpg`. The content doesn't matter, just that they upload.

### 3. Eric's UUID
After creating Eric you need his UUID for the baseline curl. Plan to grab it from the **Network tab** in DevTools (look at the response from `POST /api/users/with-profile`) or from `GET /api/users`. Pre-type the curl with a placeholder:

**PowerShell:**
```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/users/PASTE_UUID_HERE/baseline/recalculate"
```

**Git Bash / WSL:**
```bash
curl -s -X POST "http://localhost:8080/api/users/PASTE_UUID_HERE/baseline/recalculate"
```

### 4. Screen layout
- Chrome window **left ~60%**: `http://localhost:8080/clinician.html` (not logged in yet)
- Chrome window **right ~40%**: `http://localhost:8080/patient.html` (not logged in yet)
- Terminal / PowerShell: minimised, ready for baseline curl
- DevTools Network tab: open on the clinician window
- Face cam: corner overlay (OBS / built-in screen recorder PiP)

### 5. Fresh app state
```bash
npm run dev:infra-reset   # wipes DB — do this before recording
docker compose up --build
```
Wait until app is live at `http://localhost:8080/login.html`. Confirm `eric@patient.com` does **not** exist yet.

### 6. Practice runs
Do at least **two full dry runs** before recording. The timing is tight. Key moments to rehearse:
- The welfare popup fires 700ms after roster load — don't be surprised, have your line ready
- The Day 3 baseline curl — tab-switch to terminal, paste UUID, run, tab back
- Day 5 Run Assessment → score spikes → popup fires for Eric — pause, react, explain

---

## Technical gotchas (read once, know cold)

| Gotcha | What it means | What to say / do |
|--------|--------------|------------------|
| RiskEngine uses real clock | `logDate` must be within 7 real days of recording date | Use your pre-planned dates (§above) |
| No baseline → flat score | Comparative factors skipped until `POST /baseline/recalculate` runs | Run curl after Day 3, before explaining the jump |
| Evidence PENDING doesn't count | `isEffectiveClaim` needs ≥1 APPROVED for each signal type | Approve everything on the clinician side before Run Assessment |
| OR-merge on re-submit | Can't un-tick a signal for the same logDate | One submit per date, in order |
| Score is recalculated from zero | Each Run Assessment replaces the number, not adds to it | Say "it recomputes from scratch each time" |
| Welfare popup fires once per patient | `prompted` flag prevents repeat popups | If popup doesn't fire again: click the tray item to reopen |
| RECOVERING state requires trend | Needs prior state CONCERNING/ACUTE + last two assessments decreasing | Don't promise RECOVERING — it may not appear |

---

## Window & score expectations by moment

| Moment | Expected score range | Expected state |
|--------|---------------------|----------------|
| After Day 1 (no baseline) | 0–15 | STABLE |
| After Day 2 (no baseline) | 0–15 | STABLE |
| After Day 3 + baseline | 0–12 | STABLE |
| After Day 4 crash | 30–50 | DRIFTING → CONCERNING |
| After Day 5 crash + 2 missed appts | 70–100 | **ACUTE_RISK** |

If Day 5 doesn't hit ACUTE_RISK: check that baseline ran, both appointments were marked "No", and all Day 4/5 toggles are off.

---

## Full video script

---

### [0:00 – 0:28] REQUIRED: Face + ID + hands

**Screen:** Camera only or camera prominent

1. Show face and student ID card / phone screen with **s2411221**
2. Say: **"s2411221"** out loud
3. Left hand — spread all five fingers — move slowly left to right in front of your face, then back — face must be visible through fingers
4. Right hand — same
5. Transition straight into intro

**Say exactly:**
> "Student ID s2411221. I built Recovery Rhythm — a post-discharge monitoring system for Applied Cloud Programming."

---

### [0:28 – 1:20] §A — Background: who, why, what domain

**Screen:** Camera still visible (face cam corner). Slowly reveal the app login screen.

**Say:**
> "After psychiatric discharge — hospital back to home — patients are given a medication plan and a routine to follow. This is statistically the most dangerous period. Routine drift happens quietly and gradually: a missed morning check-in, skipped medication, a meal not eaten, sleep pushed back by an hour. None of these alone looks alarming. Together, over five or six days, they can signal a crisis forming."

> "The people this system is built for are community mental health teams — clinicians who are responsible for ten, twenty, thirty patients between appointments. They can't call every patient every day. They need a signal that something is wrong before the next scheduled contact."

> "Recovery Rhythm is that signal. It's a cloud-native monitoring platform that captures daily patient behaviour, runs it through a personalised risk model, and surfaces the result — with an explanation — to the clinician."

---

### [1:20 – 2:00] §B — Problem statement + solution pitch

**Screen:** Keep camera visible. Point to the split screen.

**Say:**
> "The specific problem I tackled: how do you build a system that doesn't just store what a patient reported, but can verify it, compare it against that individual's own baseline, explain why the score changed, and trigger an escalation path when things go critically wrong?"

> "Three technical problems sit inside that: evidence trust — how do you stop a patient gaming the score by claiming 'yes' to everything? Personalised comparison — population averages are clinically useless for this; you need to compare Eric to Eric. And real-time escalation — when the score crosses a threshold, the system needs to act and demand a response from the clinician."

> "Let me show you how I solved all three."

---

### [2:00 – 2:25] Login + one-line stack intro

**Screen:** Focus on left panel (clinician.html)

**Action:** Navigate to `http://localhost:8080/login.html`. Fill in:
- Email: `clinician@rr.nhs.uk`
- Password: `demo123`
- Role: **Clinician**
- Click **Sign In**

**Say (while filling in):**
> "Logging in as the demo clinician. The stack running this: Spring Boot REST API on port 8080, PostgreSQL as the source of truth, S3 via LocalStack for photo byte storage, Redis for operational state, RabbitMQ for async intervention jobs, and Kafka for domain event streaming. Each one has a defined role — I'll explain the choices at the end."

---

### [2:25 – 3:10] Seeded roster tour + Samira welfare popup

> **⚠️ KEY MOMENT**: The 🚨 welfare check popup fires automatically ~700ms after the roster loads. Have your line ready. Don't dismiss it yet — talk over it first.

**[Popup fires for Samira, score 85, ACUTE_RISK]**

**Say immediately:**
> "Before I even click on a patient — the portal has already scanned the roster and detected Samira Okonkwo in ACUTE_RISK, score 85. This welfare check prompt fires automatically. In a real deployment this would be a push notification or server-sent event. Here, it fires from the patient-list poll 700 milliseconds after load."

> "The options are: not dispatched yet, dispatched and en route, incoming, or arrived and checked in. I'll mark her as dispatched and en route."

**Action:** Click **"Dispatched — not yet arrived"** (it turns amber/selected)

**Say:**
> "Now Confirm and Close."

**Action:** Click **Confirm & Close**

**Say:**
> "The urgent welfare tray — that pulsing red section at the top of the sidebar — now shows Samira with her dispatch status. That tray is permanently visible for this session regardless of which patient I navigate to. It's designed to be impossible to accidentally ignore."

**[Briefly show the tray — point to it]**

**Action (quick tour — 20 seconds):**
- Click **Morgan Ellis** in sidebar → "STABLE — good signals, no episode"
- Click **Alex Thompson** → "CONCERNING — active episode, interventions and an escalation queued through RabbitMQ"
- Click **Casey Reid** → "RECOVERING — improving trend from a previous CONCERNING state"

**Say:**
> "Five seeded patients, one in each state. The one I care about for this demo is Eric — who doesn't exist yet. I'll create him now."

---

### [3:10 – 3:55] Create Eric

**Action:** Click **+ Add Patient** in sidebar

**Fill in the form** (say each field as you type it — don't rush, this shows the data model):

| Field | Value | What to say |
|-------|-------|-------------|
| Full name | `Eric Ng` | — |
| Login email | `eric@patient.com` | "Patient gets their own login for the check-in portal" |
| Login password | `demo123` | — |
| Profile photo | Upload any small image | "Goes directly to S3 — separate from the Postgres row" |
| Recovery start date | **Day 1 date** | "Start of his monitored recovery period" |
| Sleep start hour | `23` | — |
| Activity days/week | `4` | — |
| Medication doses/day | `2` | — |
| Medication schedule | `Sertraline 50mg AM, Quetiapine 25mg PM` | "This feeds the baseline model" |
| Meals per day | `3` | — |
| Activity type | `30-min walk` | — |
| Sleep target | `Lights out 23:00` | — |
| Reference source | `Clinician intake interview` | — |
| Intake notes | `Eric, 32 — MDD post-discharge. Stable on SSRI + quetiapine. Partner at home. Photo proof required for medication and activity claims.` | "This is the clinical context — it informs the baseline and is visible on his dashboard" |

**Action:** Click **Create patient**

**[Eric appears in sidebar — click him]**

**Say:**
> "Eric is now in the system. I need his user ID for the baseline calculation later. I'll grab it from the network response."

**Action:** Open DevTools (F12) → Network tab → find the `with-profile` POST → click it → in Response, find and **copy the `id` UUID** → paste it into your terminal command replacing `PASTE_UUID_HERE`

**Say:**
> "Got it. I'll use that in a moment."

---

### [3:55 – 4:10] Patient login

**Screen:** Switch to right panel (patient.html)

**Action:** Login with `eric@patient.com` / `demo123`

**Say:**
> "Eric logs in on his side. This is his daily check-in portal."

---

### [4:10 – 5:10] Day 1 — Good day, full evidence

**Screen:** Focus patient side. Keep clinician visible on left.

**Action on patient side:**
1. Date picker → **Day 1 date**
2. Toggle **ON**: Morning check-in
3. Toggle **ON**: Medication taken
4. Toggle **ON**: Activity completed
5. Toggle **ON**: Evening check-in
6. Meals section: tick **Breakfast**, **Lunch**, **Dinner** (3 chips selected, matches `expectedMealsPerDay: 3`)
7. Upload evidence:
   - Morning evidence: `morning.jpg`
   - Medication evidence: `meds.jpg`
   - Meal evidence 1 (Breakfast): `meal.jpg`
   - Meal evidence 2 (Lunch): `meal.jpg` (same file is fine)
   - Meal evidence 3 (Dinner): `meal.jpg`
   - Activity evidence: `activity.jpg`
   - Evening evidence: `evening.jpg`
8. Sleep time: `23:00`
9. Notes: `Kept to routine, felt OK.`
10. Click **Submit Check-in**

**Say while doing this:**
> "Day 1 — Eric's had a good day. All five signal types on. Three meals. Each one has a photo upload — morning, medication, all three meals, activity, and evening. These photos are submitted as a multipart request. The signal row lands in Postgres immediately. The evidence rows — one per uploaded file — land as PENDING in Postgres, and the actual bytes go to S3 through the `/daily-with-evidence` endpoint."

**[Submission completes]**

**Say:**
> "Now on the clinician side — the evidence review queue."

**Action on clinician side:**
- The verification alert banner should appear: "Verification required — X pending evidence items"
- Click **Review Now** (or scroll to the Evidence Review Queue section)

**Say:**
> "Seven pending evidence items for Eric. The risk engine will not count a claimed 'yes' toward the score unless there is at least one approved evidence record for that signal type on that day — that's `isEffectiveClaim` in the code. Pending doesn't validate. So I need to approve these before the score means anything."

**Action:** Click **Approve** on each evidence item, one at a time

**Say (approving):**
> "Morning — approved. Medication — approved. Breakfast, lunch, dinner — approved. Activity — approved. Evening — approved."

**[All items approved]**

**Action:** Click **Run Assessment** (top right of patient detail panel)

**Say:**
> "Running assessment. The risk engine loads the last seven real calendar days of signal logs, checks effective claims against approved evidence, computes rates and compares them to the baseline — except there is no baseline yet. Eric was just created. So the comparative factors are skipped for now. Score is [score] — low, STABLE. That's expected. The interesting number comes after I build his baseline."

---

### [5:10 – 6:05] Days 2 and 3 — same flow, then baseline

**Say:**
> "I'll run the same pattern for Days 2 and 3 — I'm deliberately building three strong days to establish a healthy baseline."

**Day 2 — patient side (fast, 20 seconds):**

**Action:**
1. Date picker → **Day 2**
2. All signals ON, same evidence files, same meals
3. Sleep: `23:00`
4. Submit

**Action on clinician:** Approve all → Run Assessment

**Say:**
> "Day 2 — same routine, all evidence approved. Score is still low — still no baseline. Two days of consistent signal logged."

---

**Day 3 — patient side (fast, 20 seconds):**

**Action:**
1. Date picker → **Day 3**
2. All signals ON, photos, meals, submit

**Action on clinician:** Approve all (don't run assessment yet)

**Say:**
> "Day 3 — three logs now. That's the minimum the baseline engine needs. Time to build the snapshot."

**[Switch to terminal / PowerShell]**

**Action:** Run the pre-typed curl with Eric's UUID:

```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/users/[UUID]/baseline/recalculate"
```

**Say:**
> "POST baseline/recalculate — this calls BaselineEngine. It reads Eric's three logged days and computes his personal baseline rates: 100% morning check-in completion, 100% medication adherence, 100% meal logging, 100% activity, 100% evening check-in, average sleep start 23:00. This snapshot is now stored as a BaselineSnapshot in Postgres. It's his personal reference — not a population average, not a clinical guideline. Eric compared to Eric."

**[Tab back to clinician]**

**Action:** Click **Run Assessment**

**Say:**
> "Now with the baseline in place — score is [score]. STABLE. The dashboard shows his personal baseline rates in the baseline card, and Contributing Factors shows nothing significant — he's tracking perfectly against himself."

---

### [6:05 – 6:45] Day 4 — the first crash

**Screen:** Focus patient side

**Action:**
1. Date picker → **Day 4**
2. All toggles **OFF** — don't touch them
3. Appointment attended: set to **No — did not attend**
4. Sleep: `02:00 AM` (late)
5. No photos — nothing to upload
6. Notes: `Couldn't get up. Partner away.`
7. Submit

**Say:**
> "Day 4. Something went wrong. Every signal is off — no morning, no medication, no meals, no activity, no evening check-in. He had an appointment today that he did not attend. No evidence uploads — there's nothing to verify."

**Action on clinician:** No evidence to approve. Click **Run Assessment**

**Say:**
> "Watch the score. The risk engine now has three perfect days and one complete crash in its 5-day recent window. The rates drop: morning check-in from 100% to 75% — that's a 25 percentage-point drop, crossing the 22pp threshold. Medication from 100% to 75% — above the 12pp threshold. Meals and evening check-in — same pattern. And one missed appointment at plus 12 points. Score: [score]. The state is [state]. The factors panel explains exactly what changed and by how much."

---

### [6:45 – 7:15] Day 5 — total collapse

**Screen:** Patient side

**Action:**
1. Date picker → **Day 5 (today)**
2. All toggles **OFF**
3. Appointment: **No — did not attend** (second consecutive miss)
4. Sleep: `03:00 AM`
5. No photos
6. Notes: `Still struggling.`
7. Submit

**Say:**
> "Day 5. Still nothing. Second consecutive missed appointment. Activity absence is now two days running — that triggers the streak factor. Sleep is drifting further from his 23:00 baseline."

**Action on clinician:** Click **Run Assessment**

**[Let the page update — pause 2 seconds — watch the score animate]**

---

### [7:15 – 8:10] ACUTE_RISK — welfare popup fires for Eric

**[Score hits 70+ — state badge changes to ACUTE_RISK — 🚨 welfare popup fires automatically]**

**[Pause. Don't click anything for 3–4 seconds. Let the viewer see it.]**

**Say:**
> "There it is. Score [score] — ACUTE_RISK. The welfare check modal fires automatically — the exact same mechanism that caught Samira when I first logged in, now triggered live by an assessment result for a patient I created two minutes ago."

**[Point at the Contributing Factors panel visible behind or below the popup]**

**Say:**
> "Let me walk through the factors before I respond. Two missed appointments — plus 24 points, that's the cap. Morning check-in rate dropped 40 percentage points against his 100% baseline — plus 18. Medication adherence drop — plus 12. Meal logging decline — plus 14. Evening check-in — plus 14. Activity absence streak two days — some points. Multi-signal synergy — plus 7, that fires when two or more primary factors are present simultaneously. Total: [score]. The state engine sees a score above 70, so ACUTE_RISK."

**[Now address the welfare popup]**

**Say:**
> "The welfare check prompt is asking: have you dispatched personnel? Four options. I'll mark dispatched, not yet arrived."

**Action:** Click **"Dispatched — not yet arrived"** → it highlights amber

**Say:**
> "Logged. Now look at the sidebar tray."

**Action:** Click **Confirm & Close**

**[Both Samira and Eric now visible in the pulsing red urgent tray]**

**Say:**
> "The urgent tray now shows two patients in ACUTE_RISK: Samira, who I acknowledged at the start of the session, and Eric, flagged just now. Both have dispatch statuses. Both are permanently visible. If I click Alex in the sidebar — " 

**Action:** Click Alex Thompson in sidebar

**Say:**
> "— the tray is still there. Still showing both. The clinician cannot navigate away from this information. That's the design intent. When I click Samira in the tray, the popup reopens for a status update."

**Action:** Click Samira in the tray → popup opens

**Say:**
> "If personnel have now arrived, I update to arrived and checked in."

**Action:** Click **"Dispatched, arrived & checked in"** → **Confirm & Close**

**[Samira's tray entry turns green/stable]**

**Say:**
> "Samira's status updates. Tray entry turns to resolved-green. Eric's stays amber — still en route."

---

### [8:10 – 9:15] §G — Architecture and design choices

**Screen:** Keep app visible. Maybe show docker ps or split with terminal briefly.

**Say:**
> "Let me explain why I chose each piece of infrastructure."

> "**Postgres** is the source of truth for everything relational — users, daily signal logs, baselines, risk assessments, evidence metadata, episodes, interventions, escalations. It's the thing I trust. ACID guarantees, relational integrity, queryable history."

> "**S3** — via LocalStack in Docker for development — holds the binary evidence: the photo bytes. I deliberately did not store blobs in Postgres. Mixing file bytes with relational rows at scale is an anti-pattern. Postgres keeps the metadata row — who uploaded it, when, which signal type, what status. S3 keeps the bytes. They're independently scalable and retrievable."

> "**Redis** does three specific things. It caches the last-known risk state per patient for fast reads without a Postgres roundtrip. It holds TTL-based deduplication locks for intervention jobs — if an intervention has already been queued for a patient within the last 60 minutes, the Redis key prevents a duplicate from firing. And it tracks operational streaks — like morning-miss counts — that need to be read and written quickly without polluting the main database with high-frequency writes."

> "**RabbitMQ** handles intervention and escalation jobs. When the risk engine decides an intervention is warranted — say, a gentle reminder after a missed morning check-in — it publishes to `recovery.exchange` with a routing key. The consumer picks it up, processes it asynchronously, and persists the outcome back to Postgres. Fire-and-forget, retryable, decoupled from the HTTP request that triggered the assessment."

> "**Kafka** is for domain events. Every significant state transition publishes an event: `routine.signal.logged`, `risk.assessed`, `recovery.state.changed`, `intervention.triggered`, `escalation.triggered`. These aren't logs — they're an immutable ordered stream. In production this is the foundation for stream analytics, audit trails, downstream subscribers, and HITL review workflows. Nothing is lost. Everything is replayable."

---

### [9:15 – 9:45] Design decisions — the three choices that matter

**Say:**
> "Three design decisions I want to highlight specifically."

> "First: **personalised baseline**. The risk engine does not compare Eric to a population average. It compares Eric to Eric — to his own established rates from his stable first days. A 60% medication adherence rate might be alarming for someone whose baseline is 100%, and perfectly normal for someone whose baseline was always 55%. Population thresholds would systematically mis-classify. `BaselineEngine` builds that personal snapshot, and `RiskEngine` uses it as the reference."

> "Second: **evidence verification before scoring**. A patient can claim 'yes' to anything — the system doesn't trust that without proof. The `isEffectiveClaim` check in `RiskEngine` requires at least one APPROVED evidence record for that signal type on that day before the claim counts toward rates and streaks. If evidence is pending, the score doesn't reflect it. If evidence is denied, a penalty factor fires. The verification workflow in the clinician portal is not optional — it's load-bearing for the score."

> "Third: **the RECOVERING state**. Recovery isn't linear. The state engine has a special case: if the base score puts a patient in STABLE or DRIFTING, but their prior state was CONCERNING or ACUTE_RISK, and the last two stored assessments show a strictly decreasing score — the label becomes RECOVERING, not STABLE. That's the system recognising momentum, not just a snapshot. Casey Reid in the seeded roster is there to demonstrate this."

---

### [9:45 – 10:00] Closing

**Say:**
> "Recovery Rhythm is not a medical device — it's a proof of concept for what a cloud-native post-discharge monitoring architecture looks like when every component has exactly one job. Postgres for truth. S3 for bytes. Redis for speed. RabbitMQ for work. Kafka for history. A risk model that explains itself, personalises to the individual, and escalates when it has to. Thank you."

---

## Rehearsal checklist (run through before recording)

- [ ] Dry run 1: full flow, note where you stumble
- [ ] Dry run 2: time it — should be 9–10 min
- [ ] Confirm welfare popup fires for Samira on page load
- [ ] Confirm Day 5 hits ACUTE_RISK (score ≥ 70) and welfare popup fires for Eric
- [ ] Confirm the tray shows both Samira AND Eric after Day 5
- [ ] Confirm baseline curl works (UUID pre-typed, app running)
- [ ] Confirm evidence photos upload without error
- [ ] Face cam is visible throughout (PiP / corner overlay)
- [ ] Student ID s2411221 visible and said aloud in first 30 seconds

---

## Troubleshooting quick-ref

| Symptom | Fix |
|---------|-----|
| Score doesn't jump on Day 5 | Check: both appts marked No, all Day 4 & 5 toggles off, baseline was run after Day 3 |
| Welfare popup doesn't fire for Eric | Check score ≥ 70 — if not, try re-running assessment; if score < 70, see above |
| Welfare popup fired too early (before you were ready) | It fires on page load for Samira — have your line ready; it fires for Eric on Run Assessment |
| Evidence queue empty on clinician side | Check you're looking at the correct patient; evidence is per-user-filtered |
| `Login email already exists` | `npm run dev:infra-reset` then `docker compose up --build` |
| Score is 0 after Run Assessment | No logs in last 7 real days — check logDate vs real clock date |
| Baseline card shows "No baseline calculated" | Baseline curl didn't run or ran before 3 logs existed |
| Welfare tray doesn't appear | Open DevTools console — check for JS errors; hard-refresh `clinician.html` |

---

## One-line architecture map (say this if asked anything in viva)

| Component | Exact role |
|-----------|-----------|
| **Postgres** | Source of truth — users, signals, baselines, assessments, evidence metadata, episodes, interventions, escalations |
| **S3 (LocalStack)** | Binary evidence bytes — photo blobs; Postgres holds metadata only |
| **Redis** | Risk state cache + deduplication TTL locks + operational streak counters |
| **RabbitMQ** | `recovery.exchange` → `recovery.interventions` / `recovery.escalations` / `recovery.reentry` — async work queues |
| **Kafka** | Immutable domain event stream — `routine.signal.logged`, `risk.assessed`, `recovery.state.changed`, `intervention.triggered` |
| **Spring Boot** | REST API layer — controller → service → engine → repository |
| **Welfare tray** | Client-side session state (`sessionStorage`) — not persisted to backend |

---

**Student ID: s2411221 — Good luck.**
