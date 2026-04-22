package uk.ac.ed.inf.recoveryrhythm.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "baseline_snapshots")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BaselineSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private RecoveryUser user;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder.Default
    private double morningCheckInRate = 0.0;

    @Builder.Default
    private double medicationAdherenceRate = 0.0;

    @Builder.Default
    private double mealLoggingRate = 0.0;

    @Builder.Default
    private double activityRate = 0.0;

    @Builder.Default
    private double appointmentAttendanceRate = 0.0;

    @Builder.Default
    private double eveningCheckInRate = 0.0;

    @Builder.Default
    private double averageSleepStartHour = 23.0;

    @Builder.Default
    private int stableWindowDays = 0;

    @Column(columnDefinition = "TEXT")
    private String summaryJson;

    @Builder.Default
    private boolean active = true;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
