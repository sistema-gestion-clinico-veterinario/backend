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
import java.time.LocalDateTime;

@Repository
public interface PrescripcionRepository extends JpaRepository<Prescripcion, Long> {

    List<Prescripcion> findByConsultaIdOrderByCreatedAtAsc(Long consultaId);

    // Proyeccion en lote para el resumen de historia clinica: evita materializar entidades
    // Prescripcion (y con ello el SELECT adicional que Hibernate dispara por cada fila para
    // el @OneToOne(mappedBy) "dispensacion", que no puede ser realmente perezoso sin bytecode
    // enhancement). Fila: [consultaId, id, medicamento, principioActivo, dosis, frecuencia,
    // duracionDias, viaAdministracion, instrucciones, fechaInicio, fechaFin].
    @Query("SELECT p.consulta.id, p.id, p.medicamento, p.principioActivo, p.dosis, p.frecuencia, " +
           "p.duracionDias, p.viaAdministracion, p.instrucciones, p.fechaInicio, p.fechaFin " +
           "FROM Prescripcion p WHERE p.consulta.id IN :consultaIds")
    List<Object[]> findResumenPorConsultaIds(@Param("consultaIds") List<Long> consultaIds);

    interface RecetaDelListado {
        Long getId();
        String getMedicamento();
        String getPrincipioActivo();
        String getDosis();
        String getFrecuencia();
        Integer getDuracionDias();
        String getViaAdministracion();
        String getInstrucciones();
        LocalDate getFechaInicio();
        LocalDate getFechaFin();
        String getVeterinarioNombre();
        LocalDateTime getFechaCreacion();
        Long getConsultaId();
        String getPacienteNombre();
        String getNumeroHc();
    }

    String LISTADO_COLUMNAS = "SELECT p.id AS \"id\", p.medicamento AS \"medicamento\", p.principio_activo AS \"principioActivo\", " +
            "p.dosis AS \"dosis\", p.frecuencia AS \"frecuencia\", p.duracion_dias AS \"duracionDias\", " +
            "p.via_administracion AS \"viaAdministracion\", p.instrucciones AS \"instrucciones\", " +
            "p.fecha_inicio AS \"fechaInicio\", p.fecha_fin AS \"fechaFin\", " +
            "CASE WHEN u.id IS NOT NULL THEN CONCAT(u.nombre, ' ', u.apellido) END AS \"veterinarioNombre\", " +
            "p.created_at AS \"fechaCreacion\", p.consulta_id AS \"consultaId\", " +
            "m.nombre_completo AS \"pacienteNombre\", hc.numero_hc AS \"numeroHc\" ";

    String LISTADO_UNIONES = "FROM prescripciones p " +
            "JOIN consulta c ON c.id = p.consulta_id " +
            "JOIN historia_clinica hc ON hc.id = c.historia_clinica_id " +
            "JOIN mascota m ON m.id = hc.mascota_id " +
            "JOIN apoderado a ON a.id = m.apoderado_id " +
            "LEFT JOIN empleado e ON e.id = p.veterinario_id ";

    String LISTADO_UNION_VETERINARIO = "LEFT JOIN usuario u ON u.id = e.user_id ";

    String LISTADO_FILTROS = "AND (CAST(:query AS varchar) IS NULL " +
            "     OR LOWER(p.medicamento) LIKE CAST(:query AS varchar) " +
            "     OR LOWER(p.principio_activo) LIKE CAST(:query AS varchar) " +
            "     OR LOWER(m.nombre_completo) LIKE CAST(:query AS varchar) " +
            "     OR LOWER(hc.numero_hc) LIKE CAST(:query AS varchar)) " +
            "AND (CAST(:nombreMascota AS varchar) IS NULL OR LOWER(m.nombre_completo) LIKE LOWER(CONCAT('%', CAST(:nombreMascota AS varchar), '%'))) " +
            "AND (CAST(:numeroMicrochip AS varchar) IS NULL OR LOWER(m.numero_microchip) LIKE LOWER(CONCAT('%', CAST(:numeroMicrochip AS varchar), '%'))) " +
            "AND (CAST(:numeroDocumentoApoderado AS varchar) IS NULL OR LOWER(a.numero_documento) LIKE LOWER(CONCAT('%', CAST(:numeroDocumentoApoderado AS varchar), '%'))) " +
            "AND (CAST(:numeroDocumentoEmpleado AS varchar) IS NULL OR LOWER(e.numero_documento_identidad) LIKE LOWER(CONCAT('%', CAST(:numeroDocumentoEmpleado AS varchar), '%'))) " +
            "AND (CAST(:numeroHc AS varchar) IS NULL OR LOWER(hc.numero_hc) LIKE LOWER(CONCAT('%', CAST(:numeroHc AS varchar), '%'))) " +
            "AND p.created_at >= :desde AND p.created_at < :hasta ";

    String LISTADO_DE_LA_EMPRESA = "WHERE a.company_id = :companyId ";
    String LISTADO_DE_TODAS = "WHERE 1 = 1 ";

    @Query(value = LISTADO_COLUMNAS + LISTADO_UNIONES + LISTADO_UNION_VETERINARIO + LISTADO_DE_LA_EMPRESA + LISTADO_FILTROS +
                   "ORDER BY p.created_at DESC",
           countQuery = "SELECT COUNT(*) " + LISTADO_UNIONES + LISTADO_DE_LA_EMPRESA + LISTADO_FILTROS,
           nativeQuery = true)
    Page<RecetaDelListado> buscarDeLaEmpresa(
            @Param("companyId") Integer companyId,
            @Param("query") String query,
            @Param("nombreMascota") String nombreMascota,
            @Param("numeroMicrochip") String numeroMicrochip,
            @Param("numeroDocumentoApoderado") String numeroDocumentoApoderado,
            @Param("numeroDocumentoEmpleado") String numeroDocumentoEmpleado,
            @Param("numeroHc") String numeroHc,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            Pageable pageable);

    @Query(value = LISTADO_COLUMNAS + LISTADO_UNIONES + LISTADO_UNION_VETERINARIO + LISTADO_DE_TODAS + LISTADO_FILTROS +
                   "ORDER BY p.created_at DESC",
           countQuery = "SELECT COUNT(*) " + LISTADO_UNIONES + LISTADO_DE_TODAS + LISTADO_FILTROS,
           nativeQuery = true)
    Page<RecetaDelListado> buscarDeTodasLasEmpresas(
            @Param("query") String query,
            @Param("nombreMascota") String nombreMascota,
            @Param("numeroMicrochip") String numeroMicrochip,
            @Param("numeroDocumentoApoderado") String numeroDocumentoApoderado,
            @Param("numeroDocumentoEmpleado") String numeroDocumentoEmpleado,
            @Param("numeroHc") String numeroHc,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            Pageable pageable);

    @Query("SELECT p FROM Prescripcion p " +
           "JOIN p.consulta c " +
           "JOIN c.historiaClinica hc " +
           "JOIN hc.mascota m " +
           "WHERE m.apoderado.id = :apoderadoId " +
           "ORDER BY p.createdAt DESC")
    List<Prescripcion> findByApoderadoId(@Param("apoderadoId") Long apoderadoId);

    @Query("SELECT p FROM Prescripcion p " +
           "JOIN p.consulta c " +
           "JOIN c.historiaClinica hc " +
           "JOIN hc.mascota m " +
           "WHERE m.id IN :mascotaIds " +
           "ORDER BY p.createdAt DESC")
    List<Prescripcion> findByMascotaIds(@Param("mascotaIds") java.util.Collection<Long> mascotaIds);

    @Query("SELECT YEAR(p.createdAt), MONTH(p.createdAt), COUNT(p) FROM Prescripcion p " +
           "JOIN p.consulta c JOIN c.historiaClinica h JOIN h.mascota m JOIN m.apoderado a JOIN a.user u " +
           "WHERE a.company.id = :companyId AND p.createdAt >= :desde " +
           "GROUP BY YEAR(p.createdAt), MONTH(p.createdAt) ORDER BY 1 ASC, 2 ASC")
    List<Object[]> countByMes(@Param("companyId") Integer companyId, @Param("desde") java.time.LocalDateTime desde);
}
