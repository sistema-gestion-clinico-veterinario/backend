package veterinaria.vargasvet.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.HistoriaClinica;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface HistoriaClinicaRepository extends JpaRepository<HistoriaClinica, Long> {

    Optional<HistoriaClinica> findByMascotaId(Long mascotaId);

    Optional<HistoriaClinica> findByNumeroHc(String numeroHc);

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
                   "AND ((CAST(:fechaDesde AS timestamp) IS NULL AND CAST(:fechaHasta AS timestamp) IS NULL) " +
                   "OR EXISTS (SELECT 1 FROM consulta c WHERE c.historia_clinica_id = hc.id " +
                   "AND (CAST(:fechaDesde AS timestamp) IS NULL OR c.fecha_consulta >= CAST(:fechaDesde AS timestamp)) " +
                   "AND (CAST(:fechaHasta AS timestamp) IS NULL OR c.fecha_consulta < CAST(:fechaHasta AS timestamp)))) " +
                   "ORDER BY hc.numero_hc ASC",
           countQuery = "SELECT COUNT(*) FROM historia_clinica hc " +
                        "JOIN mascota m ON m.id = hc.mascota_id " +
                        "JOIN apoderado a ON a.id = m.apoderado_id " +
                        "JOIN usuario u ON u.id = a.user_id " +
                        "WHERE (:isSuperAdmin = true OR u.company_id = :companyId) " +
                        "AND (CAST(:numeroHc AS varchar) IS NULL OR hc.numero_hc = CAST(:numeroHc AS varchar)) " +
                        "AND (CAST(:nombrePaciente AS varchar) IS NULL OR LOWER(m.nombre_completo) LIKE CAST(:nombrePaciente AS varchar)) " +
                        "AND (CAST(:nombrePropietario AS varchar) IS NULL OR LOWER(CONCAT(u.nombre, ' ', u.apellido)) LIKE CAST(:nombrePropietario AS varchar)) " +
                        "AND ((CAST(:fechaDesde AS timestamp) IS NULL AND CAST(:fechaHasta AS timestamp) IS NULL) " +
                        "OR EXISTS (SELECT 1 FROM consulta c WHERE c.historia_clinica_id = hc.id " +
                        "AND (CAST(:fechaDesde AS timestamp) IS NULL OR c.fecha_consulta >= CAST(:fechaDesde AS timestamp)) " +
                        "AND (CAST(:fechaHasta AS timestamp) IS NULL OR c.fecha_consulta < CAST(:fechaHasta AS timestamp))))",
           nativeQuery = true)
    Page<HistoriaClinica> buscar(
            @Param("isSuperAdmin") boolean isSuperAdmin,
            @Param("companyId") Integer companyId,
            @Param("numeroHc") String numeroHc,
            @Param("nombrePaciente") String nombrePaciente,
            @Param("nombrePropietario") String nombrePropietario,
            @Param("fechaDesde") LocalDateTime fechaDesde,
            @Param("fechaHasta") LocalDateTime fechaHasta,
            Pageable pageable);

    @Query(value = "SELECT hc.* FROM historia_clinica hc " +
                   "JOIN mascota m ON m.id = hc.mascota_id " +
                   "JOIN apoderado a ON a.id = m.apoderado_id " +
                   "JOIN usuario u ON u.id = a.user_id " +
                   "WHERE (:isSuperAdmin = true OR u.company_id = :companyId) " +
                   "AND (CAST(:numeroHc AS varchar) IS NULL OR hc.numero_hc = CAST(:numeroHc AS varchar)) " +
                   "AND (CAST(:nombrePaciente AS varchar) IS NULL OR LOWER(m.nombre_completo) LIKE LOWER(CONCAT('%', CAST(:nombrePaciente AS varchar), '%')) " +
                   "     OR LOWER(m.nombre_completo) % LOWER(CAST(:nombrePaciente AS varchar))) " +
                   "AND (CAST(:nombrePropietario AS varchar) IS NULL OR LOWER(CONCAT(u.nombre, ' ', u.apellido)) LIKE LOWER(CONCAT('%', CAST(:nombrePropietario AS varchar), '%')) " +
                   "     OR LOWER(CONCAT(u.nombre, ' ', u.apellido)) % LOWER(CAST(:nombrePropietario AS varchar))) " +
                   "AND ((CAST(:fechaDesde AS timestamp) IS NULL AND CAST(:fechaHasta AS timestamp) IS NULL) " +
                   "OR EXISTS (SELECT 1 FROM consulta c WHERE c.historia_clinica_id = hc.id " +
                   "AND (CAST(:fechaDesde AS timestamp) IS NULL OR c.fecha_consulta >= CAST(:fechaDesde AS timestamp)) " +
                   "AND (CAST(:fechaHasta AS timestamp) IS NULL OR c.fecha_consulta < CAST(:fechaHasta AS timestamp)))) " +
                   "ORDER BY hc.numero_hc ASC",
           countQuery = "SELECT COUNT(*) FROM historia_clinica hc " +
                        "JOIN mascota m ON m.id = hc.mascota_id " +
                        "JOIN apoderado a ON a.id = m.apoderado_id " +
                        "JOIN usuario u ON u.id = a.user_id " +
                        "WHERE (:isSuperAdmin = true OR u.company_id = :companyId) " +
                        "AND (CAST(:numeroHc AS varchar) IS NULL OR hc.numero_hc = CAST(:numeroHc AS varchar)) " +
                        "AND (CAST(:nombrePaciente AS varchar) IS NULL OR LOWER(m.nombre_completo) LIKE LOWER(CONCAT('%', CAST(:nombrePaciente AS varchar), '%')) " +
                        "     OR LOWER(m.nombre_completo) % LOWER(CAST(:nombrePaciente AS varchar))) " +
                        "AND (CAST(:nombrePropietario AS varchar) IS NULL OR LOWER(CONCAT(u.nombre, ' ', u.apellido)) LIKE LOWER(CONCAT('%', CAST(:nombrePropietario AS varchar), '%')) " +
                        "     OR LOWER(CONCAT(u.nombre, ' ', u.apellido)) % LOWER(CAST(:nombrePropietario AS varchar))) " +
                        "AND ((CAST(:fechaDesde AS timestamp) IS NULL AND CAST(:fechaHasta AS timestamp) IS NULL) " +
                        "OR EXISTS (SELECT 1 FROM consulta c WHERE c.historia_clinica_id = hc.id " +
                        "AND (CAST(:fechaDesde AS timestamp) IS NULL OR c.fecha_consulta >= CAST(:fechaDesde AS timestamp)) " +
                        "AND (CAST(:fechaHasta AS timestamp) IS NULL OR c.fecha_consulta < CAST(:fechaHasta AS timestamp))))",
           nativeQuery = true)
    Page<HistoriaClinica> buscarConCoincidenciaFlexible(
            @Param("isSuperAdmin") boolean isSuperAdmin,
            @Param("companyId") Integer companyId,
            @Param("numeroHc") String numeroHc,
            @Param("nombrePaciente") String nombrePaciente,
            @Param("nombrePropietario") String nombrePropietario,
            @Param("fechaDesde") LocalDateTime fechaDesde,
            @Param("fechaHasta") LocalDateTime fechaHasta,
            Pageable pageable);
}
