package kz.aitu.hrms.integration.repository;

import kz.aitu.hrms.integration.domain.SyncJob;
import kz.aitu.hrms.integration.domain.SyncStatus;
import kz.aitu.hrms.integration.domain.SyncTarget;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface SyncJobRepository extends JpaRepository<SyncJob, UUID> {

    boolean existsByPeriodIdAndTargetAndStatusInAndDeletedFalse(
            UUID periodId, SyncTarget target, List<SyncStatus> statuses);

    @Query("SELECT j FROM SyncJob j WHERE j.status = 'RETRYING' AND j.nextRetryAt <= :now AND j.deleted = false")
    List<SyncJob> findDueRetries(@Param("now") LocalDateTime now);

    @Query("SELECT j FROM SyncJob j WHERE j.deleted = false" +
           " AND (:target IS NULL OR j.target = :target)" +
           " AND (:status IS NULL OR j.status = :status)")
    Page<SyncJob> findFiltered(
            @Param("target") SyncTarget target,
            @Param("status") SyncStatus status,
            Pageable pageable);
}
