package uk.ac.ed.inf.recoveryrhythm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.ac.ed.inf.recoveryrhythm.entity.SupportContact;
import uk.ac.ed.inf.recoveryrhythm.entity.RecoveryUser;

import java.util.List;
import java.util.UUID;

public interface SupportContactRepository extends JpaRepository<SupportContact, UUID> {
    List<SupportContact> findByUser(RecoveryUser user);
    List<SupportContact> findByUserAndEscalationEnabledTrue(RecoveryUser user);
}
