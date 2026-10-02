package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import veterinaria.vargasvet.domain.entity.SalidaLote;

import java.util.List;

public interface SalidaLoteRepository extends JpaRepository<SalidaLote, Long> {
    List<SalidaLote> findByReferenciaTipoAndReferenciaIdOrderByIdDesc(String referenciaTipo, Long referenciaId);
    boolean existsByLoteId(Long loteId);
}
