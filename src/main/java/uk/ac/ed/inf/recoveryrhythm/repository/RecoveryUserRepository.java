package uk.ac.ed.inf.recoveryrhythm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.ac.ed.inf.recoveryrhythm.entity.RecoveryUser;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecoveryUserRepository extends JpaRepository<RecoveryUser, UUID> {
    Optional<RecoveryUser> findByLoginEmailIgnoreCase(String loginEmail);
    List<RecoveryUser> findByLoginEmailIsNotNullOrderByDisplayNameAsc();
}
