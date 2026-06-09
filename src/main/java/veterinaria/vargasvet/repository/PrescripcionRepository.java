package veterinaria.vargasvet.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.Prescripcion;

import java.util.List;
import java.time.LocalDate;

@Repository
public interface PrescripcionRepository extends JpaRepository<Prescripcion, Long> {

    List<Prescripcion> findByConsultaIdOrderByCreatedAtAsc(Long consultaId);

    @Query(value = "SELECT p.* FROM prescripciones p " +
                   "JOIN consulta c ON c.id = p.consulta_id " +
                   "JOIN historia_clinica hc ON hc.id = c.historia_clinica_id " +
                   "JOIN mascota m ON m.id = hc.mascota_id " +
                   "JOIN apoderado a ON a.id = m.apoderado_id " +
                   "JOIN usuario u ON u.id = a.user_id " +
                   "LEFT JOIN empleado e ON e.id = p.veterinario_id " +
                   "WHERE (:isSuperAdmin = true OR u.company_id = :companyId) " +
                   "AND (CAST(:query AS varchar) IS NULL " +
                   "     OR LOWER(p.medicamento) LIKE CAST(:query AS varchar) " +
                   "     OR LOWER(p.principio_activo) LIKE CAST(:query AS varchar) " +
                   "     OR LOWER(m.nombre_completo) LIKE CAST(:query AS varchar) " +
                   "     OR LOWER(hc.numero_hc) LIKE CAST(:query AS varchar)) " +
                   "AND (CAST(:nombreMascota AS varchar) IS NULL OR LOWER(m.nombre_completo) LIKE LOWER(CONCAT('%', CAST(:nombreMascota AS varchar), '%'))) " +
                   "AND (CAST(:numeroMicrochip AS varchar) IS NULL OR LOWER(m.numero_microchip) LIKE LOWER(CONCAT('%', CAST(:numeroMicrochip AS varchar), '%'))) " +
                   "AND (CAST(:numeroDocumentoApoderado AS varchar) IS NULL OR LOWER(a.numero_documento) LIKE LOWER(CONCAT('%', CAST(:numeroDocumentoApoderado AS varchar), '%'))) " +
                   "AND (CAST(:numeroDocumentoEmpleado AS varchar) IS NULL OR LOWER(e.numero_documento_identidad) LIKE LOWER(CONCAT('%', CAST(:numeroDocumentoEmpleado AS varchar), '%'))) " +
                   "AND (CAST(:numeroHc AS varchar) IS NULL OR LOWER(hc.numero_hc) LIKE LOWER(CONCAT('%', CAST(:numeroHc AS varchar), '%'))) " +
                   "AND CAST(p.created_at AS date) >= :fechaDesde " +
                   "AND CAST(p.created_at AS date) <= :fechaHasta " +
                   "ORDER BY p.created_at DESC",
           countQuery = "SELECT COUNT(*) FROM prescripciones p " +
                        "JOIN consulta c ON c.id = p.consulta_id " +
                        "JOIN historia_clinica hc ON hc.id = c.historia_clinica_id " +
                        "JOIN mascota m ON m.id = hc.mascota_id " +
                        "JOIN apoderado a ON a.id = m.apoderado_id " +
                        "JOIN usuario u ON u.id = a.user_id " +
                        "LEFT JOIN empleado e ON e.id = p.veterinario_id " +
                        "WHERE (:isSuperAdmin = true OR u.company_id = :companyId) " +
                        "AND (CAST(:query AS varchar) IS NULL " +
                        "     OR LOWER(p.medicamento) LIKE CAST(:query AS varchar) " +
                        "     OR LOWER(p.principio_activo) LIKE CAST(:query AS varchar) " +
                        "     OR LOWER(m.nombre_completo) LIKE CAST(:query AS varchar) " +
                        "     OR LOWER(hc.numero_hc) LIKE CAST(:query AS varchar)) " +
                        "AND (CAST(:nombreMascota AS varchar) IS NULL OR LOWER(m.nombre_completo) LIKE LOWER(CONCAT('%', CAST(:nombreMascota AS varchar), '%'))) " +
                        "AND (CAST(:numeroMicrochip AS varchar) IS NULL OR LOWER(m.numero_microchip) LIKE LOWER(CONCAT('%', CAST(:numeroMicrochip AS varchar), '%'))) " +
                        "AND (CAST(:numeroDocumentoApoderado AS varchar) IS NULL OR LOWER(a.numero_documento) LIKE LOWER(CONCAT('%', CAST(:numeroDocumentoApoderado AS varchar), '%'))) " +
                        "AND (CAST(:numeroDocumentoEmpleado AS varchar) IS NULL OR LOWER(e.numero_documento_identidad) LIKE LOWER(CONCAT('%', CAST(:numeroDocumentoEmpleado AS varchar), '%'))) " +
                        "AND (CAST(:numeroHc AS varchar) IS NULL OR LOWER(hc.numero_hc) LIKE LOWER(CONCAT('%', CAST(:numeroHc AS varchar), '%'))) " +
                        "AND CAST(p.created_at AS date) >= :fechaDesde " +
                        "AND CAST(p.created_at AS date) <= :fechaHasta",
           nativeQuery = true)
    Page<Prescripcion> buscar(
            @Param("isSuperAdmin") boolean isSuperAdmin,
            @Param("companyId") Integer companyId,
            @Param("query") String query,
            @Param("nombreMascota") String nombreMascota,
            @Param("numeroMicrochip") String numeroMicrochip,
            @Param("numeroDocumentoApoderado") String numeroDocumentoApoderado,
            @Param("numeroDocumentoEmpleado") String numeroDocumentoEmpleado,
            @Param("numeroHc") String numeroHc,
            @Param("fechaDesde") LocalDate fechaDesde,
            @Param("fechaHasta") LocalDate fechaHasta,
            Pageable pageable);

    @Query("SELECT p FROM Prescripcion p " +
           "JOIN p.consulta c " +
           "JOIN c.historiaClinica hc " +
           "JOIN hc.mascota m " +
           "WHERE m.apoderado.id = :apoderadoId " +
           "ORDER BY p.createdAt DESC")
    List<Prescripcion> findByApoderadoId(@Param("apoderadoId") Long apoderadoId);
}
