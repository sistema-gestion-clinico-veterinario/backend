package veterinaria.vargasvet.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.Cita;
import veterinaria.vargasvet.domain.enums.EspecieMascota;
import veterinaria.vargasvet.domain.enums.EstadoCita;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CitaRepository extends JpaRepository<Cita, Long> {

    String ESTADOS_NO_ACTIVOS = "c.estado NOT IN ('CANCELADA', 'ELIMINADA') AND c.eliminada = false";
    String ESTADOS_VIGENTES = "c.estado IN ('PROGRAMADA', 'PENDIENTE', 'CONFIRMADA', 'REPROGRAMADA', 'SALA_DE_ESPERA', 'EN_PROCESO')";

    @Query("SELECT DISTINCT c FROM Cita c " +
            "JOIN FETCH c.mascota m " +
            "JOIN FETCH m.apoderado a " +
            "JOIN FETCH a.user u " +
            "JOIN FETCH c.empleado e " +
            "LEFT JOIN FETCH e.user eu " +
            "LEFT JOIN FETCH c.servicio s " +
            "LEFT JOIN FETCH c.consulta co " +
            "WHERE u.company.id = :companyId " +
            "AND c.eliminada = false " +
            "AND c.fechaHoraInicio >= :fechaInicio " +
            "AND c.fechaHoraInicio < :fechaFin " +
            "AND (CAST(:veterinarioId AS string) IS NULL OR e.id = :veterinarioId) " +
            "AND (CAST(:especie AS string) IS NULL OR m.especie = :especie) " +
            "ORDER BY c.fechaHoraInicio")
    List<Cita> findForClinicalReport(@Param("companyId") Integer companyId,
                                     @Param("fechaInicio") LocalDateTime fechaInicio,
                                     @Param("fechaFin") LocalDateTime fechaFin,
                                     @Param("veterinarioId") Long veterinarioId,
                                     @Param("especie") EspecieMascota especie);

    interface EstadoCount {
        EstadoCita getEstado();
        Long getTotal();
    }

    @Query("SELECT c.estado AS estado, COUNT(c) AS total FROM Cita c " +
            "JOIN c.mascota m JOIN m.apoderado a JOIN a.user u " +
            "WHERE u.company.id = :companyId AND c.eliminada = false " +
            "AND (CAST(:fechaInicio AS string) IS NULL OR c.fechaHoraInicio >= :fechaInicio) " +
            "AND (CAST(:fechaFin AS string) IS NULL OR c.fechaHoraInicio < :fechaFin) " +
            "AND (CAST(:veterinarioId AS string) IS NULL OR c.empleado.id = :veterinarioId) " +
            "GROUP BY c.estado")
    List<EstadoCount> contarPorEstado(@Param("companyId") Integer companyId,
                                      @Param("fechaInicio") LocalDateTime fechaInicio,
                                      @Param("fechaFin") LocalDateTime fechaFin,
                                      @Param("veterinarioId") Long veterinarioId);

    interface ServicioCount {
        String getNombre();
        Long getTotal();
    }

    @Query("SELECT s.nombre AS nombre, COUNT(c) AS total FROM Cita c " +
            "JOIN c.servicio s " +
            "JOIN c.mascota m JOIN m.apoderado a JOIN a.user u " +
            "WHERE u.company.id = :companyId AND " + ESTADOS_NO_ACTIVOS + " " +
            "GROUP BY s.nombre")
    List<ServicioCount> countByServicio(@Param("companyId") Integer companyId);

    @Query("SELECT COUNT(c) > 0 FROM Cita c WHERE c.empleado.id = :veterinarioId " +
            "AND (CAST(:citaId AS string) IS NULL OR c.id <> :citaId) " +
            "AND " + ESTADOS_NO_ACTIVOS + " " +
            "AND c.fechaHoraInicio < :fechaHoraFin " +
            "AND c.fechaHoraFin > :fechaHoraInicio")
    boolean existsOverlappingCitaVeterinario(@Param("veterinarioId") Long veterinarioId,
                                             @Param("fechaHoraInicio") LocalDateTime fechaHoraInicio,
                                             @Param("fechaHoraFin") LocalDateTime fechaHoraFin,
                                             @Param("citaId") Long citaId);

    default boolean existsOverlappingCita(Long veterinarioId,
                                          LocalDateTime fechaHoraInicio,
                                          LocalDateTime fechaHoraFin) {
        return existsOverlappingCitaVeterinario(veterinarioId, fechaHoraInicio, fechaHoraFin, null);
    }

    default boolean existsOverlappingCitaExcludeSelf(Long veterinarioId,
                                                     LocalDateTime fechaHoraInicio,
                                                     LocalDateTime fechaHoraFin,
                                                     Long citaId) {
        return existsOverlappingCitaVeterinario(veterinarioId, fechaHoraInicio, fechaHoraFin, citaId);
    }

    @Query("SELECT COUNT(c) > 0 FROM Cita c WHERE c.mascota.id = :mascotaId " +
            "AND (CAST(:citaId AS string) IS NULL OR c.id <> :citaId) " +
            "AND " + ESTADOS_NO_ACTIVOS + " " +
            "AND c.fechaHoraInicio < :fechaHoraFin " +
            "AND c.fechaHoraFin > :fechaHoraInicio")
    boolean existsOverlappingCitaMascota(@Param("mascotaId") Long mascotaId,
                                         @Param("fechaHoraInicio") LocalDateTime fechaHoraInicio,
                                         @Param("fechaHoraFin") LocalDateTime fechaHoraFin,
                                         @Param("citaId") Long citaId);

    default boolean existsOverlappingCitaMascota(Long mascotaId,
                                                 LocalDateTime fechaHoraInicio,
                                                 LocalDateTime fechaHoraFin) {
        return existsOverlappingCitaMascota(mascotaId, fechaHoraInicio, fechaHoraFin, null);
    }

    default boolean existsOverlappingCitaMascotaExcludeSelf(Long mascotaId,
                                                            LocalDateTime fechaHoraInicio,
                                                            LocalDateTime fechaHoraFin,
                                                            Long citaId) {
        return existsOverlappingCitaMascota(mascotaId, fechaHoraInicio, fechaHoraFin, citaId);
    }

    @Query(value = "SELECT c FROM Cita c JOIN FETCH c.mascota m JOIN FETCH m.apoderado a JOIN FETCH a.user u " +
            "LEFT JOIN FETCH c.consulta " +
            "WHERE u.company.id = :companyId AND c.eliminada = false " +
            "AND (CAST(:fechaInicio AS string) IS NULL OR c.fechaHoraInicio >= :fechaInicio) " +
            "AND (CAST(:fechaFin AS string) IS NULL OR c.fechaHoraInicio < :fechaFin) " +
            "AND (CAST(:estado AS string) IS NULL OR c.estado = :estado) " +
            "AND (CAST(:veterinarioId AS string) IS NULL OR c.empleado.id = :veterinarioId)",
            countQuery = "SELECT COUNT(c) FROM Cita c JOIN c.mascota m JOIN m.apoderado a JOIN a.user u " +
                    "WHERE u.company.id = :companyId AND c.eliminada = false " +
                    "AND (CAST(:fechaInicio AS string) IS NULL OR c.fechaHoraInicio >= :fechaInicio) " +
                    "AND (CAST(:fechaFin AS string) IS NULL OR c.fechaHoraInicio < :fechaFin) " +
                    "AND (CAST(:estado AS string) IS NULL OR c.estado = :estado) " +
                    "AND (CAST(:veterinarioId AS string) IS NULL OR c.empleado.id = :veterinarioId)")
    Page<Cita> buscar(@Param("companyId") Integer companyId,
                      @Param("fechaInicio") LocalDateTime fechaInicio,
                      @Param("fechaFin") LocalDateTime fechaFin,
                      @Param("estado") EstadoCita estado,
                      @Param("veterinarioId") Long veterinarioId,
                      Pageable pageable);
    @Query("SELECT COUNT(c) FROM Cita c WHERE c.mascota.apoderado.user.company.id = :companyId AND c.eliminada = false")
    long countByCompanyId(@Param("companyId") Integer companyId);

    @Query("SELECT COUNT(c) FROM Cita c WHERE c.mascota.apoderado.user.company.id = :companyId " +
            "AND c.eliminada = false AND CAST(c.fechaHoraInicio AS date) = CURRENT_DATE")
    long countTodayByCompanyId(@Param("companyId") Integer companyId);

    @Query("SELECT COUNT(c) FROM Cita c WHERE c.mascota.apoderado.user.company.id = :companyId " +
            "AND " + ESTADOS_NO_ACTIVOS + " " +
            "AND c.fechaHoraInicio BETWEEN :start AND :end")
    long countByCompanyAndDateRange(@Param("companyId") Integer companyId,
                                    @Param("start") LocalDateTime start,
                                    @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(c) FROM Cita c WHERE " + ESTADOS_NO_ACTIVOS + " " +
            "AND c.fechaHoraInicio BETWEEN :start AND :end")
    long countGlobalByDateRange(@Param("start") LocalDateTime start,
                                @Param("end") LocalDateTime end);

    Page<Cita> findByMascota_Apoderado_User_Company_IdAndEliminadaFalseOrderByFechaHoraInicioDesc(
            Integer companyId, Pageable pageable);

    boolean existsByEmpleadoId(Long empleadoId);

    boolean existsByServicioId(Long servicioId);

    @Query("SELECT c FROM Cita c WHERE c.empleado.id = :empleadoId " +
            "AND " + ESTADOS_NO_ACTIVOS + " " +
            "AND CAST(c.fechaHoraInicio AS date) BETWEEN :startDate AND :endDate")
    List<Cita> findByEmpleadoIdAndDateRange(@Param("empleadoId") Long empleadoId,
                                            @Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate);

    @Query("SELECT c FROM Cita c WHERE c.empleado.id = :empleadoId " +
            "AND " + ESTADOS_NO_ACTIVOS + " " +
            "AND CAST(c.fechaHoraInicio AS date) = :fecha")
    List<Cita> findActiveByEmpleadoIdAndFecha(@Param("empleadoId") Long empleadoId,
                                              @Param("fecha") LocalDate fecha);

    @Query("SELECT COUNT(c) FROM Cita c WHERE c.empleado.id = :empleadoId " +
            "AND c.estado = 'EN_PROCESO' AND c.eliminada = false AND c.id <> :excludeId")
    long countEnProcesoByEmpleadoExcluding(@Param("empleadoId") Long empleadoId, @Param("excludeId") Long excludeId);

    @Query("SELECT COUNT(c) > 0 FROM Cita c WHERE c.empleado.id = :empleadoId " +
            "AND c.eliminada = false AND c.fechaHoraInicio >= :ahora AND " + ESTADOS_VIGENTES)
    boolean existsCitaVigenteByEmpleadoId(@Param("empleadoId") Long empleadoId,
                                          @Param("ahora") LocalDateTime ahora);

    List<Cita> findByMascota_Apoderado_IdAndEliminadaFalseOrderByFechaHoraInicioDesc(Long apoderadoId);

    List<Cita> findByMascota_Apoderado_IdAndMascota_IdAndEliminadaFalseOrderByFechaHoraInicioDesc(
            Long apoderadoId, Long mascotaId);

    Page<Cita> findByMascota_Apoderado_IdAndEliminadaFalseOrderByFechaHoraInicioDesc(
            Long apoderadoId, Pageable pageable);

    Page<Cita> findByMascota_Apoderado_IdAndMascota_IdAndEliminadaFalseOrderByFechaHoraInicioDesc(
            Long apoderadoId, Long mascotaId, Pageable pageable);

    default Page<Cita> findByApoderadoIdPaginated(Long apoderadoId, Pageable pageable) {
        return findByMascota_Apoderado_IdAndEliminadaFalseOrderByFechaHoraInicioDesc(apoderadoId, pageable);
    }

    default Page<Cita> findByApoderadoIdAndMascotaIdPaginated(Long apoderadoId, Long mascotaId, Pageable pageable) {
        return findByMascota_Apoderado_IdAndMascota_IdAndEliminadaFalseOrderByFechaHoraInicioDesc(
                apoderadoId, mascotaId, pageable);
    }

    @Query("SELECT c FROM Cita c WHERE c.empleado.id = :empleadoId " +
            "AND " + ESTADOS_NO_ACTIVOS + " " +
            "AND CAST(c.fechaHoraInicio AS date) = CAST(:fecha AS date)")
    List<Cita> findActiveByEmpleadoIdAndFechaString(@Param("empleadoId") Long empleadoId,
                                                    @Param("fecha") String fecha);

    @Query("SELECT c FROM Cita c WHERE c.mascota.apoderado.id = :apoderadoId " +
            "AND " + ESTADOS_NO_ACTIVOS + " " +
            "AND CAST(c.fechaHoraInicio AS date) = :fecha")
    List<Cita> findActiveByApoderadoIdAndFecha(@Param("apoderadoId") Long apoderadoId,
                                               @Param("fecha") LocalDate fecha);

    @Query("SELECT COUNT(c) > 0 FROM Cita c WHERE c.mascota.id = :mascotaId " +
            "AND c.eliminada = false AND c.fechaHoraInicio >= :ahora AND " + ESTADOS_VIGENTES)
    boolean existsCitaVigenteByMascotaId(@Param("mascotaId") Long mascotaId,
                                         @Param("ahora") LocalDateTime ahora);

    @Query("SELECT c FROM Cita c JOIN c.empleado e JOIN e.tiposEmpleado t " +
            "WHERE c.mascota.id = :mascotaId AND c.eliminada = false AND UPPER(t.nombre) = 'GROMMER' " +
            "ORDER BY c.fechaHoraInicio DESC")
    List<Cita> findServiciosNoMedicosParaMascota(@Param("mascotaId") Long mascotaId);
}