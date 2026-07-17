package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import veterinaria.vargasvet.domain.entity.ControlPreventivo;
import veterinaria.vargasvet.domain.enums.EstadoControlPreventivo;
import veterinaria.vargasvet.domain.enums.TipoControlPreventivo;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface ControlPreventivoRepository extends JpaRepository<ControlPreventivo, Long> {
    List<ControlPreventivo> findByMascotaIdOrderByFechaRecomendadaDesc(Long mascotaId);
    List<ControlPreventivo> findByCitaSuspendeId(Long citaId);
    boolean existsByMascotaIdAndTipoAndEstadoIn(Long mascotaId, TipoControlPreventivo tipo,
                                                Collection<EstadoControlPreventivo> estados);

    @Query("SELECT cp FROM ControlPreventivo cp " +
           "JOIN FETCH cp.mascota m JOIN FETCH m.apoderado a JOIN FETCH a.user u " +
           "LEFT JOIN FETCH cp.tipoVacuna " +
           "WHERE cp.fechaRecomendada <= :hasta AND cp.estado IN :estados " +
           "AND m.activo = true AND u.activo = true AND u.emailVerified = true")
    List<ControlPreventivo> findReminderCandidates(@Param("hasta") LocalDate hasta,
                                                   @Param("estados") Collection<EstadoControlPreventivo> estados);
}
