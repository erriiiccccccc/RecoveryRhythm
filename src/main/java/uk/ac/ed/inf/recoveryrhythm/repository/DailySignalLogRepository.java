package uk.ac.ed.inf.recoveryrhythm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.ac.ed.inf.recoveryrhythm.entity.DailySignalLog;
import uk.ac.ed.inf.recoveryrhythm.entity.RecoveryUser;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DailySignalLogRepository extends JpaRepository<DailySignalLog, UUID> {

    List<DailySignalLog> findByUserOrderByLogDateDesc(RecoveryUser user);

    List<DailySignalLog> findByUserAndLogDateBetweenOrderByLogDateAsc(
            RecoveryUser user, LocalDate from, LocalDate to);

    Optional<DailySignalLog> findByUserAndLogDate(RecoveryUser user, LocalDate date);

    List<DailySignalLog> findTop7ByUserOrderByLogDateDesc(RecoveryUser user);

    List<DailySignalLog> findTop14ByUserOrderByLogDateDesc(RecoveryUser user);

    long countByUser(RecoveryUser user);
}
