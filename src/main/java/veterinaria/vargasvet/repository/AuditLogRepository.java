package veterinaria.vargasvet.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.AuditLog;
import java.time.LocalDateTime;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("SELECT a FROM AuditLog a WHERE " +
            "(cast(:companyId as integer) IS NULL OR a.companyId = :companyId) AND " +
            "(cast(:userEmail as string) IS NULL OR LOWER(CAST(a.userEmail AS string)) LIKE LOWER(CAST(CONCAT('%', cast(:userEmail as string), '%') AS string))) AND " +
            "(cast(:action as string) IS NULL OR a.action = :action) AND " +
            "(cast(:module as string) IS NULL OR a.module = :module) AND " +
            "(cast(:startDate as localdatetime) IS NULL OR a.timestamp >= :startDate) AND " +
            "(cast(:endDate as localdatetime) IS NULL OR a.timestamp <= :endDate)")
    Page<AuditLog> filterLogs(
            @Param("companyId") Integer companyId,
            @Param("userEmail") String userEmail,
            @Param("action") String action,
            @Param("module") String module,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    boolean existsByUserEmailAndActionAndTimestampAfter(String userEmail, String action, LocalDateTime desde);

    @Modifying(flushAutomatically = true)
    @Query("UPDATE AuditLog a SET a.userEmail = :nuevo "
            + "WHERE a.companyId = :companyId AND LOWER(a.userEmail) = LOWER(:viejo)")
    int pseudonimizarCorreo(@Param("companyId") Integer companyId, @Param("viejo") String viejo,
                            @Param("nuevo") String nuevo);

    @Modifying(flushAutomatically = true)
    @Query("UPDATE AuditLog a SET a.details = REPLACE(a.details, :viejo, :nuevo) "
            + "WHERE a.companyId = :companyId AND a.details LIKE CONCAT('%', :viejo, '%')")
    int reemplazarEnDetalle(@Param("companyId") Integer companyId, @Param("viejo") String viejo,
                            @Param("nuevo") String nuevo);
}
