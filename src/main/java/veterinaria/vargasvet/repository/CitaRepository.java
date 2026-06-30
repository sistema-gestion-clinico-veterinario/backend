package veterinaria.vargasvet.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.Cita;
import veterinaria.vargasvet.domain.enums.EstadoCita;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Repository
public interface CitaRepository extends JpaRepository<Cita, Long> {

    @Query("SELECT COUNT(c) > 0 FROM Cita c WHERE c.empleado.id = :veterinarioId " +
           "AND c.estado NOT IN ('CANCELADA', 'ELIMINADA') AND c.eliminada = false " +
           "AND c.fechaHoraInicio < :fechaHoraFin " +
           "AND c.fechaHoraFin > :fechaHoraInicio")
    boolean existsOverlappingCita(@Param("veterinarioId") Long veterinarioId,
                                  @Param("fechaHoraInicio") LocalDateTime fechaHoraInicio,
                                  @Param("fechaHoraFin") LocalDateTime fechaHoraFin);

    @Query("SELECT COUNT(c) > 0 FROM Cita c WHERE c.empleado.id = :veterinarioId " +
           "AND c.id != :citaId " +
           "AND c.estado NOT IN ('CANCELADA', 'ELIMINADA') AND c.eliminada = false " +
           "AND c.fechaHoraInicio < :fechaHoraFin " +
           "AND c.fechaHoraFin > :fechaHoraInicio")
    boolean existsOverlappingCitaExcludeSelf(@Param("veterinarioId") Long veterinarioId,
                                              @Param("fechaHoraInicio") LocalDateTime fechaHoraInicio,
                                              @Param("fechaHoraFin") LocalDateTime fechaHoraFin,
                                              @Param("citaId") Long citaId);

    @Query("SELECT COUNT(c) > 0 FROM Cita c WHERE c.mascota.id = :mascotaId " +
           "AND c.estado NOT IN ('CANCELADA', 'ELIMINADA') AND c.eliminada = false " +
           "AND c.fechaHoraInicio < :fechaHoraFin " +
           "AND c.fechaHoraFin > :fechaHoraInicio")
    boolean existsOverlappingCitaMascota(@Param("mascotaId") Long mascotaId,
                                         @Param("fechaHoraInicio") LocalDateTime fechaHoraInicio,
                                         @Param("fechaHoraFin") LocalDateTime fechaHoraFin);

    @Query("SELECT COUNT(c) > 0 FROM Cita c WHERE c.mascota.id = :mascotaId " +
           "AND c.id != :citaId " +
           "AND c.estado NOT IN ('CANCELADA', 'ELIMINADA') AND c.eliminada = false " +
           "AND c.fechaHoraInicio < :fechaHoraFin " +
           "AND c.fechaHoraFin > :fechaHoraInicio")
    boolean existsOverlappingCitaMascotaExcludeSelf(@Param("mascotaId") Long mascotaId,
                                                     @Param("fechaHoraInicio") LocalDateTime fechaHoraInicio,
                                                     @Param("fechaHoraFin") LocalDateTime fechaHoraFin,
                                                     @Param("citaId") Long citaId);

    @Query(value = "SELECT c FROM Cita c JOIN FETCH c.mascota m JOIN FETCH m.apoderado a JOIN FETCH a.user u " +
                   "LEFT JOIN FETCH c.consulta " +
                    "WHERE u.company.id = :companyId AND c.eliminada = false " +
                    "AND (CAST(:fecha AS date) IS NULL OR CAST(c.fechaHoraInicio AS date) = :fecha) " +
                    "AND (CAST(:estado AS text) IS NULL OR c.estado = :estado) " +
                    "AND (:veterinarioId IS NULL OR c.empleado.id = :veterinarioId) " +
                    "ORDER BY c.fechaHoraInicio DESC",
            countQuery = "SELECT COUNT(c) FROM Cita c JOIN c.mascota m JOIN m.apoderado a JOIN a.user u " +
                         "WHERE u.company.id = :companyId AND c.eliminada = false " +
                        "AND (CAST(:fecha AS date) IS NULL OR CAST(c.fechaHoraInicio AS date) = :fecha) " +
                        "AND (CAST(:estado AS text) IS NULL OR c.estado = :estado) " +
                        "AND (:veterinarioId IS NULL OR c.empleado.id = :veterinarioId)")
    Page<Cita> buscar(@Param("companyId") Integer companyId,
                      @Param("fecha") LocalDate fecha,
                      @Param("estado") EstadoCita estado,
                      @Param("veterinarioId") Long veterinarioId,
                      Pageable pageable);

    @Query("SELECT COUNT(c) FROM Cita c WHERE c.mascota.apoderado.user.company.id = :companyId AND c.eliminada = false")
    long countByCompanyId(@Param("companyId") Integer companyId);

    boolean existsByEmpleadoId(Long empleadoId);

    @Query("SELECT COUNT(c) FROM Cita c WHERE c.mascota.apoderado.user.company.id = :companyId " +
           "AND c.eliminada = false AND CAST(c.fechaHoraInicio AS date) = CURRENT_DATE")
    long countTodayByCompanyId(@Param("companyId") Integer companyId);
    @Query("SELECT c FROM Cita c WHERE c.empleado.id = :empleadoId " +
           "AND c.estado NOT IN ('CANCELADA', 'ELIMINADA') AND c.eliminada = false " +
           "AND CAST(c.fechaHoraInicio AS date) BETWEEN :startDate AND :endDate")
    java.util.List<Cita> findByEmpleadoIdAndDateRange(@Param("empleadoId") Long empleadoId, 
                                                       @Param("startDate") LocalDate startDate, 
                                                       @Param("endDate") LocalDate endDate);

    @Query("SELECT c FROM Cita c WHERE c.mascota.apoderado.id = :apoderadoId AND c.eliminada = false ORDER BY c.fechaHoraInicio DESC")
    java.util.List<Cita> findByApoderadoId(@Param("apoderadoId") Long apoderadoId);

    @Query("SELECT c FROM Cita c WHERE c.mascota.apoderado.id = :apoderadoId AND c.mascota.id = :mascotaId AND c.eliminada = false ORDER BY c.fechaHoraInicio DESC")
    java.util.List<Cita> findByApoderadoIdAndMascotaId(@Param("apoderadoId") Long apoderadoId, @Param("mascotaId") Long mascotaId);

    @Query(value = "SELECT c FROM Cita c WHERE c.mascota.apoderado.id = :apoderadoId AND c.eliminada = false ORDER BY c.fechaHoraInicio DESC",
           countQuery = "SELECT COUNT(c) FROM Cita c WHERE c.mascota.apoderado.id = :apoderadoId AND c.eliminada = false")
    Page<Cita> findByApoderadoIdPaginated(@Param("apoderadoId") Long apoderadoId, Pageable pageable);

    @Query(value = "SELECT c FROM Cita c WHERE c.mascota.apoderado.user.company.id = :companyId AND c.eliminada = false ORDER BY c.fechaHoraInicio DESC",
           countQuery = "SELECT COUNT(c) FROM Cita c WHERE c.mascota.apoderado.user.company.id = :companyId AND c.eliminada = false")
    Page<Cita> findByCompanyIdPaginated(@Param("companyId") Integer companyId, Pageable pageable);

    @Query(value = "SELECT c FROM Cita c WHERE c.mascota.apoderado.id = :apoderadoId AND c.mascota.id = :mascotaId AND c.eliminada = false ORDER BY c.fechaHoraInicio DESC",
           countQuery = "SELECT COUNT(c) FROM Cita c WHERE c.mascota.apoderado.id = :apoderadoId AND c.mascota.id = :mascotaId AND c.eliminada = false")
    Page<Cita> findByApoderadoIdAndMascotaIdPaginated(@Param("apoderadoId") Long apoderadoId, @Param("mascotaId") Long mascotaId, Pageable pageable);

    @Query("SELECT c FROM Cita c WHERE c.empleado.id = :empleadoId AND c.estado NOT IN ('CANCELADA', 'ELIMINADA') AND c.eliminada = false AND CAST(c.fechaHoraInicio AS date) = :fecha")
    java.util.List<Cita> findActiveByEmpleadoIdAndFecha(@Param("empleadoId") Long empleadoId, @Param("fecha") LocalDate fecha);

    @Query("SELECT c FROM Cita c WHERE c.empleado.id = :empleadoId AND c.estado NOT IN ('CANCELADA', 'ELIMINADA') AND c.eliminada = false AND CAST(c.fechaHoraInicio AS date) = CAST(:fecha AS date)")
    java.util.List<Cita> findActiveByEmpleadoIdAndFechaString(@Param("empleadoId") Long empleadoId, @Param("fecha") String fecha);

    @Query("SELECT c FROM Cita c WHERE c.mascota.apoderado.id = :apoderadoId AND c.estado NOT IN ('CANCELADA', 'ELIMINADA') AND c.eliminada = false AND CAST(c.fechaHoraInicio AS date) = :fecha")
    java.util.List<Cita> findActiveByApoderadoIdAndFecha(@Param("apoderadoId") Long apoderadoId, @Param("fecha") LocalDate fecha);

    @Query("SELECT COUNT(c) FROM Cita c WHERE c.mascota.apoderado.user.company.id = :companyId " +
           "AND c.eliminada = false AND c.estado NOT IN ('CANCELADA', 'ELIMINADA') " +
           "AND c.fechaHoraInicio BETWEEN :start AND :end")
    long countByCompanyAndDateRange(@Param("companyId") Integer companyId, 
                                    @Param("start") LocalDateTime start, 
                                    @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(c) FROM Cita c WHERE c.eliminada = false AND c.estado NOT IN ('CANCELADA', 'ELIMINADA') " +
           "AND c.fechaHoraInicio BETWEEN :start AND :end")
    long countGlobalByDateRange(@Param("start") LocalDateTime start,
                                @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(c) FROM Cita c WHERE c.empleado.id = :empleadoId AND c.estado = 'EN_PROCESO' AND c.eliminada = false AND c.id <> :excludeId")
    long countEnProcesoByEmpleadoExcluding(@Param("empleadoId") Long empleadoId, @Param("excludeId") Long excludeId);

    @Query("SELECT COUNT(c) > 0 FROM Cita c WHERE c.mascota.id = :mascotaId " +
           "AND c.eliminada = false " +
           "AND c.fechaHoraInicio >= :ahora " +
           "AND c.estado IN ('PROGRAMADA', 'PENDIENTE', 'CONFIRMADA', 'REPROGRAMADA', 'SALA_DE_ESPERA', 'EN_PROCESO')")
    boolean existsCitaVigenteByMascotaId(@Param("mascotaId") Long mascotaId,
                                         @Param("ahora") LocalDateTime ahora);

    @Query("SELECT COUNT(c) > 0 FROM Cita c WHERE c.empleado.id = :empleadoId " +
           "AND c.eliminada = false " +
           "AND c.fechaHoraInicio >= :ahora " +
           "AND c.estado IN ('PROGRAMADA', 'PENDIENTE', 'CONFIRMADA', 'REPROGRAMADA', 'SALA_DE_ESPERA', 'EN_PROCESO')")
    boolean existsCitaVigenteByEmpleadoId(@Param("empleadoId") Long empleadoId,
                                          @Param("ahora") LocalDateTime ahora);

    @Query("SELECT c FROM Cita c JOIN c.empleado e JOIN e.tiposEmpleado t " +
           "WHERE c.mascota.id = :mascotaId AND c.eliminada = false AND UPPER(t.nombre) = 'GROMMER' " +
           "ORDER BY c.fechaHoraInicio DESC")
    java.util.List<Cita> findServiciosNoMedicosParaMascota(@Param("mascotaId") Long mascotaId);

    boolean existsByServicioId(@Param("servicioId") Long servicioId);
}
