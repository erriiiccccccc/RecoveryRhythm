package uk.ac.ed.inf.recoveryrhythm.dto;

import lombok.Builder;
import lombok.Data;
import uk.ac.ed.inf.recoveryrhythm.entity.RecoveryState;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class RiskAssessmentResponse {
    private UUID id;
    private UUID userId;
    private LocalDateTime assessedAt;
    private int riskScore;
    private RecoveryState state;
    private String conciseSummary;
    private String detailedExplanation;
    private List<ContributingFactor> contributingFactors;
}
