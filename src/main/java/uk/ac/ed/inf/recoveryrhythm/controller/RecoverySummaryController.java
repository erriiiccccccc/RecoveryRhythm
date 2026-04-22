package uk.ac.ed.inf.recoveryrhythm.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.ac.ed.inf.recoveryrhythm.dto.*;
import uk.ac.ed.inf.recoveryrhythm.engine.StateEngine;
import uk.ac.ed.inf.recoveryrhythm.entity.*;
import uk.ac.ed.inf.recoveryrhythm.repository.RiskAssessmentRepository;
import uk.ac.ed.inf.recoveryrhythm.service.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/users/{userId}/recovery-summary")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class RecoverySummaryController {

    private final UserService userService;
    private final BaselineService baselineService;
    private final RiskService riskService;
    private final EpisodeService episodeService;
    private final InterventionService interventionService;
    private final EscalationService escalationService;
    private final SignalService signalService;
    private final EvidenceService evidenceService;
    private final RiskAssessmentRepository assessmentRepo;
    private final RedisStateService redisState;
    private final StateEngine stateEngine;
    private final ObjectMapper objectMapper;

    @GetMapping
    public ResponseEntity<RecoverySummaryResponse> getSummary(@PathVariable UUID userId) {
        RecoveryUser user = userService.requireUser(userId);

        // Latest risk assessment
        Optional<RiskAssessment> latestAssessment = assessmentRepo.findTopByUserOrderByAssessedAtDesc(user);

        // Trend history (last 10 assessments)
        List<RiskAssessment> historyEntities = assessmentRepo.findTop10ByUserOrderByAssessedAtDesc(user);
        List<RecoverySummaryResponse.TrendPoint> trendHistory = historyEntities.stream()
                .map(a -> RecoverySummaryResponse.TrendPoint.builder()
                        .assessedAt(a.getAssessedAt())
                        .riskScore(a.getRiskScore())
                        .state(a.getState())
                        .build())
                .toList();

        // Trend direction
        String recentTrend = computeTrend(historyEntities);

        // Active episode
        Optional<RecoveryEpisodeResponse> activeEpisode = episodeService.getActiveEpisodeResponse(userId);

        // Baseline
        BaselineSnapshotResponse baseline = null;
        try {
            baseline = baselineService.getLatestBaseline(userId);
        } catch (Exception ignored) {}

        // Contributing factors from latest assessment
        List<ContributingFactor> factors = List.of();
        if (latestAssessment.isPresent() && latestAssessment.get().getFactorBreakdownJson() != null) {
            try {
                factors = objectMapper.readValue(latestAssessment.get().getFactorBreakdownJson(),
                        new TypeReference<List<ContributingFactor>>() {});
            } catch (Exception e) {
                log.warn("Could not parse factors: {}", e.getMessage());
            }
        }

        // Recent signals (last 7 days)
        List<DailySignalResponse> recentSignals = signalService.getRecentSignals(userId);
        List<DailySignalLog> recentSignalEntities = signalService.getRecentSignalEntities(user, 7);
        EvidenceService.UserEvidenceSummary evidenceSummary = evidenceService.summarizeUserEvidence(recentSignalEntities);

        // Days since recovery start
        long daysSince = ChronoUnit.DAYS.between(user.getRecoveryStartDate(), LocalDate.now());

        // Re-entry prompt
        String reentryPrompt = null;
        if (user.isReentryModeActive()) {
            reentryPrompt = "No pressure — your only task today is one small thing. " +
                    "Confirm you're awake, log a meal, or check your medication. That's all.";
        }

        RecoverySummaryResponse summary = RecoverySummaryResponse.builder()
                .userId(user.getId())
                .displayName(user.getDisplayName())
                .recoveryStartDate(user.getRecoveryStartDate())
                .daysSinceRecoveryStart((int) daysSince)
                .currentState(user.getCurrentState())
                .currentRiskScore(user.getCurrentRiskScore())
                .stateLabel(stateEngine.stateLabel(user.getCurrentState()))
                .stateColorHint(stateEngine.stateColorHint(user.getCurrentState()))
                .conciseSummary(latestAssessment.map(RiskAssessment::getConciseSummary)
                        .orElse("No assessment available yet."))
                .detailedExplanation(latestAssessment.map(RiskAssessment::getDetailedExplanation)
                        .orElse("Log daily signals and run a risk assessment to see detailed analysis."))
                .contributingFactors(factors)
                .baselineSummary(baseline)
                .recentTrend(recentTrend)
                .trendHistory(trendHistory)
                .activeEpisode(activeEpisode.orElse(null))
                .latestInterventions(interventionService.getRecentInterventions(userId))
                .latestEscalations(escalationService.getRecentEscalations(userId))
                .reentryModeActive(user.isReentryModeActive())
                .reentryPrompt(reentryPrompt)
                .recentSignals(recentSignals)
                .pendingEvidenceCount(evidenceSummary.pendingCount())
                .approvedEvidenceCount(evidenceSummary.approvedCount())
                .deniedEvidenceCount(evidenceSummary.deniedCount())
                .verificationConfidence(computeVerificationConfidence(evidenceSummary))
                .lastAssessedAt(latestAssessment.map(RiskAssessment::getAssessedAt).orElse(null))
                .build();

        return ResponseEntity.ok(summary);
    }

    private String computeTrend(List<RiskAssessment> history) {
        if (history.size() < 2) return "INSUFFICIENT_DATA";
        int latest = history.get(0).getRiskScore();
        int previous = history.get(1).getRiskScore();
        int diff = latest - previous;
        if (diff <= -10) return "IMPROVING";
        if (diff >= 10)  return "WORSENING";
        return "STABLE";
    }

    private String computeVerificationConfidence(EvidenceService.UserEvidenceSummary summary) {
        int total = summary.pendingCount() + summary.approvedCount() + summary.deniedCount();
        if (total == 0) return "LOW";
        double approvedRate = (double) summary.approvedCount() / total;
        if (approvedRate >= 0.8 && summary.pendingCount() == 0) return "HIGH";
        if (approvedRate >= 0.5) return "MEDIUM";
        return "LOW";
    }
}
