package uk.ac.ed.inf.recoveryrhythm.dto;

import lombok.Builder;
import lombok.Data;
import uk.ac.ed.inf.recoveryrhythm.entity.EscalationLevel;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class EscalationResponse {
    private UUID id;
    private UUID userId;
    private UUID supportContactId;
    private String supportContactName;
    private LocalDateTime triggeredAt;
    private EscalationLevel level;
    private String reason;
    private String outcomeStatus;
}
