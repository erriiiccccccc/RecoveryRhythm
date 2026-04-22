package uk.ac.ed.inf.recoveryrhythm.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DomainEvent {

    private String eventId;
    private String eventType;
    private String userId;
    private LocalDateTime occurredAt;
    private Map<String, Object> payload;

    public static DomainEvent of(String eventType, UUID userId, Map<String, Object> payload) {
        return DomainEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .userId(userId.toString())
                .occurredAt(LocalDateTime.now())
                .payload(payload)
                .build();
    }
}
