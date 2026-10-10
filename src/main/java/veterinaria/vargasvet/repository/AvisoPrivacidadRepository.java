package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.AvisoPrivacidad;
import veterinaria.vargasvet.domain.enums.AudienciaAvisoPrivacidad;

import java.util.List;
import java.util.Optional;

@Repository
public interface AvisoPrivacidadRepository extends JpaRepository<AvisoPrivacidad, Long> {

    Optional<AvisoPrivacidad> findByCompanyIdAndAudienciaAndActivoTrue(
            Integer companyId, AudienciaAvisoPrivacidad audiencia);

    Optional<AvisoPrivacidad> findByCompanyIdAndAudienciaAndVersion(
            Integer companyId, AudienciaAvisoPrivacidad audiencia, Integer version);

    List<AvisoPrivacidad> findByCompanyIdAndAudienciaOrderByVersionDesc(
            Integer companyId, AudienciaAvisoPrivacidad audiencia);

    @Query("SELECT COALESCE(MAX(a.version), 0) FROM AvisoPrivacidad a "
            + "WHERE a.company.id = :companyId AND a.audiencia = :audiencia")
    int ultimaVersion(@Param("companyId") Integer companyId,
                      @Param("audiencia") AudienciaAvisoPrivacidad audiencia);

    default Optional<AvisoPrivacidad> findByCompanyIdAndActivoTrue(Integer companyId) {
        return findByCompanyIdAndAudienciaAndActivoTrue(
                companyId, AudienciaAvisoPrivacidad.PROPIETARIOS_Y_AUTORIZADOS);
    }

    default List<AvisoPrivacidad> findByCompanyIdOrderByVersionDesc(Integer companyId) {
        return findByCompanyIdAndAudienciaOrderByVersionDesc(
                companyId, AudienciaAvisoPrivacidad.PROPIETARIOS_Y_AUTORIZADOS);
    }

    default int ultimaVersion(Integer companyId) {
        return ultimaVersion(companyId, AudienciaAvisoPrivacidad.PROPIETARIOS_Y_AUTORIZADOS);
    }
}
