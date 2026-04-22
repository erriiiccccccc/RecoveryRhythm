package uk.ac.ed.inf.recoveryrhythm.dto;

import lombok.Builder;
import lombok.Data;
import uk.ac.ed.inf.recoveryrhythm.entity.RecoveryState;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class RecoveryEpisodeResponse {
    private UUID id;
    private UUID userId;
    private LocalDateTime openedAt;
    private LocalDateTime closedAt;
    private int peakRiskScore;
    private String openingReason;
    private String closingReason;
    private RecoveryState stateAtOpen;
    private RecoveryState stateAtClose;
    private boolean active;
    private long durationHours;
}
