package uk.ac.ed.inf.recoveryrhythm.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "risk_assessments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private RecoveryUser user;

    @Column(nullable = false)
    private LocalDateTime assessedAt;

    @Builder.Default
    private int riskScore = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RecoveryState state = RecoveryState.STABLE;

    @Column(columnDefinition = "TEXT")
    private String conciseSummary;

    @Column(columnDefinition = "TEXT")
    private String detailedExplanation;

    @Column(columnDefinition = "TEXT")
    private String factorBreakdownJson;

    @PrePersist
    protected void onCreate() {
        if (assessedAt == null) assessedAt = LocalDateTime.now();
    }
}
