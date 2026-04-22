package uk.ac.ed.inf.recoveryrhythm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.ac.ed.inf.recoveryrhythm.entity.RiskAssessment;
import uk.ac.ed.inf.recoveryrhythm.entity.RecoveryUser;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RiskAssessmentRepository extends JpaRepository<RiskAssessment, UUID> {

    Optional<RiskAssessment> findTopByUserOrderByAssessedAtDesc(RecoveryUser user);

    List<RiskAssessment> findTop10ByUserOrderByAssessedAtDesc(RecoveryUser user);

    List<RiskAssessment> findByUserOrderByAssessedAtDesc(RecoveryUser user);
}
