package uk.ac.ed.inf.recoveryrhythm.events;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class KafkaEventConsumer {

    @KafkaListener(topics = {
            "routine.signal.logged",
            "baseline.updated",
            "risk.assessed",
            "recovery.state.changed",
            "recovery.episode.opened",
            "recovery.episode.closed",
            "intervention.triggered",
            "escalation.triggered"
    }, groupId = "recovery-rhythm-audit")
    public void consumeEvent(String message) {
        log.info("[Kafka][Audit] Event received: {}", message);
    }
}
