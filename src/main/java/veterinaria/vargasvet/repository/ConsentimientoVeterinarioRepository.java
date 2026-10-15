package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import veterinaria.vargasvet.domain.entity.ConsentimientoInformadoVeterinario;
import veterinaria.vargasvet.domain.enums.EstadoConsentimientoVeterinario;

import java.util.List;
import java.util.Optional;

public interface ConsentimientoVeterinarioRepository extends JpaRepository<ConsentimientoInformadoVeterinario, Long> {

    @Query("SELECT c FROM ConsentimientoInformadoVeterinario c " +
            "JOIN FETCH c.mascota JOIN FETCH c.apoderado a JOIN FETCH a.user " +
            "WHERE c.company.id = :companyId AND (:mascotaId IS NULL OR c.mascota.id = :mascotaId) " +
            "AND (:estado IS NULL OR c.estado = :estado) ORDER BY c.creadoAt DESC")
    List<ConsentimientoInformadoVeterinario> listar(@Param("companyId") Integer companyId,
                                                    @Param("mascotaId") Long mascotaId,
                                                    @Param("estado") EstadoConsentimientoVeterinario estado);

    @Query("SELECT c FROM ConsentimientoInformadoVeterinario c " +
            "JOIN FETCH c.mascota JOIN FETCH c.apoderado a JOIN FETCH a.user " +
            "WHERE a.id = :apoderadoId AND c.company.id = :companyId ORDER BY c.creadoAt DESC")
    List<ConsentimientoInformadoVeterinario> listarDelApoderado(@Param("apoderadoId") Long apoderadoId,
                                                               @Param("companyId") Integer companyId);

    @Query("SELECT c FROM ConsentimientoInformadoVeterinario c " +
            "JOIN FETCH c.mascota JOIN FETCH c.apoderado a JOIN FETCH a.user " +
            "WHERE c.uuid = :uuid AND c.company.id = :companyId")
    Optional<ConsentimientoInformadoVeterinario> findByUuidAndCompanyId(@Param("uuid") String uuid,
                                                                        @Param("companyId") Integer companyId);

    @Query("SELECT COALESCE(MAX(c.version), 0) FROM ConsentimientoInformadoVeterinario c " +
            "WHERE c.company.id = :companyId AND c.mascota.id = :mascotaId AND LOWER(c.procedimiento) = LOWER(:procedimiento)")
    int ultimaVersion(@Param("companyId") Integer companyId,
                      @Param("mascotaId") Long mascotaId,
                      @Param("procedimiento") String procedimiento);

    boolean existsByCompanyIdAndMascotaIdAndEstado(Integer companyId, Long mascotaId,
                                                    EstadoConsentimientoVeterinario estado);
}
