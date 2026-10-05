package veterinaria.vargasvet.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import veterinaria.vargasvet.domain.entity.Mascota;
import veterinaria.vargasvet.domain.enums.EspecieMascota;

import java.util.Optional;
import java.util.List;

@Repository
public interface MascotaRepository extends JpaRepository<Mascota, Long> {

    @Query("SELECT m FROM Mascota m WHERE m.apoderado.id = :apoderadoId")
    List<Mascota> findByApoderadoId(@Param("apoderadoId") Long apoderadoId);

    /** Mascotas de las que esta persona es propietaria principal, siempre dentro de su empresa. */
    @Query("SELECT m FROM Mascota m WHERE m.apoderado.id = :apoderadoId AND m.apoderado.company.id = :companyId")
    List<Mascota> findByApoderadoIdAndCompanyId(@Param("apoderadoId") Long apoderadoId,
                                                @Param("companyId") Integer companyId);

    @Query("SELECT m FROM Mascota m JOIN FETCH m.apoderado a " +
           "WHERE m.activo = true AND a.estado = false AND m.id > :afterId ORDER BY m.id")
    List<Mascota> findActiveWithInactiveOwner(@Param("afterId") Long afterId,
                                              org.springframework.data.domain.Pageable pageable);

    boolean existsByApoderadoIdAndActivoTrue(Long apoderadoId);

    @Query("SELECT DISTINCT m.apoderado.id FROM Mascota m WHERE m.apoderado.id IN :apoderadoIds AND m.activo = true")
    java.util.Set<Long> apoderadosConMascotaActiva(@Param("apoderadoIds") java.util.Collection<Long> apoderadoIds);

    @Query("SELECT m FROM Mascota m JOIN FETCH m.apoderado a JOIN FETCH a.user " +
           "WHERE a.company.id = :companyId AND m.activo = true")
    List<Mascota> findActiveByCompanyId(@Param("companyId") Integer companyId);

    /**
     * Mascotas activas sin visita reciente (o que nunca visitaron), paginadas y ordenadas por
     * antigüedad directamente en SQL — evita cargar todas las mascotas activas de la empresa en
     * memoria solo para calcular este panel del reporte, algo que no escala con clínicas grandes.
     * "Sin visita reciente" usa la fecha de la última cita COMPLETADA, o la fecha de alta si nunca
     * tuvo una.
     */
    @Query(
        value = "SELECT m FROM Mascota m JOIN FETCH m.apoderado a JOIN FETCH a.user " +
            "WHERE a.company.id = :companyId AND m.activo = true " +
            "AND COALESCE(" +
            "  (SELECT MAX(c.fechaHoraInicio) FROM Cita c WHERE c.mascota = m AND c.estado = 'COMPLETADA' AND c.eliminada = false), " +
            "  m.createdAt" +
            ") < :umbral " +
            "ORDER BY COALESCE(" +
            "  (SELECT MAX(c.fechaHoraInicio) FROM Cita c WHERE c.mascota = m AND c.estado = 'COMPLETADA' AND c.eliminada = false), " +
            "  m.createdAt" +
            ") ASC",
        countQuery = "SELECT COUNT(m) FROM Mascota m " +
            "WHERE m.apoderado.company.id = :companyId AND m.activo = true " +
            "AND COALESCE(" +
            "  (SELECT MAX(c.fechaHoraInicio) FROM Cita c WHERE c.mascota = m AND c.estado = 'COMPLETADA' AND c.eliminada = false), " +
            "  m.createdAt" +
            ") < :umbral"
    )
    Page<Mascota> findInactivasByCompanyId(@Param("companyId") Integer companyId,
                                           @Param("umbral") java.time.LocalDateTime umbral,
                                           Pageable pageable);

    @Query("SELECT m FROM Mascota m WHERE m.apoderado.id = :apoderadoId AND m.activo = true ORDER BY m.nombreCompleto ASC")
    Page<Mascota> findActiveByApoderadoId(@Param("apoderadoId") Long apoderadoId, Pageable pageable);

    @Query("SELECT m FROM Mascota m WHERE m.id IN :mascotaIds " +
           "AND (CAST(:nombre AS text) IS NULL OR LOWER(m.nombreCompleto) LIKE LOWER(CONCAT('%', CAST(:nombre AS text), '%'))) " +
           "AND (:especie IS NULL OR m.especie = :especie) " +
           "AND (:activo IS NULL OR m.activo = :activo) " +
           "ORDER BY m.nombreCompleto ASC")
    Page<Mascota> buscarPortalMascotasPorIds(@Param("mascotaIds") java.util.Collection<Long> mascotaIds,
                                             @Param("nombre") String nombre,
                                             @Param("especie") veterinaria.vargasvet.domain.enums.EspecieMascota especie,
                                             @Param("activo") Boolean activo,
                                             Pageable pageable);

    @Query("SELECT m FROM Mascota m WHERE m.apoderado.id = :apoderadoId " +
           "AND (CAST(:nombre AS text) IS NULL OR LOWER(m.nombreCompleto) LIKE LOWER(CONCAT('%', CAST(:nombre AS text), '%'))) " +
           "AND (:especie IS NULL OR m.especie = :especie) " +
           "AND (:activo IS NULL OR m.activo = :activo) " +
           "ORDER BY m.nombreCompleto ASC")
    Page<Mascota> buscarPortalMascotas(@Param("apoderadoId") Long apoderadoId,
                                       @Param("nombre") String nombre,
                                       @Param("especie") EspecieMascota especie,
                                       @Param("activo") Boolean activo,
                                       Pageable pageable);

    @Query(value = "SELECT m FROM Mascota m JOIN FETCH m.apoderado a JOIN FETCH a.user u " +
                   "WHERE a.company.id = :companyId " +
                   "AND (CAST(:nombre AS text) IS NULL OR LOWER(m.nombreCompleto) LIKE LOWER(CONCAT('%', CAST(:nombre AS text), '%'))) " +
                   "AND (:especie IS NULL OR m.especie = :especie) " +
                   "AND (:activo IS NULL OR m.activo = :activo) " +
                   "AND (CAST(:nombrePropietario AS text) IS NULL OR LOWER(CONCAT(u.nombre, ' ', u.apellido)) LIKE LOWER(CONCAT('%', CAST(:nombrePropietario AS text), '%'))) " +
                   "ORDER BY m.nombreCompleto ASC",
           countQuery = "SELECT COUNT(m) FROM Mascota m JOIN m.apoderado a JOIN a.user u " +
                        "WHERE a.company.id = :companyId " +
                        "AND (CAST(:nombre AS text) IS NULL OR LOWER(m.nombreCompleto) LIKE LOWER(CONCAT('%', CAST(:nombre AS text), '%'))) " +
                        "AND (:especie IS NULL OR m.especie = :especie) " +
                        "AND (:activo IS NULL OR m.activo = :activo) " +
                        "AND (CAST(:nombrePropietario AS text) IS NULL OR LOWER(CONCAT(u.nombre, ' ', u.apellido)) LIKE LOWER(CONCAT('%', CAST(:nombrePropietario AS text), '%')))")
    Page<Mascota> buscar(@Param("companyId") Integer companyId,
                         @Param("nombre") String nombre,
                         @Param("especie") EspecieMascota especie,
                         @Param("nombrePropietario") String nombrePropietario,
                         @Param("activo") Boolean activo,
                         Pageable pageable);

    @Query("SELECT COUNT(m) FROM Mascota m WHERE m.apoderado.company.id = :companyId")
    long countByCompanyId(@Param("companyId") Integer companyId);

    Optional<Mascota> findByUuid(String uuid);

    @Query("SELECT m FROM Mascota m JOIN FETCH m.apoderado a JOIN FETCH a.user u " +
           "WHERE m.id = :id AND a.company.id = :companyId")
    Optional<Mascota> findByIdAndCompanyId(@Param("id") Long id,
                                           @Param("companyId") Integer companyId);

    @Query("SELECT m FROM Mascota m JOIN FETCH m.apoderado a JOIN FETCH a.user u " +
           "WHERE m.uuid = :uuid AND a.company.id = :companyId")
    Optional<Mascota> findByUuidAndCompanyId(@Param("uuid") String uuid,
                                             @Param("companyId") Integer companyId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Mascota m JOIN FETCH m.apoderado a JOIN FETCH a.user u WHERE m.id = :id")
    Optional<Mascota> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT m.especie, COUNT(m) FROM Mascota m " +
           "JOIN m.apoderado a " +
           "WHERE a.company.id = :companyId AND m.activo = true " +
           "GROUP BY m.especie")
    List<Object[]> countByEspecie(@Param("companyId") Integer companyId);

    @Query("SELECT m.fechaNacimiento FROM Mascota m " +
           "JOIN m.apoderado a " +
           "WHERE a.company.id = :companyId AND m.activo = true AND m.fechaNacimiento IS NOT NULL")
    List<java.time.LocalDate> findFechasNacimientoByCompany(@Param("companyId") Integer companyId);
}
