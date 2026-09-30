package veterinaria.vargasvet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import veterinaria.vargasvet.domain.entity.MascotaPersonaRelacion;

import java.util.List;
import java.util.Optional;

public interface MascotaPersonaRelacionRepository extends JpaRepository<MascotaPersonaRelacion, Long> {

    @Query("SELECT r FROM MascotaPersonaRelacion r " +
            "JOIN FETCH r.apoderado a JOIN FETCH a.user " +
            "WHERE r.mascota.uuid = :mascotaUuid AND r.company.id = :companyId " +
            "ORDER BY CASE WHEN r.tipoRelacion = veterinaria.vargasvet.domain.enums.TipoRelacionMascota.PROPIETARIO_PRINCIPAL THEN 0 ELSE 1 END, r.createdAt")
    List<MascotaPersonaRelacion> findByMascotaUuidAndCompanyId(@Param("mascotaUuid") String mascotaUuid,
                                                               @Param("companyId") Integer companyId);

    @Query("SELECT r FROM MascotaPersonaRelacion r " +
            "JOIN FETCH r.mascota m JOIN FETCH r.apoderado a JOIN FETCH a.user " +
            "WHERE r.uuid = :uuid AND r.company.id = :companyId")
    Optional<MascotaPersonaRelacion> findByUuidAndCompanyId(@Param("uuid") String uuid,
                                                            @Param("companyId") Integer companyId);

    Optional<MascotaPersonaRelacion> findByMascotaIdAndApoderadoId(Long mascotaId, Long apoderadoId);
}
