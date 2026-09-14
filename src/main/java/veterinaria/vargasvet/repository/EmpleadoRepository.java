package veterinaria.vargasvet.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.Empleado;

import java.util.Optional;

@Repository
public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {

    interface DashboardEmployeeProjection {
        Long getEmpleadoId();
        String getNombreCompleto();
        String getCargo();
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Empleado e WHERE e.id = :id")
    Optional<Empleado> findByIdForAppointmentWrite(@Param("id") Long id);

    Optional<Empleado> findByNumeroColegiatura(String numeroColegiatura);
    Optional<Empleado> findByUserEmail(String email);

    /** Reemplaza a existsByNumeroColegiatura (global) - la unicidad real ahora es
     * por empresa y solo entre relaciones activas (uq_empleado_colegiatura_activo). */
    boolean existsByNumeroColegiaturaAndCompanyIdAndEstadoTrue(String numeroColegiatura, Integer companyId);

    /** El unico "empleado" con sentido de un usuario en un momento dado: como mucho hay
     * una fila activa por usuario (indice uq_empleado_activo_por_usuario), asi que este
     * metodo es seguro para cualquier usuario, sin importar cuantas filas historicas tenga. */
    @Query("SELECT e FROM Empleado e WHERE e.user.id = :userId AND e.estado = true")
    Optional<Empleado> findActiveByUserId(@Param("userId") Integer userId);

    boolean existsByUserIdAndEstadoTrue(Integer userId);
    boolean existsByUserId(Integer userId);
    boolean existsByUserIdAndCompanyIdAndEstadoTrue(Integer userId, Integer companyId);

    @Query("SELECT e FROM Empleado e WHERE e.id = :id AND e.company.id = :companyId")
    Optional<Empleado> findByIdAndCompanyId(@Param("id") Long id,
                                             @Param("companyId") Integer companyId);

    @Query(value = "SELECT e FROM Empleado e JOIN e.user u " +
                   "WHERE e.company.id = :companyId " +
                   "AND (CAST(:nombre AS text) IS NULL OR LOWER(u.nombre) LIKE LOWER(CONCAT('%', CAST(:nombre AS text), '%'))) " +
                   "AND (CAST(:apellido AS text) IS NULL OR LOWER(u.apellido) LIKE LOWER(CONCAT('%', CAST(:apellido AS text), '%'))) " +
                   "AND (CAST(:email AS text) IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:email AS text), '%'))) " +
                   "AND (CAST(:numeroDocumento AS text) IS NULL OR LOWER(u.dni) LIKE LOWER(CONCAT('%', CAST(:numeroDocumento AS text), '%'))) " +
                   "AND (:roleId IS NULL OR EXISTS (SELECT upr FROM u.usuariosPorRol upr WHERE upr.rol.id = :roleId)) " +
                   "AND (:activo IS NULL OR e.estado = :activo) " +
                   "AND (:tipoEmpleadoId IS NULL OR EXISTS (SELECT t FROM e.tiposEmpleado t WHERE t.id = :tipoEmpleadoId)) " +
                   "AND (:especialidadId IS NULL OR EXISTS (SELECT es FROM e.especialidades es WHERE es.id = :especialidadId)) " +
                   "ORDER BY u.apellido ASC, u.nombre ASC",
           countQuery = "SELECT COUNT(e) FROM Empleado e JOIN e.user u " +
                        "WHERE e.company.id = :companyId " +
                        "AND (CAST(:nombre AS text) IS NULL OR LOWER(u.nombre) LIKE LOWER(CONCAT('%', CAST(:nombre AS text), '%'))) " +
                        "AND (CAST(:apellido AS text) IS NULL OR LOWER(u.apellido) LIKE LOWER(CONCAT('%', CAST(:apellido AS text), '%'))) " +
                        "AND (CAST(:email AS text) IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:email AS text), '%'))) " +
                        "AND (CAST(:numeroDocumento AS text) IS NULL OR LOWER(u.dni) LIKE LOWER(CONCAT('%', CAST(:numeroDocumento AS text), '%'))) " +
                        "AND (:roleId IS NULL OR EXISTS (SELECT upr FROM u.usuariosPorRol upr WHERE upr.rol.id = :roleId)) " +
                        "AND (:activo IS NULL OR e.estado = :activo) " +
                        "AND (:tipoEmpleadoId IS NULL OR EXISTS (SELECT t FROM e.tiposEmpleado t WHERE t.id = :tipoEmpleadoId)) " +
                        "AND (:especialidadId IS NULL OR EXISTS (SELECT es FROM e.especialidades es WHERE es.id = :especialidadId))")
    Page<Empleado> buscar(@Param("companyId") Integer companyId,
                          @Param("nombre") String nombre,
                          @Param("apellido") String apellido,
                          @Param("email") String email,
                          @Param("numeroDocumento") String numeroDocumento,
                          @Param("roleId") Integer roleId,
                          @Param("activo") Boolean activo,
                          @Param("tipoEmpleadoId") Long tipoEmpleadoId,
                          @Param("especialidadId") Long especialidadId,
                          Pageable pageable);

    @Query("SELECT COUNT(e) FROM Empleado e WHERE e.company.id = :companyId")
    long countByCompanyId(@Param("companyId") Integer companyId);

    @Query("SELECT e FROM Empleado e WHERE e.company.id = :companyId")
    java.util.List<Empleado> findAllByCompanyId(@Param("companyId") Integer companyId);

    @Query("SELECT e.id AS empleadoId, " +
           "CONCAT(u.nombre, ' ', u.apellido) AS nombreCompleto, " +
           "COALESCE(MIN(t.nombre), 'Personal') AS cargo " +
           "FROM Empleado e JOIN e.user u LEFT JOIN e.tiposEmpleado t " +
           "WHERE e.company.id = :companyId AND e.estado = true " +
           "GROUP BY e.id, u.nombre, u.apellido " +
           "ORDER BY u.apellido ASC, u.nombre ASC")
    java.util.List<DashboardEmployeeProjection> findDashboardEmployeesByCompanyId(
            @Param("companyId") Integer companyId);

    @Query("SELECT e FROM Empleado e JOIN e.user u " +
           "WHERE e.company.id = :companyId AND e.estado = true " +
           "AND (:tipoEmpleadoId IS NULL OR EXISTS (SELECT t FROM e.tiposEmpleado t WHERE t.id = :tipoEmpleadoId)) " +
           "ORDER BY u.apellido ASC, u.nombre ASC")
    java.util.List<Empleado> findActiveByCompanyIdAndTipoEmpleadoId(@Param("companyId") Integer companyId, @Param("tipoEmpleadoId") Long tipoEmpleadoId);

    @Query("SELECT COUNT(e) FROM Empleado e WHERE e.estado = true AND EXISTS (SELECT t FROM e.tiposEmpleado t WHERE t.id = :tipoEmpleadoId)")
    long countActiveByTipoEmpleadoId(@Param("tipoEmpleadoId") Long tipoEmpleadoId);

    @Query("SELECT COUNT(e) FROM Empleado e WHERE EXISTS (SELECT t FROM e.tiposEmpleado t WHERE t.id = :tipoEmpleadoId)")
    long countByTipoEmpleadoId(@Param("tipoEmpleadoId") Long tipoEmpleadoId);

    @Modifying
    @Query(value = "DELETE FROM empleado_especialidad WHERE empleado_id = :empleadoId", nativeQuery = true)
    void removeEspecialidades(@Param("empleadoId") Long empleadoId);

    @Modifying
    @Query(value = "DELETE FROM empleado_tipo_empleado WHERE empleado_id = :empleadoId", nativeQuery = true)
    void removeTiposEmpleado(@Param("empleadoId") Long empleadoId);
}
