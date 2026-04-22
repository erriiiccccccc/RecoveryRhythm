package uk.ac.ed.inf.recoveryrhythm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.ac.ed.inf.recoveryrhythm.entity.EscalationRecord;
import uk.ac.ed.inf.recoveryrhythm.entity.RecoveryUser;

import java.util.List;
import java.util.UUID;

public interface EscalationRecordRepository extends JpaRepository<EscalationRecord, UUID> {

    List<EscalationRecord> findByUserOrderByTriggeredAtDesc(RecoveryUser user);

    List<EscalationRecord> findTop5ByUserOrderByTriggeredAtDesc(RecoveryUser user);
}
