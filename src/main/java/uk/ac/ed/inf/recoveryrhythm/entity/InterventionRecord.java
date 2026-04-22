package uk.ac.ed.inf.recoveryrhythm.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "intervention_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterventionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private RecoveryUser user;

    @Column(nullable = false)
    private LocalDateTime triggeredAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterventionType interventionType;

    @Column(columnDefinition = "TEXT")
    private String message;

    private UUID relatedEpisodeId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private InterventionStatus status = InterventionStatus.QUEUED;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @PrePersist
    protected void onCreate() {
        if (triggeredAt == null) triggeredAt = LocalDateTime.now();
    }
}
