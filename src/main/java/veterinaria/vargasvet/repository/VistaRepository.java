package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import veterinaria.vargasvet.domain.entity.Vista;

import java.util.List;
import java.util.Optional;

public interface VistaRepository extends JpaRepository<Vista, Integer> {
    Optional<Vista> findByCodigo(String codigo);
    boolean existsByCodigo(String codigo);
    List<Vista> findByGrupo(String grupo);
    List<Vista> findByActivoTrue();
    List<Vista> findByActivoTrueOrderByNombreAsc();
}
