package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import veterinaria.vargasvet.domain.entity.SolicitudCancelacionDatos;
import veterinaria.vargasvet.domain.enums.EstadoSolicitudCancelacion;

import java.util.List;
import java.util.Optional;

public interface SolicitudCancelacionDatosRepository extends JpaRepository<SolicitudCancelacionDatos, Long> {

    boolean existsByUsuarioIdAndCompanyIdAndEstado(Integer usuarioId, Integer companyId, EstadoSolicitudCancelacion estado);

    Optional<SolicitudCancelacionDatos> findFirstByUsuarioIdAndCompanyIdOrderByIdDesc(Integer usuarioId, Integer companyId);

    Optional<SolicitudCancelacionDatos> findByIdAndCompanyId(Long id, Integer companyId);

    @Query("SELECT s FROM SolicitudCancelacionDatos s WHERE s.company.id = :companyId "
            + "AND (:estado IS NULL OR s.estado = :estado) ORDER BY s.creadaAt DESC")
    List<SolicitudCancelacionDatos> listar(@Param("companyId") Integer companyId,
                                           @Param("estado") EstadoSolicitudCancelacion estado);
}
