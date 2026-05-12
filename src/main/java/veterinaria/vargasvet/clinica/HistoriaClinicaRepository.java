package veterinaria.vargasvet.clinica;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.clinica.HistoriaClinica;

import java.util.Optional;

@Repository
public interface HistoriaClinicaRepository extends JpaRepository<HistoriaClinica, Long> {

    Optional<HistoriaClinica> findByMascotaId(Long mascotaId);

    boolean existsByMascotaId(Long mascotaId);

    boolean existsByNumeroHc(String numeroHc);

    @Query(value = "SELECT hc.* FROM historia_clinica hc " +
                   "JOIN mascota m ON m.id = hc.mascota_id " +
                   "JOIN apoderado a ON a.id = m.apoderado_id " +
                   "JOIN usuario u ON u.id = a.user_id " +
                   "WHERE (:isSuperAdmin = true OR u.company_id = :companyId) " +
                   "AND (CAST(:numeroHc AS varchar) IS NULL OR hc.numero_hc = CAST(:numeroHc AS varchar)) " +
                   "AND (CAST(:nombrePaciente AS varchar) IS NULL OR LOWER(m.nombre_completo) LIKE CAST(:nombrePaciente AS varchar)) " +
                   "AND (CAST(:nombrePropietario AS varchar) IS NULL OR LOWER(CONCAT(u.nombre, ' ', u.apellido)) LIKE CAST(:nombrePropietario AS varchar)) " +
                   "AND (CAST(:fechaDesde AS varchar) IS NULL OR EXISTS (SELECT 1 FROM consulta c WHERE c.historia_clinica_id = hc.id AND c.fecha_consulta >= CAST(:fechaDesde AS timestamp))) " +
                   "AND (CAST(:fechaHasta AS varchar) IS NULL OR EXISTS (SELECT 1 FROM consulta c WHERE c.historia_clinica_id = hc.id AND c.fecha_consulta <= CAST(:fechaHasta AS timestamp))) " +
                   "ORDER BY hc.numero_hc DESC",
           countQuery = "SELECT COUNT(*) FROM historia_clinica hc " +
                        "JOIN mascota m ON m.id = hc.mascota_id " +
                        "JOIN apoderado a ON a.id = m.apoderado_id " +
                        "JOIN usuario u ON u.id = a.user_id " +
                        "WHERE (:isSuperAdmin = true OR u.company_id = :companyId) " +
                        "AND (CAST(:numeroHc AS varchar) IS NULL OR hc.numero_hc = CAST(:numeroHc AS varchar)) " +
                        "AND (CAST(:nombrePaciente AS varchar) IS NULL OR LOWER(m.nombre_completo) LIKE CAST(:nombrePaciente AS varchar)) " +
                        "AND (CAST(:nombrePropietario AS varchar) IS NULL OR LOWER(CONCAT(u.nombre, ' ', u.apellido)) LIKE CAST(:nombrePropietario AS varchar)) " +
                        "AND (CAST(:fechaDesde AS varchar) IS NULL OR EXISTS (SELECT 1 FROM consulta c WHERE c.historia_clinica_id = hc.id AND c.fecha_consulta >= CAST(:fechaDesde AS timestamp))) " +
                        "AND (CAST(:fechaHasta AS varchar) IS NULL OR EXISTS (SELECT 1 FROM consulta c WHERE c.historia_clinica_id = hc.id AND c.fecha_consulta <= CAST(:fechaHasta AS timestamp)))",
           nativeQuery = true)
    Page<HistoriaClinica> buscar(
            @Param("isSuperAdmin") boolean isSuperAdmin,
            @Param("companyId") Integer companyId,
            @Param("numeroHc") String numeroHc,
            @Param("nombrePaciente") String nombrePaciente,
            @Param("nombrePropietario") String nombrePropietario,
            @Param("fechaDesde") String fechaDesde,
            @Param("fechaHasta") String fechaHasta,
            Pageable pageable);
}
