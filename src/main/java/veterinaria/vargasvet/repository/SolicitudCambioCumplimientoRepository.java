package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import veterinaria.vargasvet.domain.entity.SolicitudCambioCumplimiento;
import veterinaria.vargasvet.domain.enums.ControlCumplimientoClinica;
import veterinaria.vargasvet.domain.enums.EstadoSolicitudCambioCumplimiento;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

public interface SolicitudCambioCumplimientoRepository extends JpaRepository<SolicitudCambioCumplimiento, Long> {
    @Query("SELECT s FROM SolicitudCambioCumplimiento s " +
            "JOIN FETCH s.solicitadaPor WHERE s.company.id = :companyId ORDER BY s.solicitadaAt DESC")
    List<SolicitudCambioCumplimiento> listarPorEmpresa(@Param("companyId") Integer companyId);

    @Query("SELECT s FROM SolicitudCambioCumplimiento s JOIN FETCH s.company JOIN FETCH s.solicitadaPor " +
            "WHERE s.company.id = :companyId AND s.estado = :estado ORDER BY s.solicitadaAt DESC")
    List<SolicitudCambioCumplimiento> listarPorEmpresaYEstado(@Param("companyId") Integer companyId,
                                                              @Param("estado") EstadoSolicitudCambioCumplimiento estado);

    boolean existsByCompanyIdAndControlAndEstadoIn(Integer companyId, ControlCumplimientoClinica control,
                                                    List<EstadoSolicitudCambioCumplimiento> estados);

    @Query("SELECT s FROM SolicitudCambioCumplimiento s JOIN FETCH s.company JOIN FETCH s.solicitadaPor " +
            "WHERE s.id = :id")
    Optional<SolicitudCambioCumplimiento> buscarCompleta(@Param("id") Long id);

    @EntityGraph(attributePaths = {"company", "solicitadaPor", "resueltaPor", "revisionPor"})
    @Query("SELECT s FROM SolicitudCambioCumplimiento s WHERE " +
            "(:companyId IS NULL OR s.company.id = :companyId) AND " +
            "(:control IS NULL OR s.control = :control) AND " +
            "(:estado IS NULL OR s.estado = :estado) AND " +
            "s.solicitadaAt >= :desde AND " +
            "s.solicitadaAt < :hasta")
    Page<SolicitudCambioCumplimiento> buscarGlobal(@Param("companyId") Integer companyId,
                                                    @Param("control") ControlCumplimientoClinica control,
                                                    @Param("estado") EstadoSolicitudCambioCumplimiento estado,
                                                    @Param("desde") LocalDateTime desde,
                                                    @Param("hasta") LocalDateTime hasta,
                                                    Pageable pageable);

    long countByEstadoIn(List<EstadoSolicitudCambioCumplimiento> estados);

    @EntityGraph(attributePaths = {"company", "solicitadaPor", "resueltaPor", "revisionPor"})
    List<SolicitudCambioCumplimiento> findByCompanyIdAndEstadoInAndVistaClinicaAtIsNullOrderByResueltaAtDesc(
            Integer companyId, List<EstadoSolicitudCambioCumplimiento> estados);

    @EntityGraph(attributePaths = {"company", "solicitadaPor", "resueltaPor", "revisionPor"})
    List<SolicitudCambioCumplimiento> findByCompanyIdAndEstadoInOrderBySolicitadaAtDesc(
            Integer companyId, List<EstadoSolicitudCambioCumplimiento> estados);
}
