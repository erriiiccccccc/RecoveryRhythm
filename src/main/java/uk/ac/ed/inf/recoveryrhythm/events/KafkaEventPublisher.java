package uk.ac.ed.inf.recoveryrhythm.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishSignalLogged(UUID userId, Map<String, Object> signalDetails) {
        publish("routine.signal.logged", userId, signalDetails);
    }

    public void publishBaselineUpdated(UUID userId, Map<String, Object> baselineDetails) {
        publish("baseline.updated", userId, baselineDetails);
    }

    public void publishRiskAssessed(UUID userId, int riskScore, String state) {
        publish("risk.assessed", userId, Map.of(
                "riskScore", riskScore,
                "state", state
        ));
    }

    public void publishStateChanged(UUID userId, String previousState, String newState, int riskScore) {
        publish("recovery.state.changed", userId, Map.of(
                "previousState", previousState,
                "newState", newState,
                "riskScore", riskScore
        ));
    }

    public void publishEpisodeOpened(UUID userId, UUID episodeId, String reason) {
        publish("recovery.episode.opened", userId, Map.of(
                "episodeId", episodeId.toString(),
                "reason", reason
        ));
    }

    public void publishEpisodeClosed(UUID userId, UUID episodeId, String reason) {
        publish("recovery.episode.closed", userId, Map.of(
                "episodeId", episodeId.toString(),
                "reason", reason
        ));
    }

    public void publishInterventionTriggered(UUID userId, String interventionType, String reason) {
        publish("intervention.triggered", userId, Map.of(
                "interventionType", interventionType,
                "reason", reason
        ));
    }

    public void publishEscalationTriggered(UUID userId, String level, String reason) {
        publish("escalation.triggered", userId, Map.of(
                "level", level,
                "reason", reason
        ));
    }

    private void publish(String topic, UUID userId, Map<String, Object> payload) {
        try {
            DomainEvent event = DomainEvent.of(topic, userId, payload);
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, userId.toString(), json);
            log.info("[Kafka] Published {} for user {}", topic, userId);
        } catch (JsonProcessingException e) {
            log.error("[Kafka] Failed to publish event {}: {}", topic, e.getMessage());
        }
    }
}
