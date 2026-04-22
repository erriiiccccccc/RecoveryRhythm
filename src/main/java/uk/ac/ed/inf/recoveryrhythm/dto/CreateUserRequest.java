package uk.ac.ed.inf.recoveryrhythm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateUserRequest {

    @NotBlank
    private String displayName;

    private String loginEmail;
    private String loginPassword;

    @NotNull
    private LocalDate recoveryStartDate;

    private String baselineIntakeNotes;
    private Integer typicalSleepStartHour;
    private Integer expectedActivityDaysPerWeek;
    private Integer expectedMedicationDosesPerDay;
    private String expectedMedicationSchedule;
    private Integer expectedMealsPerDay;
    private String expectedActivityType;
    private String expectedSleepTarget;
    private String baselineReferenceSource;
}
