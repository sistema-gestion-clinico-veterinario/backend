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

    @Query("SELECT a FROM Apoderado a WHERE a.id = :id AND a.company.id = :companyId")
    java.util.Optional<Apoderado> findByIdAndCompanyId(@Param("id") Long id,
                                                        @Param("companyId") Integer companyId);

    @Query(value = "SELECT a FROM Apoderado a JOIN a.user u " +
                   "WHERE a.company.id = :companyId " +
                   "AND (:nombre IS NULL OR LOWER(CAST(CONCAT(u.nombre, ' ', u.apellido) AS text)) LIKE LOWER(CAST(CONCAT('%', REPLACE(CAST(:nombre AS text), ' ', '%'), '%') AS text))) " +
                   "AND (:numeroDocumento IS NULL OR a.numeroDocumento LIKE CAST(CONCAT('%', :numeroDocumento, '%') AS text)) " +
                   "ORDER BY u.apellido ASC, u.nombre ASC",
           countQuery = "SELECT COUNT(a) FROM Apoderado a JOIN a.user u " +
                        "WHERE a.company.id = :companyId " +
                        "AND (:nombre IS NULL OR LOWER(CAST(CONCAT(u.nombre, ' ', u.apellido) AS text)) LIKE LOWER(CAST(CONCAT('%', REPLACE(CAST(:nombre AS text), ' ', '%'), '%') AS text))) " +
                        "AND (:numeroDocumento IS NULL OR a.numeroDocumento LIKE CAST(CONCAT('%', :numeroDocumento, '%') AS text))")
    Page<Apoderado> buscar(@Param("companyId") Integer companyId,
                           @Param("nombre") String nombre,
                           @Param("numeroDocumento") String numeroDocumento,
                           Pageable pageable);

    /** Empresa puntual: uso normal en un contexto de sesion (JWT companyId), donde
     * como mucho hay UNA fila Apoderado activa para ese (usuario, empresa). */
    @Query("SELECT a FROM Apoderado a WHERE a.user.id = :userId AND a.company.id = :companyId")
    java.util.Optional<Apoderado> findByUserIdAndCompanyId(@Param("userId") Integer userId,
                                                            @Param("companyId") Integer companyId);

    /** Lista completa: para cuando de verdad se necesitan TODAS las empresas donde
     * es cliente activo (ej. CompanyMembershipService, selector de empresa). */
    @Query("SELECT a FROM Apoderado a WHERE a.user.id = :userId AND a.estado = true")
    java.util.List<Apoderado> findAllActiveByUserId(@Param("userId") Integer userId);

    @Query("SELECT a FROM Apoderado a WHERE a.company.id = :companyId")
    java.util.List<Apoderado> findByCompanyId(@Param("companyId") Integer companyId);

    boolean existsByUserId(Integer userId);
    boolean existsByUserIdAndEstadoTrue(Integer userId);
    boolean existsByUserIdAndCompanyId(Integer userId, Integer companyId);
    boolean existsByUserIdAndCompanyIdAndEstadoTrue(Integer userId, Integer companyId);
}
