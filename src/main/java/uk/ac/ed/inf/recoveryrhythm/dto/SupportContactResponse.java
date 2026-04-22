package uk.ac.ed.inf.recoveryrhythm.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class SupportContactResponse {
    private UUID id;
    private UUID userId;
    private String name;
    private String relationship;
    private String contactChannel;
    private String contactValue;
    private boolean escalationEnabled;
    private LocalDateTime createdAt;
}
