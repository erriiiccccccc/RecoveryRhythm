# Recovery Rhythm — Demo script (split-screen)

**Two paths:** (1) click through the **seeded** roster in the clinician list to show every **state**; (2) **Add patient → Eric** for a **live** story. The notes below follow the current app behaviour (risk window, baseline).

---

## Before you record (quick checks)

### 1) Risk uses the **real calendar** for its window

`RiskEngine` loads `DailySignalLog` rows with `logDate` in **`[LocalDate.now() minus 7 days, LocalDate.now()]`** (inclusive).  

- If you pick a **simulated** date that is **more than 7 days before real “today”**, that day **is not in the next assessment** — the score can ignore it completely.  
- **Demo rule:** work **back from today** on the real machine. Example: if today is **2026-04-22**, use **Day 1 = 2026-04-18**, **Day 2 = 04-19**, …, **Day 5 = 04-22** (five **distinct** days, all within the 7-day window). Do **not** use arbitrary 2023 dates on a 2026 clock.

### 2) A **baseline snapshot** is required for “vs baseline” factors

- Creating Eric **does not** call `POST /baseline/recalculate`. The **clinician UI** only calls `POST /risk/recalculate` (**Run Assessment**).  
- If no active `BaselineSnapshot` exists, `RiskEngine` still runs, but most **comparative** factors are skipped (`baseline == null` branches). In practice, Eric’s first assessments can look **flat** or odd until a baseline is built.  
- **After you have at least 3 daily logs** (see `minimum-days-required` in `application.yml`), run **once**:

```http
POST http://localhost:8080/api/users/{USER_ID}/baseline/recalculate
```

**Get `USER_ID`:** from the JSON response when you create the patient (Network tab), or from `GET /api/users`. Then **Run Assessment** again.

**Windows PowerShell example** (replace the UUID):

```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/users/00000000-0000-0000-0000-000000000000/baseline/recalculate"
```

**curl** (Git Bash / WSL):

```bash
curl -s -X POST "http://localhost:8080/api/users/00000000-0000-0000-0000-000000000000/baseline/recalculate"
```

### 3) **Verified** evidence vs risk

For each signal type, a “yes” counts in risk when the day is **fully verified** *or* there is **≥1** `APPROVED` evidence for that type on that log (`RiskEngine.isEffectiveClaim`). **Pending** uploads do not validate the claim. Approve in **Evidence review queue** before you trust the score.

### 4) “Re-engagement bonus” uses **real yesterday**

`RiskEngine` looks for a log on **`LocalDate.now().minusDays(1)`** (real calendar), not your narrative “Day 4”. A pure backdated demo may never trigger that bonus. Say *“in production, yesterday’s engagement would…”* if asked.

### 5) **Same day** re-submit merges OR-logic

`SignalService` **OR**s new booleans with existing for the same `logDate`. You cannot “turn off” a prior true through the same UI without a different API. Use **one submit per date** in the video.

### 6) Baseline from logs vs risk from effective claims

`BaselineEngine` rates for baseline snapshot use **raw** checkboxes on the entity, not `isEffectiveClaim`. Risk uses **verified** effective claims. They can **diverge** if evidence is still pending. Narrate: *“trust path for scoring is what’s verified”*.

---

## 0. Pre-flight checklist

- [ ] `docker compose up --build` (or `-d`); app responds at `http://localhost:8080/login.html`.  
- [ ] If `eric@patient.com` already exists, use a new email or `npm run dev:infra-reset` then rebuild (**destroys** DB).  
- [ ] A few small **JPEG/PNG/WebP** images (&lt; 10MB each; **client** guard). Server allows **up to 12MB** per part (`application.yml`).  
- [ ] **Split screen:** **left** = clinician (wider), **right** = patient.  
- [ ] Plan **5 consecutive real-calendar dates** ending **today** (or ending **yesterday** if you prefer, but still within `[today-6, today]` for all five). **Write them on a sticky** before recording.

**Suggested opening (technology-accurate):**

> “Patient check-ins are stored in Postgres; photo bytes in S3. Redis holds operational cache and de-dupe locks; RabbitMQ runs intervention-style jobs; Kafka streams domain events. The clinician reviews evidence, then I run a fresh risk pass — that call loads only logs in the last **seven real calendar days**.”

---

## 1. Window layout

| Window | After login | Role |
|--------|-------------|------|
| **Left ~55–60%** | `clinician.html` | Roster, Eric, evidence queue, **Run Assessment** |
| **Right** | `patient.html` | Date, toggles, meal chips, files, submit |

**Poll behaviour (clinician):** ~**5s** main refresh, ~**15s** full roster, ~**2s** evidence queue (see `clinician.html`).

**Order:** clinician login → **Add New Patient (Eric)** → **copy user id** if you need the baseline curl → patient login.

---

## 2. Create Eric (clinician) — paste table

> Set **Recovery start date** to your **Day 1** log date (first row in §5), *or* one day before it — that keeps `daysSinceRecoveryStart` and baseline window **coherent**.

| Field | Value |
|-------|--------|
| **Full name** | `Eric Ng` |
| **Login email** | `eric@patient.com` |
| **Login password** | `demo123` |
| **Profile photo** | Optional (S3) |
| **Recovery start date** | Same as **Day 1** date in §5 (e.g. `2026-04-18` if Day 5 is `2026-04-22`) |
| **Typical sleep start hour** | `23` |
| **Expected activity days per week** | `4` |
| **Expected medication doses per day** | `2` |
| **Medication schedule** | `Sertraline 50mg — morning; Quetiapine 25mg — evening` |
| **Expected meals per day** | `3` |
| **Expected activity type** | `30-minute walk or light gym, 4×/week` |
| **Expected sleep target** | `Lights out 23:00; wind-down from 22:00` |
| **Baseline reference source** | `Clinician intake interview` |
| **Baseline intake notes** | (block below) |

**Intake notes (paste):**

```text
Eric (he/him, 32) — recent stepped discharge with MDD, stable on SSRI + low-dose quetiapine. Partner at home. Targets: 3 meals/day, 2 med passes, 4 activity days/week, sleep by ~23:00. Vulnerability: late night scrolling, then missed morning. Photo proof for any claimed yes to meds/meals/activity so the score can trust the trend.
```

**Click Create** → highlight Eric. **Save the returned `id`** for the baseline `POST` (or fetch from `GET /api/users`).

---

## 3. Patient login (right window)

- `eric@patient.com` / `demo123` → `patient.html`

---

## 4. Suggested **demo day calendar** (example)

If **real today = 2026-04-22**:

| Story day | Log date (patient picker) |
|-----------|---------------------------|
| Day 1 | 2026-04-18 |
| Day 2 | 2026-04-19 |
| Day 3 | 2026-04-20 |
| Day 4 | 2026-04-21 |
| Day 5 | 2026-04-22 |

Adjust the block so **all dates are within the last 7 real days** from your recording date.

---

## 5. Day-by-day clicks (concerning → recovery)

**After Day 2 is logged** (i.e. you have 2+ days, aim for 3 before baseline — **ideally after Day 3**): run **`POST .../baseline/recalculate`**, then **Run Assessment** on the clinician. That matches `BaselineEngine`’s `minimum-days-required: 3` path cleanly when you have three log rows on Apr 18–20.

**Bad days (Days 1–2):** keep toggles **off** to avoid large evidence uploads. **All-off** = no files.

### Day 1 — “Couldn’t start”

| Control | Set |
|--------|-----|
| Morning / Med / Meal / Activity / Evening | All **off** |
| Appt | No |
| Sleep | `1:00 AM` |
| Notes | e.g. `In bed all day, skipped routine.` |

**Submit** → **Run Assessment** (no evidence to approve). Narrate: Postgres + Redis streak update + `routine.signal.logged` to Kafka; interventions only if state/thresholds fire after enough data.

### Day 2 — “Still flat”

| Control | Set |
|--------|-----|
| All routine toggles | **off** |
| Sleep | `2:00 AM` |
| Notes | e.g. `Partner at work, stayed inside.` |

**Submit** → **Run Assessment**. Say: *trend is still not recovery*.

### Day 3 — “First structured day” (needs photos + approvals)

| Control | Set |
|--------|-----|
| Morning | **On** + `morningEvidence` file |
| Medication | **On** + `medicationEvidence` file |
| Meal | **On**; tick **Breakfast, Lunch, Dinner** (or a subset) and upload **one file per** selected chip (`mealEvidence`, `mealEvidence2`, `mealEvidence3` as in UI) — **required** count must match selected meal parts |
| Activity | **On** + `activityEvidence` file |
| Evening | **On** + `eveningEvidence` file |
| Sleep | `12:00 AM` (midnight) or `11:00 PM` as you prefer |
| Notes | e.g. `First full routine day with partner back.` |

**Submit** → **left:** approve all pending in queue → **Run Assessment**.

**Then** (mandatory for fair comparative scoring in later days): run **`POST .../baseline/recalculate`** (see §0), then **Run Assessment** again.

### Day 4 — “Bending the curve”

| Control | Set |
|--------|-----|
| Same as Day 3 | All on with photos, meal chips as needed |
| Sleep | `11:00 PM` (closer to intake target) |
| Notes | e.g. `Walked twice, kept bedtime.` |

**Approve** → **Run Assessment**. You want **lowering** score and/or improved factors vs early days. **RECOVERING** may appear only with **score band + prior state + last-two-assessment downtrend** (`StateEngine`); do not over-promise the label.

### Day 5 — optional close

Repeat strong day; optional: schedule + attend an appointment. **Approve** → **Run Assessment**.

**Closing line idea:**

> “We combined relational storage, object storage, review workflow, and event-backed orchestration. The number is a teaching aid — the architecture is the deliverable.”

---

## 6. Seeded roster (optional, ~1 min)

The app starts with **five** patients (no logins) — one per state. In the list, show **Morgan** → **Samira** so the **badges** land in order: **Stable → Drifting → Concerning → Acute → Recovering** (order in the UI may be alphabetical: still fine).

- **Alex** has the **richest** history (episodes, interventions).  
- **Live recompute** on seed data works best with the machine date in **~April 2026**; otherwise use the **dashboard text** and skip hammering **Run Assessment**.  

Then switch to **Eric** (or your created patient) for the **main** live demo.

---

## 7. Troubleshooting

| Symptom | Likely cause | Fix |
|--------|----------------|-----|
| Score ignores a “day” you logged | `logDate` &gt; 7 days before **real** today | Use **recent** calendar dates (§1, §4). |
| “No baseline” or flat comparative factors | No `BaselineSnapshot` | `POST /baseline/recalculate` after 3+ logs. |
| “Yes” did not help risk | Pending / no approval | Approve evidence, **Run Assessment** again. |
| Score stuck high | All-off days still have trends vs baseline; multi-factor synergy | Check factors list; add baseline after Day 3. |
| S3 or upload error | LocalStack not ready, wrong env | `docker compose` logs; `S3_ENDPOINT` in app points to `localstack:4566` on Docker network. |
| `Login email already exists` | Prior run | New email or `docker compose down -v`. |

---

## 8. One-line map (viva)

- **Postgres** — source of truth  
- **S3 (LocalStack)** — evidence and profile **bytes**; DB holds **metadata**  
- **Redis** — cache, locks, operational streaks  
- **RabbitMQ** — `recovery.interventions` / `escalations` / `reentry`  
- **Kafka** — `routine.signal.logged`, `risk.assessed`, etc.

---

**Good luck** — use the **seeded** names to show **states** fast, then **Eric** (or a fresh patient) for the main story. Align **log dates to this week** and **baseline** once for live assess.
