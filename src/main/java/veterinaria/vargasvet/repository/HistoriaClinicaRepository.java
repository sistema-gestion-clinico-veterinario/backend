package veterinaria.vargasvet.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.HistoriaClinica;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface HistoriaClinicaRepository extends JpaRepository<HistoriaClinica, Long> {

    Optional<HistoriaClinica> findByMascotaId(Long mascotaId);

    boolean existsByNumeroHc(String numeroHc);

    @Query(value = "SELECT hc FROM HistoriaClinica hc " +
                   "JOIN hc.mascota m " +
                   "JOIN m.apoderado a " +
                   "JOIN a.user u " +
                   "WHERE (:isSuperAdmin = true OR u.company.id = :companyId) " +
                   "AND (:numeroHc IS NULL OR hc.numeroHc = :numeroHc) " +
                   "AND (:nombrePaciente IS NULL OR LOWER(m.nombreCompleto) LIKE :nombrePaciente) " +
                   "AND (:nombrePropietario IS NULL OR LOWER(CONCAT(u.nombre, ' ', u.apellido)) LIKE :nombrePropietario) " +
                   "ORDER BY hc.numeroHc DESC",
           countQuery = "SELECT COUNT(hc) FROM HistoriaClinica hc " +
                        "JOIN hc.mascota m " +
                        "JOIN m.apoderado a " +
                        "JOIN a.user u " +
                        "WHERE (:isSuperAdmin = true OR u.company.id = :companyId) " +
                        "AND (:numeroHc IS NULL OR hc.numeroHc = :numeroHc) " +
                        "AND (:nombrePaciente IS NULL OR LOWER(m.nombreCompleto) LIKE :nombrePaciente) " +
                        "AND (:nombrePropietario IS NULL OR LOWER(CONCAT(u.nombre, ' ', u.apellido)) LIKE :nombrePropietario)")
    Page<HistoriaClinica> buscar(
            @Param("isSuperAdmin") boolean isSuperAdmin,
            @Param("companyId") Integer companyId,
            @Param("numeroHc") String numeroHc,
            @Param("nombrePaciente") String nombrePaciente,
            @Param("nombrePropietario") String nombrePropietario,
            @Param("fechaDesde") LocalDateTime fechaDesde,
            @Param("fechaHasta") LocalDateTime fechaHasta,
            Pageable pageable);
}
