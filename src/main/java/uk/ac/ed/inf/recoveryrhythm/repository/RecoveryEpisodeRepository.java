package uk.ac.ed.inf.recoveryrhythm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.ac.ed.inf.recoveryrhythm.entity.RecoveryEpisode;
import uk.ac.ed.inf.recoveryrhythm.entity.RecoveryUser;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecoveryEpisodeRepository extends JpaRepository<RecoveryEpisode, UUID> {

    Optional<RecoveryEpisode> findByUserAndActiveTrue(RecoveryUser user);

    List<RecoveryEpisode> findByUserOrderByOpenedAtDesc(RecoveryUser user);
}
