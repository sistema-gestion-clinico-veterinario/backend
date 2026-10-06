package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import veterinaria.vargasvet.domain.entity.CodigoVerificacion;

import java.time.LocalDateTime;
import java.util.Optional;

public interface CodigoVerificacionRepository extends JpaRepository<CodigoVerificacion, Long> {

    @Modifying(flushAutomatically = true)
    @Query("UPDATE CodigoVerificacion c SET c.usadoAt = :ahora WHERE c.usuario.id = :usuarioId AND c.company.id = :companyId "
            + "AND c.proposito = :proposito AND c.usadoAt IS NULL")
    void invalidarPendientes(@Param("usuarioId") Integer usuarioId, @Param("companyId") Integer companyId,
                             @Param("proposito") String proposito, @Param("ahora") LocalDateTime ahora);

    long countByUsuarioIdAndCompanyIdAndPropositoAndCreadoAtAfter(
            Integer usuarioId, Integer companyId, String proposito, LocalDateTime desde);

    Optional<CodigoVerificacion> findFirstByUsuarioIdAndCompanyIdAndPropositoAndUsadoAtIsNullOrderByCreadoAtDesc(
            Integer usuarioId, Integer companyId, String proposito);

    Optional<CodigoVerificacion> findFirstByUsuarioIdAndCompanyIdAndPropositoOrderByCreadoAtDesc(
            Integer usuarioId, Integer companyId, String proposito);
}
