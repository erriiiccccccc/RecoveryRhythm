package uk.ac.ed.inf.recoveryrhythm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.ac.ed.inf.recoveryrhythm.dto.InterventionResponse;
import uk.ac.ed.inf.recoveryrhythm.entity.*;
import uk.ac.ed.inf.recoveryrhythm.events.KafkaEventPublisher;
import uk.ac.ed.inf.recoveryrhythm.messaging.RabbitMqInterventionPublisher;
import uk.ac.ed.inf.recoveryrhythm.repository.InterventionRecordRepository;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InterventionService {

    private final InterventionRecordRepository interventionRepo;
    private final RabbitMqInterventionPublisher rabbitPublisher;
    private final RedisStateService redisState;
    private final UserService userService;
    private final KafkaEventPublisher kafkaPublisher;

    public void triggerInterventionsForState(RecoveryUser user, RecoveryState state,
                                              int riskScore, RecoveryEpisode episode) {
        UUID episodeId = episode != null ? episode.getId() : null;
        String stateName = state.name();

        switch (state) {
            case DRIFTING -> {
                queueIfNotLocked(user, InterventionType.GENTLE_REMINDER,
                        "User state drifted from baseline", episodeId, riskScore, stateName);
                queueIfNotLocked(user, InterventionType.FOLLOW_UP_RECHECK,
                        "Follow-up scheduled after drifting signal", episodeId, riskScore, stateName);
            }
            case CONCERNING -> {
                queueIfNotLocked(user, InterventionType.ENHANCED_CHECKIN,
                        "Concerning pattern detected", episodeId, riskScore, stateName);
                queueIfNotLocked(user, InterventionType.LOW_PRESSURE_MESSAGE,
                        "Supportive low-pressure message during concerning period", episodeId, riskScore, stateName);
            }
            case ACUTE_RISK -> {
                queueIfNotLocked(user, InterventionType.ENHANCED_CHECKIN,
                        "Acute risk: immediate check-in required", episodeId, riskScore, stateName);
                queueIfNotLocked(user, InterventionType.ESCALATION_PREPARATION,
                        "Supporter alert preparation for acute risk", episodeId, riskScore, stateName);
            }
            case RECOVERING -> {
                queueIfNotLocked(user, InterventionType.RECOVERY_ACKNOWLEDGEMENT,
                        "Recovery trend acknowledged", episodeId, riskScore, stateName);
            }
            default -> { /* STABLE — no intervention needed */ }
        }
    }

    public void triggerReentryMode(RecoveryUser user, String reason) {
        redisState.setReentryMode(user.getId(), true);
        rabbitPublisher.publishReentryJob(user.getId(), reason);
        kafkaPublisher.publishInterventionTriggered(user.getId(), InterventionType.REENTRY_MODE_OFFER.name(), reason);
    }

    public List<InterventionResponse> getInterventions(UUID userId) {
        RecoveryUser user = userService.requireUser(userId);
        return interventionRepo.findByUserOrderByTriggeredAtDesc(user).stream()
                .map(this::toResponse).toList();
    }

    public List<InterventionResponse> getRecentInterventions(UUID userId) {
        RecoveryUser user = userService.requireUser(userId);
        return interventionRepo.findTop10ByUserOrderByTriggeredAtDesc(user).stream()
                .map(this::toResponse).toList();
    }

    private void queueIfNotLocked(RecoveryUser user, InterventionType type, String reason,
                                   UUID episodeId, int riskScore, String state) {
        boolean locked = !redisState.acquireInterventionLock(user.getId(), type.name(), Duration.ofHours(1));
        if (locked) {
            log.debug("[Intervention] Skipping {} for {} — dedup lock active", type, user.getDisplayName());
            return;
        }
        rabbitPublisher.publishInterventionJob(user.getId(), type, reason, episodeId, riskScore, state);
        kafkaPublisher.publishInterventionTriggered(user.getId(), type.name(), reason);
    }

    public InterventionResponse toResponse(InterventionRecord r) {
        return InterventionResponse.builder()
                .id(r.getId())
                .userId(r.getUser().getId())
                .triggeredAt(r.getTriggeredAt())
                .interventionType(r.getInterventionType())
                .message(r.getMessage())
                .relatedEpisodeId(r.getRelatedEpisodeId())
                .status(r.getStatus())
                .reason(r.getReason())
                .build();
    }
}
