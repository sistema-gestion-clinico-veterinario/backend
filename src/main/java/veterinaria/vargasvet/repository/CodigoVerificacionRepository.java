package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import veterinaria.vargasvet.domain.entity.CodigoVerificacion;

import java.util.Optional;

public interface CodigoVerificacionRepository extends JpaRepository<CodigoVerificacion, Long> {

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM CodigoVerificacion c WHERE c.usuario.id = :usuarioId AND c.company.id = :companyId "
            + "AND c.proposito = :proposito AND c.usadoAt IS NULL")
    void deletePendientes(@Param("usuarioId") Integer usuarioId, @Param("companyId") Integer companyId,
                          @Param("proposito") String proposito);

    Optional<CodigoVerificacion> findFirstByUsuarioIdAndCompanyIdAndPropositoAndUsadoAtIsNullOrderByCreadoAtDesc(
            Integer usuarioId, Integer companyId, String proposito);
}
