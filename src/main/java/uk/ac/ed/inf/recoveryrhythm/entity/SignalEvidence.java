package uk.ac.ed.inf.recoveryrhythm.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "signal_evidence")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignalEvidence {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private RecoveryUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_signal_log_id", nullable = false)
    private DailySignalLog dailySignalLog;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private EvidenceSignalType signalType;

    @Column(nullable = false, length = 100)
    private String mimeType;

    @Lob
    @Column(nullable = false, columnDefinition = "bytea")
    private byte[] imageData;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @Builder.Default
    private VerificationStatus status = VerificationStatus.PENDING;

    @Column(length = 120)
    private String clinicianName;

    @Column(columnDefinition = "TEXT")
    private String verificationReason;

    @Column(nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    private LocalDateTime verifiedAt;

    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
    }
}
