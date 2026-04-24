package uk.ac.ed.inf.recoveryrhythm.engine;

import org.springframework.stereotype.Component;
import uk.ac.ed.inf.recoveryrhythm.entity.RecoveryState;
import uk.ac.ed.inf.recoveryrhythm.entity.RiskAssessment;

import java.util.List;

/**
 * Maps a computed risk score and recent assessment history to a RecoveryState.
 *
 * Score thresholds:
 *   0–24  → STABLE
 *   25–49 → DRIFTING
 *   50–79 → CONCERNING
 *   80–100 → ACUTE_RISK
 *
 * RECOVERING override: if the previous state was CONCERNING or ACUTE_RISK
 * and the current score has dropped to DRIFTING range with a consistent
 * downward trend over recent assessments, we classify as RECOVERING.
 */
@Component
public class StateEngine {

    public RecoveryState determineState(int riskScore, RecoveryState previousState,
                                        List<RiskAssessment> recentHistory) {
        RecoveryState baseState = fromScore(riskScore);

        if (baseState == RecoveryState.DRIFTING || baseState == RecoveryState.STABLE) {
            if (isRecovering(previousState, riskScore, recentHistory)) {
                return RecoveryState.RECOVERING;
            }
        }

        return baseState;
    }

    public RecoveryState fromScore(int score) {
        if (score < 25)  return RecoveryState.STABLE;
        if (score < 50)  return RecoveryState.DRIFTING;
        if (score < 80)  return RecoveryState.CONCERNING;
        return RecoveryState.ACUTE_RISK;
    }

    private boolean isRecovering(RecoveryState previousState, int currentScore,
                                  List<RiskAssessment> history) {
        if (previousState != RecoveryState.CONCERNING && previousState != RecoveryState.ACUTE_RISK) {
            return false;
        }
        if (history == null || history.size() < 2) return false;

        // Check that the last 2 assessments show a downward score trend
        int prev1 = history.get(0).getRiskScore();
        int prev2 = history.get(1).getRiskScore();
        return currentScore < prev1 && prev1 < prev2;
    }

    public String stateLabel(RecoveryState state) {
        return switch (state) {
            case STABLE      -> "Stable";
            case DRIFTING    -> "Drifting";
            case CONCERNING  -> "Concerning";
            case ACUTE_RISK  -> "Acute Risk";
            case RECOVERING  -> "Recovering";
        };
    }

    public String stateColorHint(RecoveryState state) {
        return switch (state) {
            case STABLE      -> "#2ed573";
            case DRIFTING    -> "#ffaa00";
            case CONCERNING  -> "#ff6b35";
            case ACUTE_RISK  -> "#ff4757";
            case RECOVERING  -> "#00d4ff";
        };
    }
}
