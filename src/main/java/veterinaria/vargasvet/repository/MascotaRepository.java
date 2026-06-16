package veterinaria.vargasvet.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.Mascota;
import veterinaria.vargasvet.domain.enums.EspecieMascota;

import java.util.Optional;
import java.util.List;

@Repository
public interface MascotaRepository extends JpaRepository<Mascota, Long> {

    @Query("SELECT m FROM Mascota m WHERE m.apoderado.id = :apoderadoId")
    List<Mascota> findByApoderadoId(@Param("apoderadoId") Long apoderadoId);

    @Query("SELECT m FROM Mascota m WHERE m.apoderado.id = :apoderadoId AND m.activo = true ORDER BY m.nombreCompleto ASC")
    Page<Mascota> findActiveByApoderadoId(@Param("apoderadoId") Long apoderadoId, Pageable pageable);

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

    @Query(value = "SELECT m FROM Mascota m JOIN m.apoderado a JOIN a.user u " +
                   "WHERE u.company.id = :companyId " +
                   "AND (CAST(:nombre AS text) IS NULL OR LOWER(m.nombreCompleto) LIKE LOWER(CONCAT('%', CAST(:nombre AS text), '%'))) " +
                   "AND (:especie IS NULL OR m.especie = :especie) " +
                   "AND (:activo IS NULL OR m.activo = :activo) " +
                   "AND (CAST(:nombrePropietario AS text) IS NULL OR LOWER(CONCAT(u.nombre, ' ', u.apellido)) LIKE LOWER(CONCAT('%', CAST(:nombrePropietario AS text), '%'))) " +
                   "ORDER BY m.nombreCompleto ASC",
           countQuery = "SELECT COUNT(m) FROM Mascota m JOIN m.apoderado a JOIN a.user u " +
                        "WHERE u.company.id = :companyId " +
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

    @Query("SELECT COUNT(m) FROM Mascota m WHERE m.apoderado.user.company.id = :companyId")
    long countByCompanyId(@Param("companyId") Integer companyId);

    Optional<Mascota> findByUuid(String uuid);
}
