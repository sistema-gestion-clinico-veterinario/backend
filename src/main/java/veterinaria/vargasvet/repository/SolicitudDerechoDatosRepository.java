package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import veterinaria.vargasvet.domain.entity.SolicitudDerechoDatos;
import veterinaria.vargasvet.domain.enums.EstadoSolicitudDerecho;
import veterinaria.vargasvet.domain.enums.TipoDerechoDatos;

import java.util.List;
import java.util.Optional;

public interface SolicitudDerechoDatosRepository extends JpaRepository<SolicitudDerechoDatos, Long> {

    @Query("SELECT s FROM SolicitudDerechoDatos s JOIN FETCH s.usuario " +
            "WHERE s.company.id = :companyId AND (:estado IS NULL OR s.estado = :estado) ORDER BY s.creadaAt DESC")
    List<SolicitudDerechoDatos> listar(@Param("companyId") Integer companyId,
                                       @Param("estado") EstadoSolicitudDerecho estado);

    @Query("SELECT s FROM SolicitudDerechoDatos s WHERE s.usuario.id = :userId " +
            "AND s.company.id = :companyId ORDER BY s.creadaAt DESC")
    List<SolicitudDerechoDatos> listarDelTitular(@Param("userId") Integer userId,
                                                 @Param("companyId") Integer companyId);

    @Query("SELECT s FROM SolicitudDerechoDatos s JOIN FETCH s.usuario WHERE s.id = :id AND s.company.id = :companyId")
    Optional<SolicitudDerechoDatos> findByIdAndCompanyId(@Param("id") Long id,
                                                        @Param("companyId") Integer companyId);

    @Query("SELECT COUNT(s) > 0 FROM SolicitudDerechoDatos s WHERE s.usuario.id = :userId " +
            "AND s.company.id = :companyId AND s.tipo = :tipo " +
            "AND s.estado IN (veterinaria.vargasvet.domain.enums.EstadoSolicitudDerecho.RECIBIDA, " +
            "veterinaria.vargasvet.domain.enums.EstadoSolicitudDerecho.EN_REVISION, " +
            "veterinaria.vargasvet.domain.enums.EstadoSolicitudDerecho.REQUIERE_INFORMACION)")
    boolean existeAbierta(@Param("userId") Integer userId,
                          @Param("companyId") Integer companyId,
                          @Param("tipo") TipoDerechoDatos tipo);
}
