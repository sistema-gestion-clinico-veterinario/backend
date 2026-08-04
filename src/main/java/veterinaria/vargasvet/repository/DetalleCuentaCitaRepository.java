package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import veterinaria.vargasvet.domain.entity.DetalleCuentaCita;

import java.util.List;

public interface DetalleCuentaCitaRepository extends JpaRepository<DetalleCuentaCita, Long> {
    List<DetalleCuentaCita> findByCitaIdOrderByCreatedAtAscIdAsc(Long citaId);
    boolean existsByCitaIdAndEsServicioBaseTrue(Long citaId);
}
