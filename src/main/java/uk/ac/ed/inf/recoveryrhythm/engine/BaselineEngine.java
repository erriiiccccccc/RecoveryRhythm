package uk.ac.ed.inf.recoveryrhythm.engine;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.ac.ed.inf.recoveryrhythm.entity.BaselineSnapshot;
import uk.ac.ed.inf.recoveryrhythm.entity.DailySignalLog;
import uk.ac.ed.inf.recoveryrhythm.entity.RecoveryUser;
import uk.ac.ed.inf.recoveryrhythm.repository.DailySignalLogRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.OptionalDouble;

@Component
@RequiredArgsConstructor
@Slf4j
public class BaselineEngine {

    private final DailySignalLogRepository signalRepo;

    @Value("${recovery-rhythm.baseline.stable-window-days:5}")
    private int stableWindowDays;

    @Value("${recovery-rhythm.baseline.minimum-days-required:3}")
    private int minimumDaysRequired;

    /**
     * Calculates a baseline snapshot from the user's earliest logged signals.
     * We use the first N days of data as the "stable period" since this represents
     * the freshly-structured post-discharge phase.
     */
    public BaselineSnapshot calculate(RecoveryUser user) {
        LocalDate startDate = user.getRecoveryStartDate();
        LocalDate endDate = startDate.plusDays(stableWindowDays - 1);

        List<DailySignalLog> stableLogs = signalRepo.findByUserAndLogDateBetweenOrderByLogDateAsc(
                user, startDate, endDate);

        if (stableLogs.size() < minimumDaysRequired) {
            // Fall back to most recent available logs if stable window is sparse
            List<DailySignalLog> allLogs = signalRepo.findTop14ByUserOrderByLogDateDesc(user);
            if (allLogs.isEmpty()) {
                log.warn("No signal data for user {}, creating empty baseline", user.getDisplayName());
                return emptyBaseline(user);
            }
            stableLogs = allLogs.subList(0, Math.min(stableWindowDays, allLogs.size()));
        }

        int n = stableLogs.size();

        double morningRate = rate(stableLogs, DailySignalLog::isMorningCheckInCompleted);
        double medRate = rate(stableLogs, DailySignalLog::isMedicationTaken);
        double mealRate = rate(stableLogs, DailySignalLog::isMealLogged);
        double activityRate = rate(stableLogs, DailySignalLog::isActivityLogged);
        double eveningRate = rate(stableLogs, DailySignalLog::isEveningCheckInCompleted);

        long apptDays = stableLogs.stream().filter(DailySignalLog::isAppointmentScheduled).count();
        long apptAttended = stableLogs.stream()
                .filter(s -> s.isAppointmentScheduled() && s.isAppointmentAttended()).count();
        double apptRate = apptDays > 0 ? (double) apptAttended / apptDays : 1.0;

        OptionalDouble avgSleep = stableLogs.stream()
                .filter(s -> s.getSleepStartHour() != null)
                .mapToInt(DailySignalLog::getSleepStartHour)
                .average();
        double avgSleepHour = avgSleep.orElse(23.0);

        log.info("Baseline calculated for {} over {} days: morning={}%, med={}%, activity={}%",
                user.getDisplayName(), n,
                Math.round(morningRate * 100), Math.round(medRate * 100), Math.round(activityRate * 100));

        return BaselineSnapshot.builder()
                .user(user)
                .morningCheckInRate(morningRate)
                .medicationAdherenceRate(medRate)
                .mealLoggingRate(mealRate)
                .activityRate(activityRate)
                .appointmentAttendanceRate(apptRate)
                .eveningCheckInRate(eveningRate)
                .averageSleepStartHour(avgSleepHour)
                .stableWindowDays(n)
                .active(true)
                .build();
    }

    private double rate(List<DailySignalLog> logs, java.util.function.Predicate<DailySignalLog> getter) {
        if (logs.isEmpty()) return 0.0;
        return (double) logs.stream().filter(getter).count() / logs.size();
    }

    private BaselineSnapshot emptyBaseline(RecoveryUser user) {
        return BaselineSnapshot.builder()
                .user(user)
                .morningCheckInRate(0.8)
                .medicationAdherenceRate(0.9)
                .mealLoggingRate(0.85)
                .activityRate(0.8)
                .appointmentAttendanceRate(1.0)
                .eveningCheckInRate(0.8)
                .averageSleepStartHour(23.0)
                .stableWindowDays(0)
                .active(true)
                .build();
    }
}
