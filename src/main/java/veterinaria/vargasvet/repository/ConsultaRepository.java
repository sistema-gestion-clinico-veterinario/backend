package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.Consulta;

import java.util.Optional;

@Repository
public interface ConsultaRepository extends JpaRepository<Consulta, Long> {
    Optional<Consulta> findByCitaId(Long citaId);
}
