package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import veterinaria.vargasvet.domain.entity.EntregaAvisoPrivacidad;

import java.util.Optional;

public interface EntregaAvisoPrivacidadRepository extends JpaRepository<EntregaAvisoPrivacidad, Long> {

    boolean existsByUsuarioIdAndAvisoIdAndCanal(Integer usuarioId, Long avisoId, String canal);

    @EntityGraph(attributePaths = {"company", "usuario", "aviso"})
    Optional<EntregaAvisoPrivacidad> findOneById(Long id);
}
