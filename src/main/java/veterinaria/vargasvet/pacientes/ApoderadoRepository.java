package veterinaria.vargasvet.pacientes;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.pacientes.Apoderado;

@Repository
public interface ApoderadoRepository extends JpaRepository<Apoderado, Long> {

    @Query(value = "SELECT a FROM Apoderado a JOIN a.user u " +
                   "WHERE u.company.id = :companyId " +
                   "AND (:nombre IS NULL OR LOWER(CAST(CONCAT(u.nombre, ' ', u.apellido) AS text)) LIKE LOWER(CAST(CONCAT('%', :nombre, '%') AS text))) " +
                   "AND (:numeroDocumento IS NULL OR a.numeroDocumento LIKE CAST(CONCAT('%', :numeroDocumento, '%') AS text)) " +
                   "ORDER BY u.apellido ASC, u.nombre ASC",
           countQuery = "SELECT COUNT(a) FROM Apoderado a JOIN a.user u " +
                        "WHERE u.company.id = :companyId " +
                        "AND (:nombre IS NULL OR LOWER(CAST(CONCAT(u.nombre, ' ', u.apellido) AS text)) LIKE LOWER(CAST(CONCAT('%', :nombre, '%') AS text))) " +
                        "AND (:numeroDocumento IS NULL OR a.numeroDocumento LIKE CAST(CONCAT('%', :numeroDocumento, '%') AS text))")
    Page<Apoderado> buscar(@Param("companyId") Integer companyId,
                           @Param("nombre") String nombre,
                           @Param("numeroDocumento") String numeroDocumento,
                           Pageable pageable);
}
