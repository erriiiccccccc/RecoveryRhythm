package uk.ac.ed.inf.recoveryrhythm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.ac.ed.inf.recoveryrhythm.entity.*;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface SignalEvidenceRepository extends JpaRepository<SignalEvidence, UUID> {

    List<SignalEvidence> findByDailySignalLogOrderByUploadedAtDesc(DailySignalLog dailySignalLog);

    List<SignalEvidence> findByDailySignalLogInOrderByUploadedAtDesc(Collection<DailySignalLog> logs);

    List<SignalEvidence> findTop100ByStatusOrderByUploadedAtDesc(VerificationStatus status);

    long countByUserAndStatusAndDailySignalLog_LogDateBetween(
            RecoveryUser user,
            VerificationStatus status,
            LocalDate from,
            LocalDate to
    );

    boolean existsByDailySignalLogAndSignalTypeAndStatus(
            DailySignalLog dailySignalLog,
            EvidenceSignalType signalType,
            VerificationStatus status
    );
}
