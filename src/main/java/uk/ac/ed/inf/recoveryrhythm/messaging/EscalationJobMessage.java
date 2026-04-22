package uk.ac.ed.inf.recoveryrhythm.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.ac.ed.inf.recoveryrhythm.entity.EscalationLevel;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EscalationJobMessage {

    private UUID jobId;
    private UUID userId;
    private UUID supportContactId;
    private EscalationLevel level;
    private String reason;
    private LocalDateTime scheduledFor;
    private int riskScoreAtTrigger;
}
