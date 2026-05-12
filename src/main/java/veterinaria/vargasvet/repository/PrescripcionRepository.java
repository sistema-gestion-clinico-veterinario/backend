package veterinaria.vargasvet.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.Prescripcion;

import java.util.List;

@Repository
public interface PrescripcionRepository extends JpaRepository<Prescripcion, Long> {

    List<Prescripcion> findByConsultaIdOrderByCreatedAtAsc(Long consultaId);

    @Query(value = "SELECT p.* FROM prescripcion p " +
                   "JOIN consulta c ON c.id = p.consulta_id " +
                   "JOIN historia_clinica hc ON hc.id = c.historia_clinica_id " +
                   "JOIN mascota m ON m.id = hc.mascota_id " +
                   "JOIN apoderado a ON a.id = m.apoderado_id " +
                   "JOIN usuario u ON u.id = a.user_id " +
                   "WHERE (:isSuperAdmin = true OR u.company_id = :companyId) " +
                   "AND (CAST(:query AS varchar) IS NULL " +
                   "     OR LOWER(p.medicamento) LIKE CAST(:query AS varchar) " +
                   "     OR LOWER(m.nombre_completo) LIKE CAST(:query AS varchar)) " +
                   "ORDER BY p.created_at DESC",
           countQuery = "SELECT COUNT(*) FROM prescripcion p " +
                        "JOIN consulta c ON c.id = p.consulta_id " +
                        "JOIN historia_clinica hc ON hc.id = c.historia_clinica_id " +
                        "JOIN mascota m ON m.id = hc.mascota_id " +
                        "JOIN apoderado a ON a.id = m.apoderado_id " +
                        "JOIN usuario u ON u.id = a.user_id " +
                        "WHERE (:isSuperAdmin = true OR u.company_id = :companyId) " +
                        "AND (CAST(:query AS varchar) IS NULL " +
                        "     OR LOWER(p.medicamento) LIKE CAST(:query AS varchar) " +
                        "     OR LOWER(m.nombre_completo) LIKE CAST(:query AS varchar))",
           nativeQuery = true)
    Page<Prescripcion> buscar(
            @Param("isSuperAdmin") boolean isSuperAdmin,
            @Param("companyId") Integer companyId,
            @Param("query") String query,
            Pageable pageable);
}
