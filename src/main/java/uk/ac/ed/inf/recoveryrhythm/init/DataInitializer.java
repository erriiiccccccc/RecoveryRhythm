package uk.ac.ed.inf.recoveryrhythm.init;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ed.inf.recoveryrhythm.entity.*;
import uk.ac.ed.inf.recoveryrhythm.repository.*;
import uk.ac.ed.inf.recoveryrhythm.service.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Seeds demo data: a full narrative for "Alex Thompson" (CONCERNING) plus
 * four lightweight roster personas at fixed {@link RecoveryState} levels for the dashboard.
 *
 * Timeline (all dates relative to April 7, 2026 = recovery start):
 *   Alex — days 1–5 solid; 6–10 drift/crisis; 11–15 recovery arc (see method comments)
 *   Other personas — 7-day window days 9–15 with patterns matching their state label.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RecoveryUserRepository userRepo;
    private final SupportContactRepository supportContactRepo;
    private final DailySignalLogRepository signalRepo;
    private final BaselineSnapshotRepository baselineRepo;
    private final RiskAssessmentRepository assessmentRepo;
    private final RecoveryEpisodeRepository episodeRepo;
    private final InterventionRecordRepository interventionRepo;
    private final EscalationRecordRepository escalationRepo;

    private final RedisStateService redisState;

    private static final LocalDate RECOVERY_START = LocalDate.of(2026, 4, 7);

    /** Last 7-day window used for roster demos (days 9–15 of recovery = Apr 16–22). */
    private static final int WINDOW_START_OFFSET = 9;
    private static final int WINDOW_END_OFFSET = 15;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("══════════════════════════════════════════════════════════");
        log.info("  Recovery Rhythm — Seeding demo data");
        log.info("══════════════════════════════════════════════════════════");

        RecoveryUser alex = createAlex();
        createSupportContacts(alex);
        seedSignalLogs(alex);
        calculateAndSeedBaseline(alex);
        seedRiskHistory(alex);
        seedEpisode(alex);
        seedInterventions(alex);
        seedEscalation(alex);
        setAlexCurrentState(alex);

        RecoveryUser morgan = seedMorganStable();
        RecoveryUser jordan = seedJordanDrifting();
        RecoveryUser samira = seedSamiraAcute();
        RecoveryUser casey = seedCaseyRecovering();

        log.info("══════════════════════════════════════════════════════════");
        log.info("  Demo data seeded.");
        log.info("  Alex Thompson (CONCERNING)  : {}", alex.getId());
        log.info("  Morgan Ellis (STABLE)     : {}", morgan.getId());
        log.info("  Jordan Lee (DRIFTING)     : {}", jordan.getId());
        log.info("  Samira Okonkwo (ACUTE)    : {}", samira.getId());
        log.info("  Casey Reid (RECOVERING)   : {}", casey.getId());
        log.info("  Dashboard: http://localhost:8080/login.html");
        log.info("  Clinician login  : clinician@rr.nhs.uk / demo123");
        log.info("  Patient login    : create in clinician portal (e.g. eric@patient.com / demo123)");
        log.info("══════════════════════════════════════════════════════════");
    }

    private RecoveryUser createAlex() {
        RecoveryUser alex = RecoveryUser.builder()
                .displayName("Alex Thompson")
                .recoveryStartDate(RECOVERY_START)
                .currentState(RecoveryState.CONCERNING)
                .currentRiskScore(58)
                .reentryModeActive(false)
                .build();
        return userRepo.save(alex);
    }

    private void createSupportContacts(RecoveryUser alex) {
        supportContactRepo.save(SupportContact.builder()
                .user(alex)
                .name("Sarah Thompson")
                .relationship("Sister")
                .contactChannel("EMAIL")
                .contactValue("sarah.thompson@example.com")
                .escalationEnabled(true)
                .build());

        supportContactRepo.save(SupportContact.builder()
                .user(alex)
                .name("Dr. Priya Nair")
                .relationship("Community Psychiatric Nurse")
                .contactChannel("PHONE")
                .contactValue("+44-7700-123456")
                .escalationEnabled(true)
                .build());
    }

    private void seedSignalLogs(RecoveryUser alex) {
        // ── Days 1–5: STABLE baseline period ─────────────────────────────────
        saveSignal(alex, RECOVERY_START,        true,  true,  true,  true,  false, false, true,  23, "First day. Feeling hopeful.");
        saveSignal(alex, RECOVERY_START.plusDays(1), true, true, true, true, false, false, true, 23, null);
        saveSignal(alex, RECOVERY_START.plusDays(2), true, true, true, true, true,  true,  true, 22, "Attended GP appointment.");
        saveSignal(alex, RECOVERY_START.plusDays(3), true, true, true, true, false, false, true, 23, null);
        saveSignal(alex, RECOVERY_START.plusDays(4), true, true, true, true, false, false, true, 23, "Good week. Routine feels manageable.");

        // ── Days 6–8: DRIFTING – sleep shifts, morning misses start ──────────
        saveSignal(alex, RECOVERY_START.plusDays(5), false, true, true,  true,  false, false, true,  1,  "Couldn't get up this morning.");
        saveSignal(alex, RECOVERY_START.plusDays(6), false, true, true,  false, false, false, false, 2,  "Tired. Skipped walk.");
        saveSignal(alex, RECOVERY_START.plusDays(7), false, true, false, false, false, false, false, 2,  "Hard day. Didn't eat much.");

        // ── Days 9–10: CONCERNING / ACUTE RISK ───────────────────────────────
        saveSignal(alex, RECOVERY_START.plusDays(8), false, false, false, false, true, false, false, 3, "Missed meds. Appointment felt too much.");
        saveSignal(alex, RECOVERY_START.plusDays(9), false, false, false, false, false, false, false, 3, "Very low day. Stayed in.");

        // ── Days 11–13: Gradual RECOVERY ─────────────────────────────────────
        saveSignal(alex, RECOVERY_START.plusDays(10), false, true,  true,  false, false, false, true, 2, "Managed medication today.");
        saveSignal(alex, RECOVERY_START.plusDays(11), true,  true,  true,  false, false, false, true, 1, "Morning check-in felt easier.");
        saveSignal(alex, RECOVERY_START.plusDays(12), true,  true,  true,  true,  false, false, true, 0, "Short walk outside. Progress.");

        // ── Days 14–15: Stabilising ──────────────────────────────────────────
        saveSignal(alex, RECOVERY_START.plusDays(13), true, true, true, true, false, false, true, 0,  "Better sleep.");
        saveSignal(alex, RECOVERY_START.plusDays(14), true, true, true, true, false, false, true, 23, "Routine mostly back. Cautiously stable.");

        log.info("  ✓ Signal logs seeded (15 days)");
    }

    private void saveSignal(RecoveryUser user, LocalDate date,
                             boolean morning, boolean med, boolean meal, boolean activity,
                             boolean apptScheduled, boolean apptAttended, boolean evening,
                             int sleepHour, String notes) {
        DailySignalLog log_ = DailySignalLog.builder()
                .user(user)
                .logDate(date)
                .morningCheckInCompleted(morning)
                .medicationTaken(med)
                .mealLogged(meal)
                .activityLogged(activity)
                .appointmentScheduled(apptScheduled)
                .appointmentAttended(apptAttended)
                .eveningCheckInCompleted(evening)
                .sleepStartHour(sleepHour == 0 ? null : sleepHour)
                .notes(notes)
                .manuallyEntered(true)
                .verificationState(DailyVerificationState.VERIFIED)
                .build();
        signalRepo.save(log_);

        // Update Redis streaks to reflect seeded data state
        redisState.updateSignalStreaks(user.getId(), morning, activity, evening, med, meal);
    }

    private void calculateAndSeedBaseline(RecoveryUser alex) {
        BaselineSnapshot baseline = BaselineSnapshot.builder()
                .user(alex)
                .morningCheckInRate(0.86)
                .medicationAdherenceRate(0.90)
                .mealLoggingRate(0.88)
                .activityRate(0.82)
                .appointmentAttendanceRate(1.0)
                .eveningCheckInRate(0.90)
                .averageSleepStartHour(22.8)
                .stableWindowDays(5)
                .summaryJson("{\"note\":\"Established from days 1–5 of recovery period\"}")
                .active(true)
                .build();
        baselineRepo.save(baseline);
        log.info("  ✓ Baseline snapshot seeded (stable window = 5 days)");
    }

    private void seedRiskHistory(RecoveryUser alex) {
        // Historical assessments to show the full trajectory arc
        List<SeedAssessment> assessments = List.of(
            new SeedAssessment(RECOVERY_START.plusDays(4), 5,  RecoveryState.STABLE,
                "Recovery pattern is stable and consistent with personal baseline.",
                "All signals within baseline range. Morning check-in 100%, medication 100%, activity 100%."),
            new SeedAssessment(RECOVERY_START.plusDays(6), 18, RecoveryState.STABLE,
                "Recovery pattern is stable and consistent with personal baseline.",
                "Minor sleep drift noted (+1h) but overall signals remain on baseline."),
            new SeedAssessment(RECOVERY_START.plusDays(7), 35, RecoveryState.DRIFTING,
                "Routine signals have begun to drift from your baseline. Primary signal: Morning check-in completion.",
                "Morning check-in dropped to 33% (vs 86% baseline). Activity reduced. Sleep shifted by 1.8h."),
            new SeedAssessment(RECOVERY_START.plusDays(8), 62, RecoveryState.CONCERNING,
                "Several routine signals have deviated significantly from baseline. Risk score: 62.",
                "Morning check-in at 0% vs 86% baseline (−86pp). Medication miss recorded. Activity absent for 2 consecutive days. Sleep shifted +2.2h."),
            new SeedAssessment(RECOVERY_START.plusDays(9), 81, RecoveryState.ACUTE_RISK,
                "Multiple critical signals are outside your stable baseline. Immediate support is being coordinated. Risk score: 81.",
                "Morning check-in 0% (−86pp). Medication 0% today. No meals logged. No activity for 3 consecutive days. Sleep +3h from baseline. Multiple synergy factors active."),
            new SeedAssessment(RECOVERY_START.plusDays(11), 68, RecoveryState.CONCERNING,
                "Several routine signals have deviated significantly from baseline. Risk score: 68.",
                "Morning check-in returned (1 day). Medication resumed. Activity still absent. Sleep improving slowly."),
            new SeedAssessment(RECOVERY_START.plusDays(13), 52, RecoveryState.CONCERNING,
                "Recent recovery pattern has drifted significantly from baseline.",
                "Morning check-in improving. Activity returning. Sleep drift reducing. Still 2 synergy factors."),
            new SeedAssessment(RECOVERY_START.plusDays(14), 38, RecoveryState.RECOVERING,
                "Recovery trajectory is improving. Signals are returning toward baseline.",
                "Consistent improvement across morning, medication, and activity signals over last 3 days.")
        );

        for (SeedAssessment sa : assessments) {
            RiskAssessment assessment = RiskAssessment.builder()
                    .user(alex)
                    .assessedAt(sa.date.atTime(9, 30))
                    .riskScore(sa.score)
                    .state(sa.state)
                    .conciseSummary(sa.concise)
                    .detailedExplanation(sa.detail)
                    .factorBreakdownJson("[]")
                    .build();
            assessmentRepo.save(assessment);
        }
        log.info("  ✓ Risk assessment history seeded (8 assessments)");
    }

    private void seedEpisode(RecoveryUser alex) {
        RecoveryEpisode episode = RecoveryEpisode.builder()
                .user(alex)
                .openedAt(RECOVERY_START.plusDays(8).atTime(9, 35))
                .peakRiskScore(81)
                .openingReason("Concerning pattern detected: morning check-in dropped 86pp from baseline, medication miss, activity absent 2+ days.")
                .stateAtOpen(RecoveryState.CONCERNING)
                .active(true)
                .build();
        episodeRepo.save(episode);
        log.info("  ✓ Active recovery episode seeded");
    }

    private void seedInterventions(RecoveryUser alex) {
        List<SeedIntervention> interventions = List.of(
            new SeedIntervention(RECOVERY_START.plusDays(5).atTime(10, 0), InterventionType.GENTLE_REMINDER,
                "Hi Alex — just a gentle check-in. How are you doing today? Logging even one thing helps us support you better.",
                InterventionStatus.SENT, "User state drifted from baseline"),
            new SeedIntervention(RECOVERY_START.plusDays(7).atTime(14, 0), InterventionType.ENHANCED_CHECKIN,
                "Hi Alex — we've noticed your routine has shifted a bit lately. Would you like to do a short check-in with us today?",
                InterventionStatus.SENT, "Concerning pattern detected"),
            new SeedIntervention(RECOVERY_START.plusDays(8).atTime(10, 0), InterventionType.LOW_PRESSURE_MESSAGE,
                "Hi Alex — no need to do anything right now. We're thinking of you and we're here whenever you need support.",
                InterventionStatus.SENT, "Supportive low-pressure message during concerning period"),
            new SeedIntervention(RECOVERY_START.plusDays(9).atTime(9, 0), InterventionType.ESCALATION_PREPARATION,
                "Hi Alex — we want to make sure you're okay. If you'd like, your support contact has been informed that you might need a check-in.",
                InterventionStatus.SENT, "Acute risk: supporter alert preparation"),
            new SeedIntervention(RECOVERY_START.plusDays(10).atTime(10, 0), InterventionType.REENTRY_MODE_OFFER,
                "Hi Alex — welcome back. Today, you only need to do one thing. Confirm you're awake, log one meal, or just say hello. That's it.",
                InterventionStatus.SENT, "Re-entry mode activated after 4-day check-in streak"),
            new SeedIntervention(RECOVERY_START.plusDays(13).atTime(9, 0), InterventionType.RECOVERY_ACKNOWLEDGEMENT,
                "Hi Alex — we've noticed your routine is improving. That's real progress. Keep going at your own pace.",
                InterventionStatus.SENT, "Recovery trend acknowledged")
        );

        for (SeedIntervention si : interventions) {
            interventionRepo.save(InterventionRecord.builder()
                    .user(alex)
                    .triggeredAt(si.triggeredAt)
                    .interventionType(si.type)
                    .message(si.message)
                    .status(si.status)
                    .reason(si.reason)
                    .build());
        }
        log.info("  ✓ Intervention records seeded (6 records)");
    }

    private void seedEscalation(RecoveryUser alex) {
        var contacts = supportContactRepo.findByUser(alex);
        SupportContact sarah = contacts.stream()
                .filter(c -> c.getName().equals("Sarah Thompson"))
                .findFirst().orElse(null);

        EscalationRecord escalation = EscalationRecord.builder()
                .user(alex)
                .supportContact(sarah)
                .triggeredAt(RECOVERY_START.plusDays(9).atTime(9, 5))
                .level(EscalationLevel.LEVEL_2_ACTIVE_CONCERN)
                .reason("Acute risk score of 81 detected. Multiple baseline deviations for 3+ days. No morning check-in for 4 consecutive days.")
                .outcomeStatus("NOTIFIED")
                .build();
        escalationRepo.save(escalation);
        log.info("  ✓ Escalation record seeded");
    }

    private void setAlexCurrentState(RecoveryUser alex) {
        alex.setCurrentState(RecoveryState.CONCERNING);
        alex.setCurrentRiskScore(58);
        userRepo.save(alex);

        redisState.cacheRiskState(alex.getId(), 58, RecoveryState.CONCERNING.name(),
                "[{\"factor\":\"morning_checkin_drop\",\"impact\":15,\"details\":\"86% → 40% (−46 pp vs baseline)\",\"severity\":\"MEDIUM\"}," +
                "{\"factor\":\"activity_absence_streak\",\"impact\":20,\"details\":\"3 consecutive days without any activity\",\"severity\":\"MEDIUM\"}," +
                "{\"factor\":\"sleep_timing_drift\",\"impact\":10,\"details\":\"Sleep shifted 2.2h later vs baseline\",\"severity\":\"MEDIUM\"}," +
                "{\"factor\":\"multi_signal_synergy\",\"impact\":10,\"details\":\"3 concurrent risk indicators\",\"severity\":\"HIGH\"}]");

        redisState.setRolling7Count(alex.getId(), "med_taken", 5);
        redisState.setRolling7Count(alex.getId(), "meal_logged", 4);
    }

    // ══ Light roster personas (clinician list only — no patient login) ═══════

    private RecoveryUser seedMorganStable() {
        RecoveryUser u = userRepo.save(RecoveryUser.builder()
                .displayName("Morgan Ellis")
                .recoveryStartDate(RECOVERY_START)
                .currentState(RecoveryState.STABLE)
                .currentRiskScore(18)
                .reentryModeActive(false)
                .baselineIntakeNotes("Seeded demo — routine consistently on track in the last 7 log days.")
                .build());
        for (int d = WINDOW_START_OFFSET; d <= WINDOW_END_OFFSET; d++) {
            saveSignal(u, RECOVERY_START.plusDays(d), true, true, true, true, false, false, true, 23, null);
        }
        saveSimpleBaseline(u, 0.95, 0.96, 0.95, 0.93, 0.94, 1.0, 23.0);
        saveAssessmentAt(u, RECOVERY_START.plusDays(14), 20, RecoveryState.STABLE,
                "Recovery pattern is stable and consistent with personal baseline.",
                "All monitored signals in range for the recent window.",
                "[]");
        saveAssessmentAt(u, RECOVERY_START.plusDays(15), 18, RecoveryState.STABLE,
                "Recovery pattern is stable and consistent with personal baseline.",
                "Morning, medication, meals, and activity on target; sleep within goal.",
                "[]");
        cacheDemoRedis(u, 18, RecoveryState.STABLE,
                "[{\"factor\":\"re_engagement_bonus\",\"impact\":-10,\"details\":\"All signals completed yesterday — positive engagement signal\",\"severity\":\"PROTECTIVE\"}]");
        log.info("  ✓ Morgan Ellis (STABLE) seeded");
        return u;
    }

    private RecoveryUser seedJordanDrifting() {
        RecoveryUser u = userRepo.save(RecoveryUser.builder()
                .displayName("Jordan Lee")
                .recoveryStartDate(RECOVERY_START)
                .currentState(RecoveryState.DRIFTING)
                .currentRiskScore(33)
                .reentryModeActive(false)
                .baselineIntakeNotes("Seeded demo — morning and sleep slipped vs earlier stable week.")
                .build());
        // Drifting: ~4/7 mornings missed, uneven activity, variable sleep
        boolean[] morning = {true, false, false, true, false, true, false};
        boolean[] activity = {true, true, false, true, true, false, true};
        for (int i = 0; i < 7; i++) {
            int d = WINDOW_START_OFFSET + i;
            int sleep = (i < 3) ? 1 : 23;
            boolean evening = i != 1 && i != 2;
            saveSignal(u, RECOVERY_START.plusDays(d), morning[i], true, true, activity[i], false, false, evening, sleep,
                    i == 3 ? "Overslept, skipped walk." : null);
        }
        saveSimpleBaseline(u, 0.92, 0.9, 0.88, 0.85, 0.88, 1.0, 23.0);
        saveAssessmentAt(u, RECOVERY_START.plusDays(14), 28, RecoveryState.DRIFTING,
                "Routine signals have begun to drift from your baseline.",
                "Morning check-in and activity below earlier stable rates; sleep variable.",
                "[]");
        saveAssessmentAt(u, RECOVERY_START.plusDays(15), 33, RecoveryState.DRIFTING,
                "Routine signals have begun to drift from your baseline. Primary signal: Morning check-in completion.",
                "Ongoing shortfall in morning and activity rate vs your established baseline.",
                "[]");
        cacheDemoRedis(u, 33, RecoveryState.DRIFTING,
                "[{\"factor\":\"morning_checkin_drop\",\"impact\":15,\"details\":\"92% → 57% (\\u221235 pp vs baseline)\",\"severity\":\"MEDIUM\"}," +
                "{\"factor\":\"activity_absence_streak\",\"impact\":20,\"details\":\"1 consecutive days without any activity\",\"severity\":\"MEDIUM\"}]");
        log.info("  ✓ Jordan Lee (DRIFTING) seeded");
        return u;
    }

    private RecoveryUser seedSamiraAcute() {
        RecoveryUser u = userRepo.save(RecoveryUser.builder()
                .displayName("Samira Okonkwo")
                .recoveryStartDate(RECOVERY_START)
                .currentState(RecoveryState.ACUTE_RISK)
                .currentRiskScore(85)
                .reentryModeActive(true)
                .baselineIntakeNotes("Seeded demo — high concern: disengaged across most signals; escalation path illustrated.")
                .build());
        for (int i = 0; i < 7; i++) {
            int d = WINDOW_START_OFFSET + i;
            boolean appt = (i == 2);
            saveSignal(u, RECOVERY_START.plusDays(d),
                    false, false, false, false, appt, false, false, 3,
                    i == 0 ? "Crisis week — could not keep routine." : null);
        }
        saveSimpleBaseline(u, 0.85, 0.88, 0.9, 0.8, 0.85, 1.0, 23.0);
        saveAssessmentAt(u, RECOVERY_START.plusDays(14), 78, RecoveryState.ACUTE_RISK,
                "Multiple critical signals are outside your stable baseline. Immediate support is being coordinated. Risk score: 78.",
                "Severe disengagement across check-ins, meals, and activity; sleep very late.",
                "[]");
        saveAssessmentAt(u, RECOVERY_START.plusDays(15), 85, RecoveryState.ACUTE_RISK,
                "Multiple critical signals are outside your stable baseline. Immediate support is being coordinated. Risk score: 85.",
                "Sustained pattern — highest priority review.",
                "[]");
        episodeRepo.save(RecoveryEpisode.builder()
                .user(u)
                .openedAt(RECOVERY_START.plusDays(14).atTime(8, 0))
                .peakRiskScore(85)
                .openingReason("Seeded ACUTE_RISK demo — multi-signal breakdown vs baseline.")
                .stateAtOpen(RecoveryState.ACUTE_RISK)
                .active(true)
                .build());
        interventionRepo.save(InterventionRecord.builder()
                .user(u)
                .triggeredAt(RECOVERY_START.plusDays(15).atTime(9, 0))
                .interventionType(InterventionType.ENHANCED_CHECKIN)
                .message("Hi Samira — your team has been trying to reach you. Reply when you can; no judgement.")
                .status(InterventionStatus.SENT)
                .reason("Acute risk: immediate check-in required")
                .build());
        escalationRepo.save(EscalationRecord.builder()
                .user(u)
                .supportContact(null)
                .triggeredAt(RECOVERY_START.plusDays(15).atTime(9, 15))
                .level(EscalationLevel.LEVEL_2_ACTIVE_CONCERN)
                .reason("Seeded demo: acute risk score 85 — welfare check coordinated.")
                .outcomeStatus("PENDING")
                .build());
        cacheDemoRedis(u, 85, RecoveryState.ACUTE_RISK,
                "[{\"factor\":\"medication_adherence_drop\",\"impact\":25,\"details\":\"88% → 0%\",\"severity\":\"HIGH\"}," +
                "{\"factor\":\"activity_absence_streak\",\"impact\":25,\"details\":\"5 consecutive days without any activity\",\"severity\":\"HIGH\"}," +
                "{\"factor\":\"multi_signal_synergy\",\"impact\":10,\"details\":\"3 concurrent risk indicators\",\"severity\":\"HIGH\"}]");
        log.info("  ✓ Samira Okonkwo (ACUTE_RISK) seeded");
        return u;
    }

    private RecoveryUser seedCaseyRecovering() {
        RecoveryUser u = userRepo.save(RecoveryUser.builder()
                .displayName("Casey Reid")
                .recoveryStartDate(RECOVERY_START)
                .currentState(RecoveryState.RECOVERING)
                .currentRiskScore(36)
                .reentryModeActive(false)
                .baselineIntakeNotes("Seeded demo — post-crisis improvement; scores trending down for three assessments.")
                .build());
        for (int d = WINDOW_START_OFFSET; d <= WINDOW_END_OFFSET; d++) {
            boolean good = d >= WINDOW_START_OFFSET + 3;
            saveSignal(u, RECOVERY_START.plusDays(d),
                    good, good, good, good, false, false, good, good ? 23 : 2,
                    good ? "Back on a simple routine. One day at a time." : "Rough patch — disengaged.");
        }
        saveSimpleBaseline(u, 0.88, 0.9, 0.87, 0.86, 0.88, 1.0, 23.0);
        // History: need downtrend for RECOVERING narrative (newest = lowest score)
        saveAssessmentAt(u, RECOVERY_START.plusDays(12), 68, RecoveryState.CONCERNING,
                "Several routine signals have deviated significantly from baseline. Risk score: 68.",
                "Earlier in the week, multiple signals off track.",
                "[]");
        saveAssessmentAt(u, RECOVERY_START.plusDays(14), 48, RecoveryState.CONCERNING,
                "Several routine signals have deviated significantly from baseline. Risk score: 48.",
                "Improvement started — check-ins and meds returning.",
                "[]");
        saveAssessmentAt(u, RECOVERY_START.plusDays(15), 36, RecoveryState.RECOVERING,
                "Recovery trajectory is improving. Signals are returning toward baseline.",
                "Third consecutive down assessment; routine rebuilding.",
                "[]");
        cacheDemoRedis(u, 36, RecoveryState.RECOVERING,
                "[{\"factor\":\"activity_absence_streak\",\"impact\":20,\"details\":\"0 consecutive days without any activity\",\"severity\":\"MEDIUM\"}]");
        log.info("  ✓ Casey Reid (RECOVERING) seeded");
        return u;
    }

    private void saveSimpleBaseline(RecoveryUser user,
                                    double morning, double med, double meal, double activity,
                                    double evening, double appt, double sleepAvg) {
        baselineRepo.save(BaselineSnapshot.builder()
                .user(user)
                .morningCheckInRate(morning)
                .medicationAdherenceRate(med)
                .mealLoggingRate(meal)
                .activityRate(activity)
                .appointmentAttendanceRate(appt)
                .eveningCheckInRate(evening)
                .averageSleepStartHour(sleepAvg)
                .stableWindowDays(5)
                .summaryJson("{\"note\":\"Seeded demo baseline\"}")
                .active(true)
                .build());
    }

    private void saveAssessmentAt(RecoveryUser user, LocalDate onDay, int score, RecoveryState state,
                                  String concise, String detail, String factorJson) {
        assessmentRepo.save(RiskAssessment.builder()
                .user(user)
                .assessedAt(onDay.atTime(10, 0))
                .riskScore(score)
                .state(state)
                .conciseSummary(concise)
                .detailedExplanation(detail)
                .factorBreakdownJson(factorJson)
                .build());
    }

    private void cacheDemoRedis(RecoveryUser user, int score, RecoveryState state, String factorJson) {
        user.setCurrentState(state);
        user.setCurrentRiskScore(score);
        userRepo.save(user);
        redisState.cacheRiskState(user.getId(), score, state.name(), factorJson);
    }

    // ── Helper records ────────────────────────────────────────────────────────

    private record SeedAssessment(LocalDate date, int score, RecoveryState state,
                                   String concise, String detail) {}

    private record SeedIntervention(LocalDateTime triggeredAt, InterventionType type,
                                     String message, InterventionStatus status, String reason) {}
}
