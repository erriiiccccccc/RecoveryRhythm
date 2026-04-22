package uk.ac.ed.inf.recoveryrhythm.dto;

import lombok.Builder;
import lombok.Data;
import uk.ac.ed.inf.recoveryrhythm.entity.DailyVerificationState;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class DailySignalResponse {
    private UUID id;
    private UUID userId;
    private LocalDate logDate;
    private boolean morningCheckInCompleted;
    private boolean medicationTaken;
    private boolean mealLogged;
    private boolean activityLogged;
    private boolean appointmentScheduled;
    private boolean appointmentAttended;
    private boolean eveningCheckInCompleted;
    private Integer sleepStartHour;
    private String notes;
    private LocalDateTime loggedAt;
    private int signalsCompletedCount;
    private int signalsTotalCount;
    private DailyVerificationState verificationState;
    private int pendingEvidenceCount;
    private int deniedEvidenceCount;
}
