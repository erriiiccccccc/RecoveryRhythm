package uk.ac.ed.inf.recoveryrhythm.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class BaselineSnapshotResponse {
    private UUID id;
    private UUID userId;
    private LocalDateTime createdAt;
    private double morningCheckInRate;
    private double medicationAdherenceRate;
    private double mealLoggingRate;
    private double activityRate;
    private double appointmentAttendanceRate;
    private double eveningCheckInRate;
    private double averageSleepStartHour;
    private int stableWindowDays;
    private boolean active;
}
