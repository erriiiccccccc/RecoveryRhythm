package uk.ac.ed.inf.recoveryrhythm.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import uk.ac.ed.inf.recoveryrhythm.config.RabbitMqConfig;
import uk.ac.ed.inf.recoveryrhythm.entity.*;
import uk.ac.ed.inf.recoveryrhythm.repository.*;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class RabbitMqInterventionConsumer {

    private final InterventionRecordRepository interventionRepo;
    private final EscalationRecordRepository escalationRepo;
    private final SupportContactRepository supportContactRepo;
    private final RecoveryUserRepository userRepo;

    @RabbitListener(queues = RabbitMqConfig.INTERVENTION_QUEUE)
    public void handleInterventionJob(InterventionJobMessage job) {
        log.info("[RabbitMQ] Processing intervention job {} for user {} type={}",
                job.getJobId(), job.getUserId(), job.getInterventionType());

        Optional<RecoveryUser> userOpt = userRepo.findById(job.getUserId());
        if (userOpt.isEmpty()) {
            log.warn("[RabbitMQ] User {} not found, skipping intervention job", job.getUserId());
            return;
        }

        RecoveryUser user = userOpt.get();
        String message = buildInterventionMessage(job.getInterventionType(), user.getDisplayName());

        InterventionRecord record = InterventionRecord.builder()
                .user(user)
                .triggeredAt(LocalDateTime.now())
                .interventionType(job.getInterventionType())
                .message(message)
                .relatedEpisodeId(job.getRelatedEpisodeId())
                .status(InterventionStatus.SENT)
                .reason(job.getReason())
                .build();

        interventionRepo.save(record);
        log.info("[Intervention][SENT] {} → {}: \"{}\"", job.getInterventionType(), user.getDisplayName(), message);
    }

    @RabbitListener(queues = RabbitMqConfig.ESCALATION_QUEUE)
    public void handleEscalationJob(EscalationJobMessage job) {
        log.info("[RabbitMQ] Processing escalation job {} for user {} level={}",
                job.getJobId(), job.getUserId(), job.getLevel());

        Optional<RecoveryUser> userOpt = userRepo.findById(job.getUserId());
        if (userOpt.isEmpty()) {
            log.warn("[RabbitMQ] User {} not found, skipping escalation job", job.getUserId());
            return;
        }

        RecoveryUser user = userOpt.get();
        SupportContact contact = null;

        if (job.getSupportContactId() != null) {
            contact = supportContactRepo.findById(job.getSupportContactId()).orElse(null);
        }

        if (contact == null) {
            var contacts = supportContactRepo.findByUserAndEscalationEnabledTrue(user);
            if (!contacts.isEmpty()) contact = contacts.get(0);
        }

        EscalationRecord record = EscalationRecord.builder()
                .user(user)
                .supportContact(contact)
                .triggeredAt(LocalDateTime.now())
                .level(job.getLevel())
                .reason(job.getReason())
                .outcomeStatus("NOTIFIED")
                .build();

        escalationRepo.save(record);

        String contactName = contact != null ? contact.getName() : "no contact configured";
        log.info("[Escalation][SENT] Level {} → {} notified: {} — Reason: {}",
                job.getLevel(), contactName, user.getDisplayName(), job.getReason());
    }

    @RabbitListener(queues = RabbitMqConfig.REENTRY_QUEUE)
    public void handleReentryJob(InterventionJobMessage job) {
        log.info("[RabbitMQ] Processing re-entry job {} for user {}", job.getJobId(), job.getUserId());

        Optional<RecoveryUser> userOpt = userRepo.findById(job.getUserId());
        if (userOpt.isEmpty()) return;

        RecoveryUser user = userOpt.get();
        user.setReentryModeActive(true);
        userRepo.save(user);

        InterventionRecord record = InterventionRecord.builder()
                .user(user)
                .triggeredAt(LocalDateTime.now())
                .interventionType(InterventionType.REENTRY_MODE_OFFER)
                .message("Hi " + user.getDisplayName() + " — we've noticed you've been away for a bit. " +
                         "No pressure at all. When you're ready, just confirm you're awake or log one small thing. That's enough for today.")
                .status(InterventionStatus.SENT)
                .reason(job.getReason())
                .build();

        interventionRepo.save(record);
        log.info("[Re-entry] Mode activated for user {}", user.getDisplayName());
    }

    private String buildInterventionMessage(InterventionType type, String name) {
        return switch (type) {
            case GENTLE_REMINDER ->
                "Hi " + name + " — just a gentle check-in. How are you doing today? Logging even one thing helps us support you better.";
            case FOLLOW_UP_RECHECK ->
                "Hi " + name + " — following up on our earlier check-in. Remember, there's no pressure. We're just here when you're ready.";
            case ENHANCED_CHECKIN ->
                "Hi " + name + " — we've noticed your routine has shifted a bit lately. Would you like to do a short check-in with us today?";
            case LOW_PRESSURE_MESSAGE ->
                "Hi " + name + " — no need to do anything right now. We're thinking of you and we're here whenever you need support.";
            case REENTRY_MODE_OFFER ->
                "Hi " + name + " — welcome back. Today, you only need to do one thing. Confirm you're awake, log one meal, or just say hello. That's it.";
            case REENTRY_MINIMAL_TASK ->
                "Hi " + name + " — great to see you. Your only task today: confirm you've taken your medication or had a meal. Small steps count.";
            case ESCALATION_PREPARATION ->
                "Hi " + name + " — we want to make sure you're okay. If you'd like, your support contact has been informed that you might need a check-in.";
            case ESCALATION_EXECUTION ->
                "Hi " + name + " — your support contact has been notified and may reach out to you soon. You are not alone.";
            case RECOVERY_ACKNOWLEDGEMENT ->
                "Hi " + name + " — we've noticed your routine is improving. That's real progress. Keep going at your own pace.";
        };
    }
}
