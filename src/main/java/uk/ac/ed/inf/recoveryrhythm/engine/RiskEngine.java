package uk.ac.ed.inf.recoveryrhythm.engine;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ed.inf.recoveryrhythm.dto.ContributingFactor;
import uk.ac.ed.inf.recoveryrhythm.dto.RiskAssessmentResponse;
import uk.ac.ed.inf.recoveryrhythm.entity.*;
import uk.ac.ed.inf.recoveryrhythm.events.KafkaEventPublisher;
import uk.ac.ed.inf.recoveryrhythm.repository.*;
import uk.ac.ed.inf.recoveryrhythm.service.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core risk computation engine.
 *
 * Strategy:
 *   1. Load the user's active baseline snapshot.
 *   2. Load recent signal logs (configurable window, default 5–7 days).
 *   3. Compute factor contributions (see FACTOR SCORING section).
 *   4. Sum to a total risk score (0–100).
 *   5. Map score + prior state → RecoveryState via StateEngine.
 *   6. Generate human-readable explanation.
 *   7. Persist RiskAssessment.
 *   8. Handle episode open/close.
 *   9. Trigger interventions via InterventionService.
 *   10. Publish Kafka events.
 *
 * FACTOR SCORING:
 *   morning_checkin_drop        : baseline vs recent 5d rate drop > 30pp   → +15
 *   medication_adherence_drop   : baseline vs recent 7d rate drop > 20pp   → +20
 *   activity_absence_streak     : 3 consecutive days no activity            → +20 (5d → +25)
 *   meal_logging_decline        : baseline vs recent 5d rate drop > 25pp   → +10
 *   sleep_drift_later           : recent avg sleep hour vs baseline > +2h   → +10
 *   appointment_miss            : any missed scheduled appointment           → +15 per miss (max 30)
 *   evening_checkin_decline     : baseline vs recent 5d rate drop > 30pp   → +10
 *   multi_signal_synergy        : 3+ factors active simultaneously          → +10
 *   re_engagement_bonus         : all signals completed yesterday            → -10
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RiskEngine {

    private final DailySignalLogRepository signalRepo;
    private final SignalEvidenceRepository evidenceRepo;
    private final BaselineSnapshotRepository baselineRepo;
    private final RiskAssessmentRepository assessmentRepo;
    private final StateEngine stateEngine;
    private final EpisodeService episodeService;
    private final InterventionService interventionService;
    private final EscalationService escalationService;
    private final RedisStateService redisState;
    private final KafkaEventPublisher kafkaPublisher;
    private final ObjectMapper objectMapper;

    @Value("${recovery-rhythm.risk.recent-window-days:5}")
    private int recentWindowDays;

    @Value("${recovery-rhythm.risk.assessment-window-days:7}")
    private int assessmentWindowDays;

    @Value("${recovery-rhythm.reentry.disengagement-streak-threshold:4}")
    private int reentryStreakThreshold;

    @Transactional
    public RiskAssessmentResponse assess(RecoveryUser user) {
        Optional<BaselineSnapshot> baselineOpt = baselineRepo.findByUserAndActiveTrue(user);

        if (baselineOpt.isEmpty()) {
            log.warn("[RiskEngine] No baseline for user {}, scoring with defaults", user.getDisplayName());
        }

        BaselineSnapshot baseline = baselineOpt.orElse(null);

        LocalDate windowStart = LocalDate.now().minusDays(assessmentWindowDays);
        List<DailySignalLog> recentLogs = signalRepo
                .findByUserAndLogDateBetweenOrderByLogDateAsc(user, windowStart, LocalDate.now());

        LocalDate recentWindowStart = LocalDate.now().minusDays(recentWindowDays);
        List<DailySignalLog> shortWindowLogs = recentLogs.stream()
                .filter(l -> !l.getLogDate().isBefore(recentWindowStart))
                .collect(Collectors.toList());

        List<ContributingFactor> factors = new ArrayList<>();
        int totalScore = 0;

        // ── Factor 1: Morning check-in drop ──────────────────────────────────
        if (baseline != null && !shortWindowLogs.isEmpty()) {
            double baselineMorning = baseline.getMorningCheckInRate();
            double recentMorning = rate(shortWindowLogs, log -> isEffectiveClaim(log, EvidenceSignalType.MORNING_CHECKIN, DailySignalLog::isMorningCheckInCompleted));
            double drop = baselineMorning - recentMorning;
            if (drop > 0.30) {
                int impact = drop > 0.50 ? 20 : 15;
                factors.add(ContributingFactor.builder()
                        .factor("morning_checkin_drop")
                        .impact(impact)
                        .details(String.format("%.0f%% → %.0f%% (−%.0f pp vs baseline)",
                                baselineMorning * 100, recentMorning * 100, drop * 100))
                        .severity(drop > 0.50 ? "HIGH" : "MEDIUM")
                        .build());
                totalScore += impact;
            }
        }

        // ── Factor 2: Medication adherence drop (verified claim) ─────────────
        if (baseline != null && !recentLogs.isEmpty()) {
            double baselineMed = baseline.getMedicationAdherenceRate();
            double recentMed = rate(recentLogs, log -> isEffectiveClaim(log, EvidenceSignalType.MEDICATION, DailySignalLog::isMedicationTaken));
            double drop = baselineMed - recentMed;
            if (drop > 0.20) {
                int impact = drop > 0.40 ? 25 : 20;
                factors.add(ContributingFactor.builder()
                        .factor("medication_adherence_drop")
                        .impact(impact)
                        .details(String.format("%.0f%% → %.0f%% (−%.0f pp vs baseline)",
                                baselineMed * 100, recentMed * 100, drop * 100))
                        .severity(drop > 0.40 ? "HIGH" : "MEDIUM")
                        .build());
                totalScore += impact;
            }
        }

        // ── Factor 3: Activity absence streak ────────────────────────────────
        int activityStreak = computeEffectiveActivityMissStreak(recentLogs);
        if (activityStreak >= 3) {
            int impact = activityStreak >= 5 ? 25 : 20;
            factors.add(ContributingFactor.builder()
                    .factor("activity_absence_streak")
                    .impact(impact)
                    .details(activityStreak + " consecutive days without any activity")
                    .severity(activityStreak >= 5 ? "HIGH" : "MEDIUM")
                    .build());
            totalScore += impact;
        }

        // ── Factor 4: Meal logging decline (verified claim) ──────────────────
        if (baseline != null && !shortWindowLogs.isEmpty()) {
            double baselineMeal = baseline.getMealLoggingRate();
            double recentMeal = rate(shortWindowLogs, log -> isEffectiveClaim(log, EvidenceSignalType.MEAL, DailySignalLog::isMealLogged));
            double drop = baselineMeal - recentMeal;
            if (drop > 0.25) {
                factors.add(ContributingFactor.builder()
                        .factor("meal_logging_decline")
                        .impact(10)
                        .details(String.format("%.0f%% → %.0f%% (−%.0f pp vs baseline)",
                                baselineMeal * 100, recentMeal * 100, drop * 100))
                        .severity("MEDIUM")
                        .build());
                totalScore += 10;
            }
        }

        // ── Factor 5: Sleep timing drift ─────────────────────────────────────
        if (baseline != null && !shortWindowLogs.isEmpty()) {
            OptionalDouble recentSleepOpt = shortWindowLogs.stream()
                    .filter(s -> s.getSleepStartHour() != null)
                    .mapToInt(DailySignalLog::getSleepStartHour)
                    .average();

            if (recentSleepOpt.isPresent()) {
                double drift = recentSleepOpt.getAsDouble() - baseline.getAverageSleepStartHour();
                if (Math.abs(drift) > 2.0) {
                    String direction = drift > 0 ? "later" : "earlier";
                    int impact = Math.abs(drift) > 3.0 ? 15 : 10;
                    factors.add(ContributingFactor.builder()
                            .factor("sleep_timing_drift")
                            .impact(impact)
                            .details(String.format("Sleep shifted %.1fh %s vs baseline (%.0fh avg vs %.0fh baseline)",
                                    Math.abs(drift), direction,
                                    recentSleepOpt.getAsDouble(), baseline.getAverageSleepStartHour()))
                            .severity(Math.abs(drift) > 3.0 ? "HIGH" : "MEDIUM")
                            .build());
                    totalScore += impact;
                }
            }
        }

        // ── Factor 6: Missed appointments ────────────────────────────────────
        long missedAppointments = recentLogs.stream()
                .filter(s -> s.isAppointmentScheduled() && !s.isAppointmentAttended())
                .count();
        if (missedAppointments > 0) {
            int impact = (int) Math.min(missedAppointments * 15, 30);
            factors.add(ContributingFactor.builder()
                    .factor("appointment_miss")
                    .impact(impact)
                    .details(missedAppointments + " scheduled appointment(s) missed in last " + assessmentWindowDays + " days")
                    .severity(missedAppointments > 1 ? "HIGH" : "MEDIUM")
                    .build());
            totalScore += impact;
        }

        // ── Factor 7: Evening check-in decline ───────────────────────────────
        if (baseline != null && !shortWindowLogs.isEmpty()) {
            double baselineEvening = baseline.getEveningCheckInRate();
            double recentEvening = rate(shortWindowLogs, log -> isEffectiveClaim(log, EvidenceSignalType.EVENING_CHECKIN, DailySignalLog::isEveningCheckInCompleted));
            double drop = baselineEvening - recentEvening;
            if (drop > 0.30) {
                factors.add(ContributingFactor.builder()
                        .factor("evening_checkin_decline")
                        .impact(10)
                        .details(String.format("%.0f%% → %.0f%% (−%.0f pp vs baseline)",
                                baselineEvening * 100, recentEvening * 100, drop * 100))
                        .severity("LOW")
                        .build());
                totalScore += 10;
            }
        }

        // ── Factor 8: Multi-signal synergy bonus ─────────────────────────────
        if (factors.size() >= 3) {
            factors.add(ContributingFactor.builder()
                    .factor("multi_signal_synergy")
                    .impact(10)
                    .details(factors.size() + " concurrent risk indicators — combined pattern elevates concern")
                    .severity("HIGH")
                    .build());
            totalScore += 10;
        }

        // ── Factor 10: Denied evidence in recent window ──────────────────────
        long deniedEvidenceCount = evidenceRepo.countByUserAndStatusAndDailySignalLog_LogDateBetween(
                user, VerificationStatus.DENIED, windowStart, LocalDate.now());
        if (deniedEvidenceCount > 0) {
            int impact = deniedEvidenceCount >= 3 ? 15 : 10;
            factors.add(ContributingFactor.builder()
                    .factor("denied_evidence_claims")
                    .impact(impact)
                    .details(deniedEvidenceCount + " evidence item(s) were denied by clinician verification")
                    .severity(deniedEvidenceCount >= 3 ? "HIGH" : "MEDIUM")
                    .build());
            totalScore += impact;
        }

        // ── Factor 9: Re-engagement bonus (positive) ─────────────────────────
        if (!recentLogs.isEmpty()) {
            DailySignalLog yesterday = recentLogs.stream()
                    .filter(l -> l.getLogDate().equals(LocalDate.now().minusDays(1)))
                    .findFirst().orElse(null);
            if (yesterday != null && isFullyEngaged(yesterday)) {
                factors.add(ContributingFactor.builder()
                        .factor("re_engagement_bonus")
                        .impact(-10)
                        .details("All signals completed yesterday — positive engagement signal")
                        .severity("PROTECTIVE")
                        .build());
                totalScore = Math.max(0, totalScore - 10);
            }
        }

        totalScore = Math.min(100, Math.max(0, totalScore));

        // ── State determination ──────────────────────────────────────────────
        RecoveryState previousState = user.getCurrentState();
        List<RiskAssessment> recentHistory = assessmentRepo.findTop10ByUserOrderByAssessedAtDesc(user);
        RecoveryState newState = stateEngine.determineState(totalScore, previousState, recentHistory);

        // ── Generate human-readable explanation ──────────────────────────────
        String conciseSummary = buildConciseSummary(newState, totalScore, factors);
        String detailedExplanation = buildDetailedExplanation(newState, totalScore, factors, baseline);

        // ── Persist assessment ───────────────────────────────────────────────
        String factorJson = toJson(factors);
        RiskAssessment assessment = RiskAssessment.builder()
                .user(user)
                .riskScore(totalScore)
                .state(newState)
                .conciseSummary(conciseSummary)
                .detailedExplanation(detailedExplanation)
                .factorBreakdownJson(factorJson)
                .build();
        assessment = assessmentRepo.save(assessment);

        // ── Update user state ─────────────────────────────────────────────────
        user.setCurrentState(newState);
        user.setCurrentRiskScore(totalScore);

        // ── Update Redis cache ────────────────────────────────────────────────
        redisState.cacheRiskState(user.getId(), totalScore, newState.name(), factorJson);

        // ── Publish Kafka events ──────────────────────────────────────────────
        kafkaPublisher.publishRiskAssessed(user.getId(), totalScore, newState.name());
        if (newState != previousState) {
            kafkaPublisher.publishStateChanged(user.getId(), previousState.name(), newState.name(), totalScore);
        }

        // ── Episode management ────────────────────────────────────────────────
        RecoveryEpisode episode = episodeService
                .handleStateTransition(user, previousState, newState, totalScore, conciseSummary)
                .orElse(null);

        // ── Re-entry mode check ───────────────────────────────────────────────
        int morningStreak = redisState.getStreak(user.getId(), "morning_miss");
        if (morningStreak >= reentryStreakThreshold && !user.isReentryModeActive()) {
            interventionService.triggerReentryMode(user,
                    "User missed morning check-in for " + morningStreak + " consecutive days");
        }

        // ── Interventions ─────────────────────────────────────────────────────
        interventionService.triggerInterventionsForState(user, newState, totalScore, episode);

        // ── Escalation (acute risk only) ──────────────────────────────────────
        if (newState == RecoveryState.ACUTE_RISK && totalScore >= 80) {
            escalationService.triggerEscalation(user, EscalationLevel.LEVEL_2_ACTIVE_CONCERN,
                    totalScore, "Acute risk score of " + totalScore + " detected: " + conciseSummary);
        }

        log.info("[RiskEngine] User={} Score={} State={} Factors={}",
                user.getDisplayName(), totalScore, newState, factors.size());

        return RiskAssessmentResponse.builder()
                .id(assessment.getId())
                .userId(user.getId())
                .assessedAt(assessment.getAssessedAt())
                .riskScore(totalScore)
                .state(newState)
                .conciseSummary(conciseSummary)
                .detailedExplanation(detailedExplanation)
                .contributingFactors(factors)
                .build();
    }

    private String buildConciseSummary(RecoveryState state, int score, List<ContributingFactor> factors) {
        String topFactor = factors.stream()
                .filter(f -> f.getImpact() > 0)
                .max(Comparator.comparingInt(ContributingFactor::getImpact))
                .map(f -> " Primary signal: " + formatFactorName(f.getFactor()) + ".")
                .orElse("");

        return switch (state) {
            case STABLE      -> "Recovery pattern is stable and consistent with personal baseline.";
            case DRIFTING    -> "Routine signals have begun to drift from your baseline." + topFactor;
            case CONCERNING  -> "Several routine signals have deviated significantly from baseline. Risk score: " + score + "." + topFactor;
            case ACUTE_RISK  -> "Multiple critical signals are outside your stable baseline. Immediate support is being coordinated. Risk score: " + score + ".";
            case RECOVERING  -> "Recovery trajectory is improving. Signals are returning toward baseline." + topFactor;
        };
    }

    private String buildDetailedExplanation(RecoveryState state, int score,
                                             List<ContributingFactor> factors,
                                             BaselineSnapshot baseline) {
        StringBuilder sb = new StringBuilder();
        sb.append("Current state: ").append(state.name())
          .append(". Risk score: ").append(score).append("/100.\n\n");

        if (factors.isEmpty()) {
            sb.append("All monitored signals are within normal range relative to your personal baseline. ");
            sb.append("Continue with your current routine.");
            return sb.toString();
        }

        List<ContributingFactor> positiveFactors = factors.stream()
                .filter(f -> f.getImpact() > 0)
                .sorted(Comparator.comparingInt(ContributingFactor::getImpact).reversed())
                .toList();

        List<ContributingFactor> protectiveFactors = factors.stream()
                .filter(f -> f.getImpact() < 0)
                .toList();

        if (!positiveFactors.isEmpty()) {
            sb.append("Contributing factors:\n");
            for (ContributingFactor f : positiveFactors) {
                sb.append("• ").append(formatFactorName(f.getFactor()))
                  .append(": ").append(f.getDetails()).append("\n");
            }
        }

        if (!protectiveFactors.isEmpty()) {
            sb.append("\nProtective signals:\n");
            for (ContributingFactor f : protectiveFactors) {
                sb.append("• ").append(formatFactorName(f.getFactor()))
                  .append(": ").append(f.getDetails()).append("\n");
            }
        }

        if (baseline != null) {
            sb.append("\nPersonal baseline (established over ").append(baseline.getStableWindowDays())
              .append(" stable days): morning check-in ")
              .append(String.format("%.0f%%", baseline.getMorningCheckInRate() * 100))
              .append(", medication ")
              .append(String.format("%.0f%%", baseline.getMedicationAdherenceRate() * 100))
              .append(", activity ")
              .append(String.format("%.0f%%", baseline.getActivityRate() * 100))
              .append(".");
        }

        return sb.toString().trim();
    }

    private String formatFactorName(String factor) {
        return switch (factor) {
            case "morning_checkin_drop"      -> "Morning check-in completion";
            case "medication_adherence_drop" -> "Medication adherence";
            case "activity_absence_streak"   -> "Activity absence streak";
            case "meal_logging_decline"      -> "Meal logging rate";
            case "sleep_timing_drift"        -> "Sleep timing shift";
            case "appointment_miss"          -> "Missed appointments";
            case "evening_checkin_decline"   -> "Evening check-in completion";
            case "multi_signal_synergy"      -> "Multiple concurrent risk signals";
            case "re_engagement_bonus"       -> "Recent re-engagement";
            case "denied_evidence_claims"    -> "Denied evidence claims";
            default -> factor.replace("_", " ");
        };
    }

    private boolean isFullyEngaged(DailySignalLog log) {
        return log.isMorningCheckInCompleted()
                && isEffectiveClaim(log, EvidenceSignalType.MORNING_CHECKIN, DailySignalLog::isMorningCheckInCompleted)
                && isEffectiveClaim(log, EvidenceSignalType.MEDICATION, DailySignalLog::isMedicationTaken)
                && isEffectiveClaim(log, EvidenceSignalType.MEAL, DailySignalLog::isMealLogged)
                && isEffectiveClaim(log, EvidenceSignalType.ACTIVITY, DailySignalLog::isActivityLogged)
                && isEffectiveClaim(log, EvidenceSignalType.EVENING_CHECKIN, DailySignalLog::isEveningCheckInCompleted);
    }

    /**
     * A checked-in signal only contributes to risk rates after verification:
     * either the day is fully verified, or this signal type has APPROVED evidence.
     * Pending evidence does not count as an effective (verified) claim.
     */
    private boolean isEffectiveClaim(
            DailySignalLog log,
            EvidenceSignalType signalType,
            java.util.function.Predicate<DailySignalLog> claimPredicate
    ) {
        if (!claimPredicate.test(log)) {
            return false;
        }
        if (log.getVerificationState() == DailyVerificationState.VERIFIED) {
            return true;
        }
        long approved = evidenceRepo.countByDailySignalLogAndSignalTypeAndStatus(log, signalType, VerificationStatus.APPROVED);
        return approved >= requiredApprovalCount(log.getUser(), signalType);
    }

    private int computeEffectiveActivityMissStreak(List<DailySignalLog> logs) {
        if (logs.isEmpty()) return 0;
        List<DailySignalLog> ordered = logs.stream()
                .sorted(Comparator.comparing(DailySignalLog::getLogDate).reversed())
                .toList();

        int streak = 0;
        for (DailySignalLog log : ordered) {
            boolean effectiveActivity = isEffectiveClaim(log, EvidenceSignalType.ACTIVITY, DailySignalLog::isActivityLogged);
            if (effectiveActivity) {
                break;
            }
            streak++;
        }
        return streak;
    }

    private double rate(List<DailySignalLog> logs, java.util.function.Predicate<DailySignalLog> getter) {
        if (logs.isEmpty()) return 1.0;
        return (double) logs.stream().filter(getter).count() / logs.size();
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private int requiredApprovalCount(RecoveryUser user, EvidenceSignalType signalType) {
        if (signalType == EvidenceSignalType.MEAL) {
            Integer expectedMeals = user.getExpectedMealsPerDay();
            if (expectedMeals != null && expectedMeals >= 3) {
                return 3;
            }
        }
        return 1;
    }
}
