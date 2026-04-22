package uk.ac.ed.inf.recoveryrhythm.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "recovery_episodes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecoveryEpisode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private RecoveryUser user;

    @Column(nullable = false)
    private LocalDateTime openedAt;

    private LocalDateTime closedAt;

    @Builder.Default
    private int peakRiskScore = 0;

    @Column(columnDefinition = "TEXT")
    private String openingReason;

    @Column(columnDefinition = "TEXT")
    private String closingReason;

    @Enumerated(EnumType.STRING)
    private RecoveryState stateAtOpen;

    @Enumerated(EnumType.STRING)
    private RecoveryState stateAtClose;

    @Builder.Default
    private boolean active = true;

    @Column(columnDefinition = "TEXT")
    private String interventionsSummaryJson;

    @PrePersist
    protected void onCreate() {
        if (openedAt == null) openedAt = LocalDateTime.now();
    }
}
