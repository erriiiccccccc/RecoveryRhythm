package uk.ac.ed.inf.recoveryrhythm.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.ac.ed.inf.recoveryrhythm.entity.InterventionType;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterventionJobMessage {

    private UUID jobId;
    private UUID userId;
    private InterventionType interventionType;
    private String reason;
    private UUID relatedEpisodeId;
    private LocalDateTime scheduledFor;
    private int riskScoreAtTrigger;
    private String stateAtTrigger;
}
