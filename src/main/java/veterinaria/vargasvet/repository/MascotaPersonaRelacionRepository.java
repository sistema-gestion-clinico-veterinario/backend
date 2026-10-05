package veterinaria.vargasvet.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import veterinaria.vargasvet.domain.entity.Apoderado;
import veterinaria.vargasvet.domain.entity.Mascota;
import veterinaria.vargasvet.domain.entity.MascotaPersonaRelacion;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MascotaPersonaRelacionRepository extends JpaRepository<MascotaPersonaRelacion, Long> {

    /** Un vínculo está vigente si está activo, ya empezó y todavía no terminó. */
    String VIGENTE = "r.activo = true AND r.fechaInicio <= :hoy AND (r.fechaFin IS NULL OR r.fechaFin >= :hoy)";

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

    /** Los vínculos de la persona con la mascota que siguen activos (vigentes, por empezar o vencidos sin cerrar). */
    List<MascotaPersonaRelacion> findAllByMascotaIdAndApoderadoIdAndActivoTrue(Long mascotaId, Long apoderadoId);

    Optional<MascotaPersonaRelacion> findFirstByMascotaIdAndApoderadoIdAndTipoRelacionAndActivoTrue(
            Long mascotaId, Long apoderadoId, veterinaria.vargasvet.domain.enums.TipoRelacionMascota tipoRelacion);

    /** ¿Alguna persona ACTIVA de esta empresa puede autorizar la atención de la mascota ahora mismo? */
    @Query("SELECT COUNT(r) > 0 FROM MascotaPersonaRelacion r " +
            "WHERE r.mascota.id = :mascotaId AND r.company.id = :companyId " +
            "AND " + VIGENTE + " AND r.puedeAutorizarAtencion = true " +
            "AND r.apoderado.estado = true AND r.apoderado.company.id = :companyId")
    boolean existsActiveAuthorizer(@Param("mascotaId") Long mascotaId,
                                   @Param("companyId") Integer companyId,
                                   @Param("hoy") LocalDate hoy);

    /** Personas que pueden autorizar la atención de la mascota en esta empresa, activas o no en este momento. */
    @Query("SELECT DISTINCT r.apoderado FROM MascotaPersonaRelacion r " +
            "WHERE r.mascota.id = :mascotaId AND r.company.id = :companyId " +
            "AND " + VIGENTE + " AND r.puedeAutorizarAtencion = true")
    List<Apoderado> findAuthorizers(@Param("mascotaId") Long mascotaId,
                                    @Param("companyId") Integer companyId,
                                    @Param("hoy") LocalDate hoy);

    /** Mascotas de ESTA empresa cuya atención esta persona puede autorizar (como principal o copropietaria). */
    @Query("SELECT DISTINCT r.mascota FROM MascotaPersonaRelacion r " +
            "WHERE r.apoderado.id = :apoderadoId AND r.company.id = :companyId " +
            "AND " + VIGENTE + " AND r.puedeAutorizarAtencion = true")
    List<Mascota> findPetsAuthorizedBy(@Param("apoderadoId") Long apoderadoId,
                                       @Param("companyId") Integer companyId,
                                       @Param("hoy") LocalDate hoy);

    /** Vínculos vigentes de una persona activa, con su mascota. */
    @Query("SELECT r FROM MascotaPersonaRelacion r JOIN FETCH r.mascota " +
            "WHERE r.apoderado.id = :apoderadoId AND r.company.id = :companyId " +
            "AND " + VIGENTE + " AND r.apoderado.estado = true")
    List<MascotaPersonaRelacion> findVigentesDeLaPersona(@Param("apoderadoId") Long apoderadoId,
                                                         @Param("companyId") Integer companyId,
                                                         @Param("hoy") LocalDate hoy);

    @Query("SELECT r FROM MascotaPersonaRelacion r " +
            "WHERE r.apoderado.id = :apoderadoId AND r.mascota.id = :mascotaId AND r.company.id = :companyId " +
            "AND " + VIGENTE + " AND r.apoderado.estado = true")
    List<MascotaPersonaRelacion> findVigentesDeLaPersonaYMascota(@Param("apoderadoId") Long apoderadoId,
                                                                 @Param("mascotaId") Long mascotaId,
                                                                 @Param("companyId") Integer companyId,
                                                                 @Param("hoy") LocalDate hoy);

    /** Vínculos vigentes de varias mascotas con sus personas activas (para decidir a quién se avisa). */
    @Query("SELECT r FROM MascotaPersonaRelacion r JOIN FETCH r.apoderado a JOIN FETCH a.user JOIN FETCH r.mascota " +
            "WHERE r.mascota.id IN :mascotaIds AND r.company.id = :companyId " +
            "AND " + VIGENTE + " AND a.estado = true AND a.company.id = :companyId")
    List<MascotaPersonaRelacion> findVigentesDeLasMascotas(@Param("mascotaIds") Collection<Long> mascotaIds,
                                                           @Param("companyId") Integer companyId,
                                                           @Param("hoy") LocalDate hoy);

    /** ¿La persona tiene algún vínculo vigente con alguna mascota de esta empresa? */
    @Query("SELECT COUNT(r) > 0 FROM MascotaPersonaRelacion r " +
            "WHERE r.apoderado.id = :apoderadoId AND r.company.id = :companyId AND " + VIGENTE)
    boolean existsVigenteDeLaPersona(@Param("apoderadoId") Long apoderadoId,
                                     @Param("companyId") Integer companyId,
                                     @Param("hoy") LocalDate hoy);

    /** Vínculos que siguen marcados como activos aunque su fecha de fin ya pasó. */
    @Query("SELECT r FROM MascotaPersonaRelacion r JOIN FETCH r.mascota JOIN FETCH r.apoderado a JOIN FETCH a.user " +
            "WHERE r.activo = true AND r.fechaFin IS NOT NULL AND r.fechaFin < :hoy ORDER BY r.id")
    List<MascotaPersonaRelacion> findVencidasSinCerrar(@Param("hoy") LocalDate hoy, Pageable pageable);
}
