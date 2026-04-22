package uk.ac.ed.inf.recoveryrhythm.dto;

import lombok.Builder;
import lombok.Data;
import uk.ac.ed.inf.recoveryrhythm.entity.RecoveryState;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class UserResponse {
    private UUID id;
    private String displayName;
    private String loginEmail;
    private LocalDate recoveryStartDate;
    private RecoveryState currentState;
    private int currentRiskScore;
    private boolean reentryModeActive;
    private String baselineIntakeNotes;
    private Integer typicalSleepStartHour;
    private Integer expectedActivityDaysPerWeek;
    private Integer expectedMedicationDosesPerDay;
    private String expectedMedicationSchedule;
    private Integer expectedMealsPerDay;
    private String expectedActivityType;
    private String expectedSleepTarget;
    private String baselineReferenceSource;
    private String profilePhotoMimeType;
    private String profilePhotoBase64;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
