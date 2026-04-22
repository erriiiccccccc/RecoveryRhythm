package uk.ac.ed.inf.recoveryrhythm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ed.inf.recoveryrhythm.dto.DailySignalRequest;
import uk.ac.ed.inf.recoveryrhythm.dto.DailySignalResponse;
import uk.ac.ed.inf.recoveryrhythm.entity.DailySignalLog;
import uk.ac.ed.inf.recoveryrhythm.entity.DailyVerificationState;
import uk.ac.ed.inf.recoveryrhythm.entity.RecoveryUser;
import uk.ac.ed.inf.recoveryrhythm.events.KafkaEventPublisher;
import uk.ac.ed.inf.recoveryrhythm.repository.DailySignalLogRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SignalService {

    private final DailySignalLogRepository signalRepo;
    private final UserService userService;
    private final RedisStateService redisStateService;
    private final KafkaEventPublisher kafkaPublisher;
    private final EvidenceService evidenceService;

    @Transactional
    public DailySignalResponse logDailySignal(UUID userId, DailySignalRequest req) {
        RecoveryUser user = userService.requireUser(userId);
        LocalDate logDate = req.getLogDate() != null ? req.getLogDate() : LocalDate.now();

        DailySignalLog existing = signalRepo.findByUserAndLogDate(user, logDate).orElse(null);
        DailySignalLog log_ = existing != null
                ? existing
                : DailySignalLog.builder().user(user).logDate(logDate).build();

        // Same-day updates are additive: once a signal is logged true, it stays true.
        log_.setMorningCheckInCompleted(log_.isMorningCheckInCompleted() || req.isMorningCheckInCompleted());
        log_.setMedicationTaken(log_.isMedicationTaken() || req.isMedicationTaken());
        log_.setMealLogged(log_.isMealLogged() || req.isMealLogged());
        log_.setActivityLogged(log_.isActivityLogged() || req.isActivityLogged());
        log_.setAppointmentScheduled(log_.isAppointmentScheduled() || req.isAppointmentScheduled());
        log_.setAppointmentAttended(log_.isAppointmentAttended() || req.isAppointmentAttended());
        log_.setEveningCheckInCompleted(log_.isEveningCheckInCompleted() || req.isEveningCheckInCompleted());
        if (req.getSleepStartHour() != null) {
            log_.setSleepStartHour(req.getSleepStartHour());
        }
        if (req.getNotes() != null) {
            log_.setNotes(req.getNotes());
        }
        log_.setVerificationState(DailyVerificationState.PENDING);

        log_ = signalRepo.save(log_);
        evidenceService.refreshVerificationState(log_);
        log_ = signalRepo.save(log_);

        redisStateService.updateSignalStreaks(
                userId,
                log_.isMorningCheckInCompleted(),
                log_.isActivityLogged(),
                log_.isEveningCheckInCompleted(),
                log_.isMedicationTaken(),
                log_.isMealLogged()
        );

        kafkaPublisher.publishSignalLogged(userId, Map.of(
                "logDate", logDate.toString(),
                "morningCheckIn", log_.isMorningCheckInCompleted(),
                "medicationTaken", log_.isMedicationTaken(),
                "mealLogged", log_.isMealLogged(),
                "activityLogged", log_.isActivityLogged(),
                "eveningCheckIn", log_.isEveningCheckInCompleted()
        ));

        log.info("Signal logged for user {} on {}", user.getDisplayName(), logDate);
        return toResponse(log_);
    }

    public List<DailySignalResponse> getSignals(UUID userId) {
        RecoveryUser user = userService.requireUser(userId);
        return signalRepo.findByUserOrderByLogDateDesc(user).stream()
                .map(this::toResponse).toList();
    }

    public List<DailySignalResponse> getRecentSignals(UUID userId) {
        RecoveryUser user = userService.requireUser(userId);
        return signalRepo.findTop7ByUserOrderByLogDateDesc(user).stream()
                .map(this::toResponse).toList();
    }

    public List<DailySignalLog> getRecentSignalEntities(RecoveryUser user, int days) {
        LocalDate from = LocalDate.now().minusDays(days);
        return signalRepo.findByUserAndLogDateBetweenOrderByLogDateAsc(user, from, LocalDate.now());
    }

    public List<DailySignalLog> getStableWindowEntities(RecoveryUser user, LocalDate from, LocalDate to) {
        return signalRepo.findByUserAndLogDateBetweenOrderByLogDateAsc(user, from, to);
    }

    public int countLoggedDays(RecoveryUser user) {
        return (int) signalRepo.countByUser(user);
    }

    public DailySignalResponse toResponse(DailySignalLog s) {
        int completed = countCompleted(s);
        EvidenceService.EvidenceRollup evidenceRollup = evidenceService.summarizeSignalEvidence(s);
        return DailySignalResponse.builder()
                .id(s.getId())
                .userId(s.getUser().getId())
                .logDate(s.getLogDate())
                .morningCheckInCompleted(s.isMorningCheckInCompleted())
                .medicationTaken(s.isMedicationTaken())
                .mealLogged(s.isMealLogged())
                .activityLogged(s.isActivityLogged())
                .appointmentScheduled(s.isAppointmentScheduled())
                .appointmentAttended(s.isAppointmentAttended())
                .eveningCheckInCompleted(s.isEveningCheckInCompleted())
                .sleepStartHour(s.getSleepStartHour())
                .notes(s.getNotes())
                .loggedAt(s.getLoggedAt())
                .signalsCompletedCount(completed)
                .signalsTotalCount(6)
                .verificationState(evidenceRollup.state())
                .pendingEvidenceCount(evidenceRollup.pendingCount())
                .deniedEvidenceCount(evidenceRollup.deniedCount())
                .build();
    }

    private int countCompleted(DailySignalLog s) {
        int count = 0;
        if (s.isMorningCheckInCompleted()) count++;
        if (s.isMedicationTaken()) count++;
        if (s.isMealLogged()) count++;
        if (s.isActivityLogged()) count++;
        if (s.isEveningCheckInCompleted()) count++;
        if (s.getSleepStartHour() != null) count++;
        return count;
    }
}
