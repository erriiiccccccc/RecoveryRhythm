package uk.ac.ed.inf.recoveryrhythm.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import uk.ac.ed.inf.recoveryrhythm.config.RabbitMqConfig;
import uk.ac.ed.inf.recoveryrhythm.entity.EscalationLevel;
import uk.ac.ed.inf.recoveryrhythm.entity.InterventionType;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RabbitMqInterventionPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishInterventionJob(
            UUID userId,
            InterventionType type,
            String reason,
            UUID episodeId,
            int riskScore,
            String state) {

        InterventionJobMessage job = InterventionJobMessage.builder()
                .jobId(UUID.randomUUID())
                .userId(userId)
                .interventionType(type)
                .reason(reason)
                .relatedEpisodeId(episodeId)
                .scheduledFor(LocalDateTime.now())
                .riskScoreAtTrigger(riskScore)
                .stateAtTrigger(state)
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMqConfig.EXCHANGE,
                RabbitMqConfig.ROUTING_INTERVENTION,
                job
        );

        log.info("[RabbitMQ] Queued intervention job {} for user {} (type={})", job.getJobId(), userId, type);
    }

    public void publishEscalationJob(
            UUID userId,
            UUID supportContactId,
            EscalationLevel level,
            String reason,
            int riskScore) {

        EscalationJobMessage job = EscalationJobMessage.builder()
                .jobId(UUID.randomUUID())
                .userId(userId)
                .supportContactId(supportContactId)
                .level(level)
                .reason(reason)
                .scheduledFor(LocalDateTime.now())
                .riskScoreAtTrigger(riskScore)
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMqConfig.EXCHANGE,
                RabbitMqConfig.ROUTING_ESCALATION,
                job
        );

        log.info("[RabbitMQ] Queued escalation job {} for user {} (level={})", job.getJobId(), userId, level);
    }

    public void publishReentryJob(UUID userId, String reason) {
        InterventionJobMessage job = InterventionJobMessage.builder()
                .jobId(UUID.randomUUID())
                .userId(userId)
                .interventionType(InterventionType.REENTRY_MODE_OFFER)
                .reason(reason)
                .scheduledFor(LocalDateTime.now())
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMqConfig.EXCHANGE,
                RabbitMqConfig.ROUTING_REENTRY,
                job
        );

        log.info("[RabbitMQ] Queued re-entry job {} for user {}", job.getJobId(), userId);
    }
}
