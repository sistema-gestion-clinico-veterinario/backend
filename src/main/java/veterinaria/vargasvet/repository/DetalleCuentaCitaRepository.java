package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import veterinaria.vargasvet.domain.entity.DetalleCuentaCita;

import java.util.Collection;
import java.util.List;

public interface DetalleCuentaCitaRepository extends JpaRepository<DetalleCuentaCita, Long> {
    List<DetalleCuentaCita> findByCitaIdOrderByCreatedAtAscIdAsc(Long citaId);
    boolean existsByCitaIdAndEsServicioBaseTrue(Long citaId);

    @Query("""
            SELECT d
            FROM DetalleCuentaCita d
            WHERE d.cita.id IN :citaIds
            ORDER BY d.createdAt ASC, d.id ASC
            """)
    List<DetalleCuentaCita> findDeLasCitas(@Param("citaIds") Collection<Long> citaIds);
}
