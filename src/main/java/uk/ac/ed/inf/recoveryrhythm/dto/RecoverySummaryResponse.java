package uk.ac.ed.inf.recoveryrhythm.dto;

import lombok.Builder;
import lombok.Data;
import uk.ac.ed.inf.recoveryrhythm.entity.RecoveryState;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class RecoverySummaryResponse {

    private UUID userId;
    private String displayName;
    private LocalDate recoveryStartDate;
    private int daysSinceRecoveryStart;

    private RecoveryState currentState;
    private int currentRiskScore;
    private String stateLabel;
    private String stateColorHint;

    private String conciseSummary;
    private String detailedExplanation;
    private List<ContributingFactor> contributingFactors;

    private BaselineSnapshotResponse baselineSummary;

    private String recentTrend;
    private List<TrendPoint> trendHistory;

    private RecoveryEpisodeResponse activeEpisode;

    private List<InterventionResponse> latestInterventions;
    private List<EscalationResponse> latestEscalations;

    private boolean reentryModeActive;
    private String reentryPrompt;

    private List<DailySignalResponse> recentSignals;
    private int pendingEvidenceCount;
    private int approvedEvidenceCount;
    private int deniedEvidenceCount;
    private String verificationConfidence;

    private LocalDateTime lastAssessedAt;

    @Data
    @Builder
    public static class TrendPoint {
        private LocalDateTime assessedAt;
        private int riskScore;
        private RecoveryState state;
    }
}
