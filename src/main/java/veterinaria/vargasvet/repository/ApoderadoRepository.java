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
                   "AND (:nombre IS NULL OR LOWER(CONCAT(u.nombre, ' ', u.apellido)) LIKE LOWER(CONCAT('%', :nombre, '%'))) " +
                   "AND (:numeroDocumento IS NULL OR a.numeroDocumento LIKE CONCAT('%', :numeroDocumento, '%')) " +
                   "ORDER BY u.apellido ASC, u.nombre ASC",
           countQuery = "SELECT COUNT(a) FROM Apoderado a JOIN a.user u " +
                        "WHERE u.company.id = :companyId " +
                        "AND (:nombre IS NULL OR LOWER(CONCAT(u.nombre, ' ', u.apellido)) LIKE LOWER(CONCAT('%', :nombre, '%'))) " +
                        "AND (:numeroDocumento IS NULL OR a.numeroDocumento LIKE CONCAT('%', :numeroDocumento, '%'))")
    Page<Apoderado> buscar(@Param("companyId") Integer companyId,
                           @Param("nombre") String nombre,
                           @Param("numeroDocumento") String numeroDocumento,
                           Pageable pageable);
}
