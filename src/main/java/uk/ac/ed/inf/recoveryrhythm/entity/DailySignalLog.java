package uk.ac.ed.inf.recoveryrhythm.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "daily_signal_logs",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "log_date"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailySignalLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private RecoveryUser user;

    @Column(name = "log_date", nullable = false)
    private LocalDate logDate;

    @Builder.Default
    private boolean morningCheckInCompleted = false;

    @Builder.Default
    private boolean medicationTaken = false;

    @Builder.Default
    private boolean mealLogged = false;

    @Builder.Default
    private boolean activityLogged = false;

    @Builder.Default
    private boolean appointmentScheduled = false;

    @Builder.Default
    private boolean appointmentAttended = false;

    @Builder.Default
    private boolean eveningCheckInCompleted = false;

    private Integer sleepStartHour;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Builder.Default
    private boolean manuallyEntered = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @Builder.Default
    private DailyVerificationState verificationState = DailyVerificationState.PENDING;

    @Column(nullable = false, updatable = false)
    private LocalDateTime loggedAt;

    @PrePersist
    protected void onCreate() {
        loggedAt = LocalDateTime.now();
    }
}
