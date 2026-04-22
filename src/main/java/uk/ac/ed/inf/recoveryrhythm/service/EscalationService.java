package uk.ac.ed.inf.recoveryrhythm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.ac.ed.inf.recoveryrhythm.dto.EscalationResponse;
import uk.ac.ed.inf.recoveryrhythm.entity.*;
import uk.ac.ed.inf.recoveryrhythm.events.KafkaEventPublisher;
import uk.ac.ed.inf.recoveryrhythm.messaging.RabbitMqInterventionPublisher;
import uk.ac.ed.inf.recoveryrhythm.repository.EscalationRecordRepository;
import uk.ac.ed.inf.recoveryrhythm.repository.SupportContactRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EscalationService {

    private final EscalationRecordRepository escalationRepo;
    private final SupportContactRepository supportContactRepo;
    private final RabbitMqInterventionPublisher rabbitPublisher;
    private final UserService userService;
    private final KafkaEventPublisher kafkaPublisher;

    public void triggerEscalation(RecoveryUser user, EscalationLevel level, int riskScore, String reason) {
        var contacts = supportContactRepo.findByUserAndEscalationEnabledTrue(user);
        UUID contactId = contacts.isEmpty() ? null : contacts.get(0).getId();

        rabbitPublisher.publishEscalationJob(user.getId(), contactId, level, reason, riskScore);
        kafkaPublisher.publishEscalationTriggered(user.getId(), level.name(), reason);

        log.info("[Escalation] {} triggered for user {} (score={})", level, user.getDisplayName(), riskScore);
    }

    public List<EscalationResponse> getEscalations(UUID userId) {
        RecoveryUser user = userService.requireUser(userId);
        return escalationRepo.findByUserOrderByTriggeredAtDesc(user).stream()
                .map(this::toResponse).toList();
    }

    public List<EscalationResponse> getRecentEscalations(UUID userId) {
        RecoveryUser user = userService.requireUser(userId);
        return escalationRepo.findTop5ByUserOrderByTriggeredAtDesc(user).stream()
                .map(this::toResponse).toList();
    }

    public EscalationResponse toResponse(EscalationRecord r) {
        return EscalationResponse.builder()
                .id(r.getId())
                .userId(r.getUser().getId())
                .supportContactId(r.getSupportContact() != null ? r.getSupportContact().getId() : null)
                .supportContactName(r.getSupportContact() != null ? r.getSupportContact().getName() : null)
                .triggeredAt(r.getTriggeredAt())
                .level(r.getLevel())
                .reason(r.getReason())
                .outcomeStatus(r.getOutcomeStatus())
                .build();
    }
}
