package kz.aitu.hrms.leave.repository;

import kz.aitu.hrms.leave.entity.BalanceAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BalanceAdjustmentRepository extends JpaRepository<BalanceAdjustment, UUID> {
}