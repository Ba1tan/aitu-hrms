package kz.aitu.hrms.attendance.repository;

import kz.aitu.hrms.attendance.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, UUID> {

    Optional<Holiday> findByIdAndDeletedFalse(UUID id);

    Optional<Holiday> findByHolidayDateAndDeletedFalse(LocalDate date);

    boolean existsByHolidayDateAndDeletedFalse(LocalDate date);

    @Query("""
        SELECT h FROM Holiday h
        WHERE h.deleted = false
          AND FUNCTION('YEAR', h.holidayDate) =
              COALESCE(:year, FUNCTION('YEAR', h.holidayDate))
        ORDER BY h.holidayDate ASC
        """)
    List<Holiday> findByYear(@Param("year") Integer year);

    @Query("""
        SELECT h FROM Holiday h
        WHERE h.deleted = false
          AND h.holidayDate BETWEEN :from AND :to
        ORDER BY h.holidayDate ASC
        """)
    List<Holiday> findBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);
}