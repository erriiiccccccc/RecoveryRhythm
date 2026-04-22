package uk.ac.ed.inf.recoveryrhythm.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "recovery_users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecoveryUser {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String displayName;

    @Column(length = 120, unique = true)
    private String loginEmail;

    @Column(length = 120)
    private String loginPassword;

    @Column(nullable = false)
    private LocalDate recoveryStartDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RecoveryState currentState = RecoveryState.STABLE;

    @Builder.Default
    private int currentRiskScore = 0;

    @Builder.Default
    private boolean reentryModeActive = false;

    @Column(columnDefinition = "TEXT")
    private String baselineIntakeNotes;

    private Integer typicalSleepStartHour;

    private Integer expectedActivityDaysPerWeek;

    private Integer expectedMedicationDosesPerDay;

    @Column(length = 255)
    private String expectedMedicationSchedule;

    private Integer expectedMealsPerDay;

    @Column(length = 120)
    private String expectedActivityType;

    @Column(length = 120)
    private String expectedSleepTarget;

    @Column(length = 120)
    private String baselineReferenceSource;

    @Column(length = 255)
    private String profilePhotoObjectKey;

    @Column(length = 100)
    private String profilePhotoMimeType;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
