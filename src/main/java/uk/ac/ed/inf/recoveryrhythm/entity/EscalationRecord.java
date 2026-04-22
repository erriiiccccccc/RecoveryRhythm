package uk.ac.ed.inf.recoveryrhythm.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "escalation_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EscalationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private RecoveryUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "support_contact_id")
    private SupportContact supportContact;

    @Column(nullable = false)
    private LocalDateTime triggeredAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EscalationLevel level;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Builder.Default
    private String outcomeStatus = "PENDING";

    @PrePersist
    protected void onCreate() {
        if (triggeredAt == null) triggeredAt = LocalDateTime.now();
    }
}
