package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import veterinaria.vargasvet.domain.entity.EntregaAvisoPrivacidad;

import java.util.Optional;

public interface EntregaAvisoPrivacidadRepository extends JpaRepository<EntregaAvisoPrivacidad, Long> {

    boolean existsByUsuarioIdAndAvisoIdAndCanal(Integer usuarioId, Long avisoId, String canal);

    @EntityGraph(attributePaths = {"company", "usuario", "aviso"})
    Optional<EntregaAvisoPrivacidad> findOneById(Long id);

    @Modifying(flushAutomatically = true)
    @Query("UPDATE EntregaAvisoPrivacidad e SET e.destino = :reemplazo, e.detalle = NULL "
            + "WHERE e.usuario.id = :usuarioId AND e.company.id = :companyId")
    int anonimizarDestino(@Param("usuarioId") Integer usuarioId, @Param("companyId") Integer companyId,
                          @Param("reemplazo") String reemplazo);
}
