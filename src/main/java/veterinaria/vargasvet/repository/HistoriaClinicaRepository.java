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

    interface HistoriaDelListado {
        Long getId();
        String getNumeroHc();
        Boolean getActiva();
        Long getMascotaId();
        String getMascotaNombre();
        String getEspecie();
        String getRaza();
        String getPropietarioNombre();
        Integer getCompanyId();
        String getCompanyName();
        LocalDateTime getFechaUltimaConsulta();
    }

    String LISTADO_UNIONES = "FROM historia_clinica hc " +
            "JOIN mascota m ON m.id = hc.mascota_id " +
            "JOIN apoderado a ON a.id = m.apoderado_id " +
            "JOIN usuario u ON u.id = a.user_id ";

    String LISTADO_FILTROS = "WHERE (:isSuperAdmin = true OR a.company_id = :companyId) " +
            "AND (CAST(:veterinarioId AS bigint) IS NULL OR EXISTS (SELECT 1 FROM consulta own_c WHERE own_c.historia_clinica_id = hc.id AND own_c.veterinario_id = :veterinarioId)) " +
            "AND (CAST(:numeroHc AS varchar) IS NULL OR hc.numero_hc = CAST(:numeroHc AS varchar)) ";

    String LISTADO_NOMBRES_EXACTOS = "AND (CAST(:nombrePaciente AS varchar) IS NULL OR LOWER(m.nombre_completo) LIKE CAST(:nombrePaciente AS varchar)) " +
            "AND (CAST(:nombrePropietario AS varchar) IS NULL OR LOWER(CONCAT(u.nombre, ' ', u.apellido)) LIKE CAST(:nombrePropietario AS varchar)) ";

    String LISTADO_NOMBRES_FLEXIBLES = "AND (CAST(:nombrePaciente AS varchar) IS NULL OR LOWER(m.nombre_completo) LIKE LOWER(CONCAT('%', CAST(:nombrePaciente AS varchar), '%')) " +
            "     OR LOWER(m.nombre_completo) % LOWER(CAST(:nombrePaciente AS varchar))) " +
            "AND (CAST(:nombrePropietario AS varchar) IS NULL OR LOWER(CONCAT(u.nombre, ' ', u.apellido)) LIKE LOWER(CONCAT('%', CAST(:nombrePropietario AS varchar), '%')) " +
            "     OR LOWER(CONCAT(u.nombre, ' ', u.apellido)) % LOWER(CAST(:nombrePropietario AS varchar))) ";

    String LISTADO_FECHAS = "AND ((CAST(:fechaDesde AS timestamp) IS NULL AND CAST(:fechaHasta AS timestamp) IS NULL) " +
            "OR EXISTS (SELECT 1 FROM consulta c WHERE c.historia_clinica_id = hc.id " +
            "AND (CAST(:fechaDesde AS timestamp) IS NULL OR c.fecha_consulta >= CAST(:fechaDesde AS timestamp)) " +
            "AND (CAST(:fechaHasta AS timestamp) IS NULL OR c.fecha_consulta < CAST(:fechaHasta AS timestamp)))) ";

    @Query(value = "SELECT hc.id " + LISTADO_UNIONES + LISTADO_FILTROS + LISTADO_NOMBRES_EXACTOS + LISTADO_FECHAS +
                   "ORDER BY hc.numero_hc ASC",
           countQuery = "SELECT COUNT(*) " + LISTADO_UNIONES + LISTADO_FILTROS + LISTADO_NOMBRES_EXACTOS + LISTADO_FECHAS,
           nativeQuery = true)
    Page<Long> buscarIds(
            @Param("isSuperAdmin") boolean isSuperAdmin,
            @Param("companyId") Integer companyId,
            @Param("veterinarioId") Long veterinarioId,
            @Param("numeroHc") String numeroHc,
            @Param("nombrePaciente") String nombrePaciente,
            @Param("nombrePropietario") String nombrePropietario,
            @Param("fechaDesde") LocalDateTime fechaDesde,
            @Param("fechaHasta") LocalDateTime fechaHasta,
            Pageable pageable);

    @Query(value = "SELECT hc.id " + LISTADO_UNIONES + LISTADO_FILTROS + LISTADO_NOMBRES_FLEXIBLES + LISTADO_FECHAS +
                   "ORDER BY hc.numero_hc ASC",
           countQuery = "SELECT COUNT(*) " + LISTADO_UNIONES + LISTADO_FILTROS + LISTADO_NOMBRES_FLEXIBLES + LISTADO_FECHAS,
           nativeQuery = true)
    Page<Long> buscarIdsConCoincidenciaFlexible(
            @Param("isSuperAdmin") boolean isSuperAdmin,
            @Param("companyId") Integer companyId,
            @Param("veterinarioId") Long veterinarioId,
            @Param("numeroHc") String numeroHc,
            @Param("nombrePaciente") String nombrePaciente,
            @Param("nombrePropietario") String nombrePropietario,
            @Param("fechaDesde") LocalDateTime fechaDesde,
            @Param("fechaHasta") LocalDateTime fechaHasta,
            Pageable pageable);

    @Query(value = "SELECT hc.id AS \"id\", hc.numero_hc AS \"numeroHc\", hc.activa AS \"activa\", " +
                   "m.id AS \"mascotaId\", m.nombre_completo AS \"mascotaNombre\", " +
                   "COALESCE(m.especie, m.otra_especie) AS \"especie\", r.nombre AS \"raza\", " +
                   "CONCAT(u.nombre, ' ', u.apellido) AS \"propietarioNombre\", " +
                   "a.company_id AS \"companyId\", co.name AS \"companyName\", " +
                   "(SELECT MAX(uc.fecha_consulta) FROM consulta uc WHERE uc.historia_clinica_id = hc.id) AS \"fechaUltimaConsulta\" " +
                   LISTADO_UNIONES +
                   "LEFT JOIN raza r ON r.id = m.raza_id " +
                   "LEFT JOIN company co ON co.id = a.company_id " +
                   "WHERE hc.id IN (:ids)",
           nativeQuery = true)
    List<HistoriaDelListado> listadoPorIds(@Param("ids") List<Long> ids);
}
