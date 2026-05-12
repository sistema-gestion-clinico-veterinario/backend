package veterinaria.vargasvet.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.Empleado;

import java.util.Optional;

@Repository
public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {

    Optional<Empleado> findByNumeroColegiatura(String numeroColegiatura);
    boolean existsByNumeroColegiatura(String numeroColegiatura);
    Optional<Empleado> findByUserId(Integer userId);

    @Query(value = "SELECT e FROM Empleado e JOIN e.user u " +
                   "WHERE u.company.id = :companyId " +
                   "AND (CAST(:nombre AS text) IS NULL OR LOWER(u.nombre) LIKE LOWER(CONCAT('%', CAST(:nombre AS text), '%'))) " +
                   "AND (CAST(:apellido AS text) IS NULL OR LOWER(u.apellido) LIKE LOWER(CONCAT('%', CAST(:apellido AS text), '%'))) " +
                   "AND (CAST(:email AS text) IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:email AS text), '%'))) " +
                   "AND (:tipoEmpleadoId IS NULL OR EXISTS (SELECT t FROM e.tiposEmpleado t WHERE t.id = :tipoEmpleadoId)) " +
                   "AND (:especialidadId IS NULL OR EXISTS (SELECT es FROM e.especialidades es WHERE es.id = :especialidadId)) " +
                   "ORDER BY u.apellido ASC, u.nombre ASC",
           countQuery = "SELECT COUNT(e) FROM Empleado e JOIN e.user u " +
                        "WHERE u.company.id = :companyId " +
                        "AND (CAST(:nombre AS text) IS NULL OR LOWER(u.nombre) LIKE LOWER(CONCAT('%', CAST(:nombre AS text), '%'))) " +
                        "AND (CAST(:apellido AS text) IS NULL OR LOWER(u.apellido) LIKE LOWER(CONCAT('%', CAST(:apellido AS text), '%'))) " +
                        "AND (CAST(:email AS text) IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:email AS text), '%'))) " +
                        "AND (:tipoEmpleadoId IS NULL OR EXISTS (SELECT t FROM e.tiposEmpleado t WHERE t.id = :tipoEmpleadoId)) " +
                        "AND (:especialidadId IS NULL OR EXISTS (SELECT es FROM e.especialidades es WHERE es.id = :especialidadId))")
    Page<Empleado> buscar(@Param("companyId") Integer companyId,
                          @Param("nombre") String nombre,
                          @Param("apellido") String apellido,
                          @Param("email") String email,
                          @Param("tipoEmpleadoId") Long tipoEmpleadoId,
                          @Param("especialidadId") Long especialidadId,
                          Pageable pageable);

    @Query("SELECT COUNT(e) FROM Empleado e WHERE e.user.company.id = :companyId")
    long countByCompanyId(@Param("companyId") Integer companyId);
}
