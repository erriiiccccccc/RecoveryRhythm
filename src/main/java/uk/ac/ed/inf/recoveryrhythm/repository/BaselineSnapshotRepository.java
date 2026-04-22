package uk.ac.ed.inf.recoveryrhythm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import uk.ac.ed.inf.recoveryrhythm.entity.BaselineSnapshot;
import uk.ac.ed.inf.recoveryrhythm.entity.RecoveryUser;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BaselineSnapshotRepository extends JpaRepository<BaselineSnapshot, UUID> {

    Optional<BaselineSnapshot> findByUserAndActiveTrue(RecoveryUser user);

    List<BaselineSnapshot> findByUserOrderByCreatedAtDesc(RecoveryUser user);

    @Modifying
    @Query("UPDATE BaselineSnapshot b SET b.active = false WHERE b.user = :user AND b.active = true")
    void deactivateAllForUser(RecoveryUser user);
}
