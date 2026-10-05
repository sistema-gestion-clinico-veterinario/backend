package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.ConsentimientoDatos;
import veterinaria.vargasvet.domain.enums.EstadoConsentimiento;
import veterinaria.vargasvet.domain.enums.FinalidadDatos;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConsentimientoDatosRepository extends JpaRepository<ConsentimientoDatos, Long> {

    Optional<ConsentimientoDatos> findFirstByUsuarioIdAndCompanyIdAndFinalidadOrderByIdDesc(
            Integer usuarioId, Integer companyId, FinalidadDatos finalidad);

    List<ConsentimientoDatos> findByUsuarioIdAndCompanyIdOrderByIdDesc(Integer usuarioId, Integer companyId);

    @Query("SELECT c FROM ConsentimientoDatos c WHERE c.usuario.id = :usuarioId AND c.company.id = :companyId AND c.id = "
            + "(SELECT MAX(c2.id) FROM ConsentimientoDatos c2 WHERE c2.usuario.id = c.usuario.id "
            + "AND c2.company.id = c.company.id AND c2.finalidad = c.finalidad)")
    List<ConsentimientoDatos> ultimasPorFinalidad(@Param("usuarioId") Integer usuarioId,
                                                   @Param("companyId") Integer companyId);

    @Query("SELECT c.usuario.id FROM ConsentimientoDatos c WHERE c.usuario.id IN :usuarioIds "
            + "AND c.company.id = :companyId AND c.finalidad = :finalidad "
            + "AND c.estado = :estado AND c.id = "
            + "(SELECT MAX(c2.id) FROM ConsentimientoDatos c2 WHERE c2.usuario.id = c.usuario.id "
            + "AND c2.company.id = :companyId AND c2.finalidad = :finalidad)")
    List<Integer> usuariosConUltimoEstado(@Param("usuarioIds") Collection<Integer> usuarioIds,
                                          @Param("companyId") Integer companyId,
                                          @Param("finalidad") FinalidadDatos finalidad,
                                          @Param("estado") EstadoConsentimiento estado);

    @Query("SELECT DISTINCT c.usuario.id FROM ConsentimientoDatos c WHERE c.usuario.id IN :usuarioIds "
            + "AND c.company.id = :companyId AND c.finalidad = :finalidad")
    List<Integer> usuariosConRegistro(@Param("usuarioIds") Collection<Integer> usuarioIds,
                                      @Param("companyId") Integer companyId,
                                      @Param("finalidad") FinalidadDatos finalidad);
}
