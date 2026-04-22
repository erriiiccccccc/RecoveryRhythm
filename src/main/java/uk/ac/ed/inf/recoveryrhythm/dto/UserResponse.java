package uk.ac.ed.inf.recoveryrhythm.dto;

import lombok.Builder;
import lombok.Data;
import uk.ac.ed.inf.recoveryrhythm.entity.RecoveryState;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class UserResponse {
    private UUID id;
    private String displayName;
    private LocalDate recoveryStartDate;
    private RecoveryState currentState;
    private int currentRiskScore;
    private boolean reentryModeActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
