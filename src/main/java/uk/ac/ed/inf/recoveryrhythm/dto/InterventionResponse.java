package uk.ac.ed.inf.recoveryrhythm.dto;

import lombok.Builder;
import lombok.Data;
import uk.ac.ed.inf.recoveryrhythm.entity.InterventionStatus;
import uk.ac.ed.inf.recoveryrhythm.entity.InterventionType;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class InterventionResponse {
    private UUID id;
    private UUID userId;
    private LocalDateTime triggeredAt;
    private InterventionType interventionType;
    private String message;
    private UUID relatedEpisodeId;
    private InterventionStatus status;
    private String reason;
}
