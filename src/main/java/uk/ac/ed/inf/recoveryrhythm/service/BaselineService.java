package uk.ac.ed.inf.recoveryrhythm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ed.inf.recoveryrhythm.dto.BaselineSnapshotResponse;
import uk.ac.ed.inf.recoveryrhythm.engine.BaselineEngine;
import uk.ac.ed.inf.recoveryrhythm.entity.BaselineSnapshot;
import uk.ac.ed.inf.recoveryrhythm.entity.RecoveryUser;
import uk.ac.ed.inf.recoveryrhythm.events.KafkaEventPublisher;
import uk.ac.ed.inf.recoveryrhythm.repository.BaselineSnapshotRepository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BaselineService {

    private final BaselineSnapshotRepository baselineRepo;
    private final BaselineEngine baselineEngine;
    private final UserService userService;
    private final KafkaEventPublisher kafkaPublisher;

    @Transactional
    public BaselineSnapshotResponse recalculate(UUID userId) {
        RecoveryUser user = userService.requireUser(userId);
        BaselineSnapshot snapshot = baselineEngine.calculate(user);

        baselineRepo.deactivateAllForUser(user);
        snapshot.setActive(true);
        snapshot = baselineRepo.save(snapshot);

        kafkaPublisher.publishBaselineUpdated(userId, Map.of(
                "morningCheckInRate", snapshot.getMorningCheckInRate(),
                "medicationAdherenceRate", snapshot.getMedicationAdherenceRate(),
                "activityRate", snapshot.getActivityRate(),
                "stableWindowDays", snapshot.getStableWindowDays()
        ));

        log.info("Baseline recalculated for user {} (window={}d)", user.getDisplayName(), snapshot.getStableWindowDays());
        return toResponse(snapshot);
    }

    public Optional<BaselineSnapshot> getActiveBaseline(RecoveryUser user) {
        return baselineRepo.findByUserAndActiveTrue(user);
    }

    public BaselineSnapshotResponse getLatestBaseline(UUID userId) {
        RecoveryUser user = userService.requireUser(userId);
        return baselineRepo.findByUserAndActiveTrue(user)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalStateException("No baseline found for user " + userId));
    }

    public BaselineSnapshotResponse toResponse(BaselineSnapshot b) {
        return BaselineSnapshotResponse.builder()
                .id(b.getId())
                .userId(b.getUser().getId())
                .createdAt(b.getCreatedAt())
                .morningCheckInRate(b.getMorningCheckInRate())
                .medicationAdherenceRate(b.getMedicationAdherenceRate())
                .mealLoggingRate(b.getMealLoggingRate())
                .activityRate(b.getActivityRate())
                .appointmentAttendanceRate(b.getAppointmentAttendanceRate())
                .eveningCheckInRate(b.getEveningCheckInRate())
                .averageSleepStartHour(b.getAverageSleepStartHour())
                .stableWindowDays(b.getStableWindowDays())
                .active(b.isActive())
                .build();
    }
}
