package veterinaria.vargasvet.modules.citas.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.modules.citas.domain.entity.Cita;
import veterinaria.vargasvet.modules.citas.domain.enums.EstadoCita;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Repository
public interface CitaRepository extends JpaRepository<Cita, Long> {

    @Query("SELECT COUNT(c) > 0 FROM Cita c WHERE c.empleado.id = :veterinarioId " +
           "AND c.estado != 'CANCELADA' AND c.eliminada = false " +
           "AND c.fechaHoraInicio < :fechaHoraFin " +
           "AND c.fechaHoraFin > :fechaHoraInicio")
    boolean existsOverlappingCita(@Param("veterinarioId") Long veterinarioId,
                                  @Param("fechaHoraInicio") LocalDateTime fechaHoraInicio,
                                  @Param("fechaHoraFin") LocalDateTime fechaHoraFin);

    @Query("SELECT COUNT(c) > 0 FROM Cita c WHERE c.empleado.id = :veterinarioId " +
           "AND c.id != :citaId " +
           "AND c.estado != 'CANCELADA' AND c.eliminada = false " +
           "AND c.fechaHoraInicio < :fechaHoraFin " +
           "AND c.fechaHoraFin > :fechaHoraInicio")
    boolean existsOverlappingCitaExcludeSelf(@Param("veterinarioId") Long veterinarioId,
                                             @Param("fechaHoraInicio") LocalDateTime fechaHoraInicio,
                                             @Param("fechaHoraFin") LocalDateTime fechaHoraFin,
                                             @Param("citaId") Long citaId);

    @Query("SELECT COUNT(c) > 0 FROM Cita c WHERE c.mascota.id = :mascotaId " +
           "AND c.estado != 'CANCELADA' AND c.eliminada = false " +
           "AND c.fechaHoraInicio < :fechaHoraFin " +
           "AND c.fechaHoraFin > :fechaHoraInicio")
    boolean existsOverlappingCitaMascota(@Param("mascotaId") Long mascotaId,
                                         @Param("fechaHoraInicio") LocalDateTime fechaHoraInicio,
                                         @Param("fechaHoraFin") LocalDateTime fechaHoraFin);

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

    @Query("SELECT COUNT(c) FROM Cita c WHERE c.mascota.apoderado.user.company.id = :companyId " +
           "AND c.eliminada = false AND CAST(c.fechaHoraInicio AS date) = CURRENT_DATE")
    long countTodayByCompanyId(@Param("companyId") Integer companyId);
}
