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

        DailySignalLog log_ = signalRepo.findByUserAndLogDate(user, logDate)
                .orElseGet(() -> DailySignalLog.builder().user(user).logDate(logDate).build());

        log_.setMorningCheckInCompleted(req.isMorningCheckInCompleted());
        log_.setMedicationTaken(req.isMedicationTaken());
        log_.setMealLogged(req.isMealLogged());
        log_.setActivityLogged(req.isActivityLogged());
        log_.setAppointmentScheduled(req.isAppointmentScheduled());
        log_.setAppointmentAttended(req.isAppointmentAttended());
        log_.setEveningCheckInCompleted(req.isEveningCheckInCompleted());
        log_.setSleepStartHour(req.getSleepStartHour());
        log_.setNotes(req.getNotes());
        log_.setVerificationState(DailyVerificationState.PENDING);

        log_ = signalRepo.save(log_);
        evidenceService.refreshVerificationState(log_);
        log_ = signalRepo.save(log_);

        redisStateService.updateSignalStreaks(
                userId,
                req.isMorningCheckInCompleted(),
                req.isActivityLogged(),
                req.isEveningCheckInCompleted(),
                req.isMedicationTaken(),
                req.isMealLogged()
        );

        kafkaPublisher.publishSignalLogged(userId, Map.of(
                "logDate", logDate.toString(),
                "morningCheckIn", req.isMorningCheckInCompleted(),
                "medicationTaken", req.isMedicationTaken(),
                "mealLogged", req.isMealLogged(),
                "activityLogged", req.isActivityLogged(),
                "eveningCheckIn", req.isEveningCheckInCompleted()
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
