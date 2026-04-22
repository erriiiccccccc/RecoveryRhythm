package uk.ac.ed.inf.recoveryrhythm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.ac.ed.inf.recoveryrhythm.entity.InterventionRecord;
import uk.ac.ed.inf.recoveryrhythm.entity.InterventionType;
import uk.ac.ed.inf.recoveryrhythm.entity.RecoveryUser;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InterventionRecordRepository extends JpaRepository<InterventionRecord, UUID> {

    List<InterventionRecord> findTop10ByUserOrderByTriggeredAtDesc(RecoveryUser user);

    List<InterventionRecord> findByUserOrderByTriggeredAtDesc(RecoveryUser user);

    Optional<InterventionRecord> findTopByUserAndInterventionTypeOrderByTriggeredAtDesc(
            RecoveryUser user, InterventionType type);

    List<InterventionRecord> findByUserAndTriggeredAtAfter(RecoveryUser user, LocalDateTime since);
}
