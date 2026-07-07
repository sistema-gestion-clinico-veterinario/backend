package veterinaria.vargasvet.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import veterinaria.vargasvet.domain.entity.Apoderado;

@Repository
public interface ApoderadoRepository extends JpaRepository<Apoderado, Long> {

    @Query(value = "SELECT a FROM Apoderado a JOIN a.user u " +
                   "WHERE u.company.id = :companyId " +
                   "AND (:nombre IS NULL OR LOWER(CAST(CONCAT(u.nombre, ' ', u.apellido) AS text)) LIKE LOWER(CAST(CONCAT('%', REPLACE(CAST(:nombre AS text), ' ', '%'), '%') AS text))) " +
                   "AND (:numeroDocumento IS NULL OR a.numeroDocumento LIKE CAST(CONCAT('%', :numeroDocumento, '%') AS text)) " +
                   "ORDER BY u.apellido ASC, u.nombre ASC",
           countQuery = "SELECT COUNT(a) FROM Apoderado a JOIN a.user u " +
                        "WHERE u.company.id = :companyId " +
                        "AND (:nombre IS NULL OR LOWER(CAST(CONCAT(u.nombre, ' ', u.apellido) AS text)) LIKE LOWER(CAST(CONCAT('%', REPLACE(CAST(:nombre AS text), ' ', '%'), '%') AS text))) " +
                        "AND (:numeroDocumento IS NULL OR a.numeroDocumento LIKE CAST(CONCAT('%', :numeroDocumento, '%') AS text))")
    Page<Apoderado> buscar(@Param("companyId") Integer companyId,
                           @Param("nombre") String nombre,
                           @Param("numeroDocumento") String numeroDocumento,
                           Pageable pageable);

    @Query("SELECT a FROM Apoderado a WHERE a.user.id = :userId")
    java.util.Optional<Apoderado> findByUserId(@Param("userId") Integer userId);

    @Query("SELECT a FROM Apoderado a WHERE a.user.company.id = :companyId")
    java.util.List<Apoderado> findByCompanyId(@Param("companyId") Integer companyId);
}
