package veterinaria.vargasvet.modules.clinical.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.modules.clinical.domain.entity.Prescripcion;

import java.util.List;

@Repository
public interface PrescripcionRepository extends JpaRepository<Prescripcion, Long> {
    List<Prescripcion> findByConsultaIdOrderByCreatedAtAsc(Long consultaId);
}
